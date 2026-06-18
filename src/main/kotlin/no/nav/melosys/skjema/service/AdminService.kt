package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.melosys.skjema.controller.admin.AdminStatistikkDto
import no.nav.melosys.skjema.controller.admin.BrukStatistikkDto
import no.nav.melosys.skjema.controller.admin.InnsendingAdminDto
import no.nav.melosys.skjema.controller.admin.RetryResultatDto
import no.nav.melosys.skjema.controller.admin.UtkastStatistikkDto
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.repository.AdminStatistikkRepository
import no.nav.melosys.skjema.repository.AntallPerKategori
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * Administrative operasjoner som eksponeres via [no.nav.melosys.skjema.controller.admin.AdminController]
 * og konsumeres av melosys-console. Gir innsyn i og mulighet til å reprosessere feilede innsendinger.
 */
@Service
class AdminService(
    private val innsendingRepository: InnsendingRepository,
    private val skjemaRepository: SkjemaRepository,
    private val adminStatistikkRepository: AdminStatistikkRepository,
    private val innsendingService: InnsendingService
) {

    @Transactional(readOnly = true)
    fun hentStatistikk(): AdminStatistikkDto = AdminStatistikkDto(
        skjemaPerStatus = SkjemaStatus.entries.associateWith { skjemaRepository.countByStatus(it) },
        innsendingPerStatus = InnsendingStatus.entries.associateWith { innsendingRepository.countByStatus(it) },
        antallFeiledeInnsendinger = innsendingRepository.countByStatus(InnsendingStatus.KAFKA_FEILET)
    )

    @Transactional(readOnly = true)
    fun hentBruksstatistikk(): BrukStatistikkDto {
        val naa = Instant.now()

        val perSkjemadel = adminStatistikkRepository.antallInnsendtPerSkjemadel().tilMap()
        val innsendtPerSkjemadel = Skjemadel.entries.associateWith { perSkjemadel[it.name] ?: 0L }
        val perFlyt = adminStatistikkRepository.antallInnsendtPerFlyt().tilMap()
        val innsendtPerFlyt = Representasjonstype.entries.associateWith { perFlyt[it.name] ?: 0L }
        val perSprak = adminStatistikkRepository.antallInnsendtPerSprak().tilMap()
        val innsendtPerSprak = Språk.entries.associateWith { perSprak[it.kode] ?: 0L }

        val antallKomplettInnsendt = innsendtPerSkjemadel[Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL] ?: 0L
        val antallKobledePar = adminStatistikkRepository.antallKobledeSkjema() / 2

        return BrukStatistikkDto(
            tidspunkt = naa,
            utkast = hentUtkastStatistikk(naa),
            totaltInnsendt = adminStatistikkRepository.antallInnsendteSkjema(),
            innsendtSisteDoegn = adminStatistikkRepository.antallInnsendtEtter(naa.minus(1, ChronoUnit.DAYS)),
            innsendtSiste7Dager = adminStatistikkRepository.antallInnsendtEtter(naa.minus(7, ChronoUnit.DAYS)),
            innsendtSiste30Dager = adminStatistikkRepository.antallInnsendtEtter(naa.minus(30, ChronoUnit.DAYS)),
            innsendtPerSkjemadel = innsendtPerSkjemadel,
            innsendtPerFlyt = innsendtPerFlyt,
            innsendtPerSprak = innsendtPerSprak,
            antallKomplettInnsendt = antallKomplettInnsendt,
            antallKobledePar = antallKobledePar,
            antallSakerMedBeggeDeler = antallKomplettInnsendt + antallKobledePar,
            antallUnikePersoner = adminStatistikkRepository.antallUnikePersoner(),
            antallUnikeVirksomheter = adminStatistikkRepository.antallUnikeVirksomheter()
        )
    }

    private fun hentUtkastStatistikk(naa: Instant): UtkastStatistikkDto {
        val fordeling = adminStatistikkRepository.utkastAldersfordeling(
            grense1d = naa.minus(1, ChronoUnit.DAYS),
            grense7d = naa.minus(7, ChronoUnit.DAYS),
            grense30d = naa.minus(30, ChronoUnit.DAYS)
        )
        return UtkastStatistikkDto(
            antall = fordeling.totalt,
            under1Dag = fordeling.under1Dag,
            mellom1Og7Dager = fordeling.mellom1Og7Dager,
            mellom7Og30Dager = fordeling.mellom7Og30Dager,
            over30Dager = fordeling.over30Dager,
            eldsteOpprettetDato = adminStatistikkRepository.eldsteUtkastOpprettetDato()
        )
    }

    private fun List<AntallPerKategori>.tilMap(): Map<String, Long> =
        filter { it.kategori != null }.associate { it.kategori!! to it.antall }

    @Transactional(readOnly = true)
    fun hentFeiledeInnsendinger(): List<InnsendingAdminDto> =
        innsendingRepository.findByStatusMedSkjema(InnsendingStatus.KAFKA_FEILET)
            .map { it.tilAdminDto() }
            .sortedByDescending { it.opprettetDato }

    @Transactional(readOnly = true)
    fun antallFeiledeInnsendinger(): Long =
        innsendingRepository.countByStatus(InnsendingStatus.KAFKA_FEILET)

    @Transactional(readOnly = true)
    fun hentInnsending(innsendingId: UUID): InnsendingAdminDto =
        finnInnsending(innsendingId).tilAdminDto()

    /**
     * Tvinger en ny prosessering (Kafka-sending) av en enkelt innsending.
     */
    fun retryInnsending(innsendingId: UUID): InnsendingAdminDto {
        val skjemaId = finnSkjemaId(innsendingId)
        log.info { "Admin: Tvinger retry av innsending $innsendingId (skjema $skjemaId)" }
        innsendingService.prosesserInnsending(skjemaId)
        return hentInnsending(innsendingId)
    }

    /**
     * Tvinger ny prosessering av alle innsendinger med status KAFKA_FEILET.
     */
    fun retryAlleFeilede(): RetryResultatDto {
        val skjemaIder = hentFeiledeSkjemaIder()
        log.info { "Admin: Tvinger retry av ${skjemaIder.size} feilede innsendinger" }

        var feilet = 0
        skjemaIder.forEach { skjemaId ->
            try {
                innsendingService.prosesserInnsending(skjemaId)
            } catch (e: Exception) {
                feilet++
                log.error(e) { "Admin: Retry feilet for skjema $skjemaId" }
            }
        }
        return RetryResultatDto(antallForsoekt = skjemaIder.size, antallFeilet = feilet)
    }

    @Transactional(readOnly = true)
    fun hentFeiledeSkjemaIder(): List<UUID> =
        innsendingRepository.findByStatusMedSkjema(InnsendingStatus.KAFKA_FEILET).map { it.skjema.id!! }

    @Transactional(readOnly = true)
    fun finnSkjemaId(innsendingId: UUID): UUID = finnInnsending(innsendingId).skjema.id!!

    private fun finnInnsending(innsendingId: UUID): Innsending =
        innsendingRepository.findById(innsendingId)
            .orElseThrow { NoSuchElementException("Fant ingen innsending med id $innsendingId") }

    private fun Innsending.tilAdminDto() = InnsendingAdminDto(
        innsendingId = id!!,
        skjemaId = skjema.id!!,
        referanseId = referanseId,
        status = status,
        skjemaStatus = skjema.status,
        orgnr = skjema.orgnr,
        antallForsok = antallForsok,
        feilmelding = feilmelding,
        sisteForsoekTidspunkt = sisteForsoekTidspunkt,
        opprettetDato = opprettetDato,
        saksnummer = saksnummer
    )
}
