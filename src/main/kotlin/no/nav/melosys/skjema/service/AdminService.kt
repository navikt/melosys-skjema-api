package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.melosys.skjema.controller.admin.AdminStatistikkDto
import no.nav.melosys.skjema.controller.admin.BrukStatistikkDto
import no.nav.melosys.skjema.controller.admin.InnsendingAdminDto
import no.nav.melosys.skjema.controller.admin.RetryResultatDto
import no.nav.melosys.skjema.controller.admin.SaksdekningDto
import no.nav.melosys.skjema.controller.admin.UtkastStatistikkDto
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.extensions.overlapper
import no.nav.melosys.skjema.extensions.utsendelsePeriode
import no.nav.melosys.skjema.repository.AdminStatistikkRepository
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}
private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")

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
    fun hentStatistikk(): AdminStatistikkDto {
        val innsendingPerStatus = InnsendingStatus.entries.associateWith { innsendingRepository.countByStatus(it) }
        return AdminStatistikkDto(
            skjemaPerStatus = SkjemaStatus.entries.associateWith { skjemaRepository.countByStatus(it) },
            innsendingPerStatus = innsendingPerStatus,
            antallFeiledeInnsendinger = innsendingPerStatus[InnsendingStatus.KAFKA_FEILET] ?: 0L
        )
    }

    /**
     * Bruksstatistikk. Innsendt-statistikken (fordelinger, saksdekning, toppliste, unike) regnes i
     * minnet fra innsendinger med skjema, filtrert på innsendingsdato [fraOgMed]–[tilOgMed] (begge
     * valgfrie; null = ingen grense). Utkast og innsendt-trend er nåtilstand og påvirkes ikke av perioden.
     */
    @Transactional(readOnly = true)
    fun hentBruksstatistikk(fraOgMed: LocalDate?, tilOgMed: LocalDate?): BrukStatistikkDto {
        val naa = Instant.now()
        val fraGrense = fraOgMed?.atStartOfDay(OSLO)?.toInstant()
        val tilGrenseEksklusiv = tilOgMed?.plusDays(1)?.atStartOfDay(OSLO)?.toInstant()

        val iPeriode = innsendingRepository.finnAlleInnsendteMedSkjema()
            .filter { innenfor(it.opprettetDato, fraGrense, tilGrenseEksklusiv) }
        // Id-er som er erstattet av en nyere versjon innenfor perioden (holdes utenfor duplikat-tellingen).
        val erstattedeIder: Set<UUID> = iPeriode
            .mapNotNull { (it.skjema.metadata as? UtsendtArbeidstakerMetadata)?.erstatterSkjemaId }
            .toSet()

        val innsendt = iPeriode
            .mapNotNull { innsending ->
                val metadata = innsending.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@mapNotNull null
                InnsendtSkjema(
                    id = innsending.skjema.id!!,
                    fnr = innsending.skjema.fnr,
                    orgnr = innsending.skjema.orgnr,
                    juridiskEnhet = metadata.juridiskEnhetOrgnr,
                    skjemadel = metadata.skjemadel,
                    flyt = metadata.representasjonstype,
                    sprak = innsending.innsendtSprak,
                    periode = innsending.skjema.utsendelsePeriode(),
                    erstattet = innsending.skjema.id in erstattedeIder
                )
            }

        val trend = adminStatistikkRepository.innsendtTrend(
            grense1d = naa.minus(1, ChronoUnit.DAYS),
            grense7d = naa.minus(7, ChronoUnit.DAYS),
            grense30d = naa.minus(30, ChronoUnit.DAYS)
        )

        return BrukStatistikkDto(
            tidspunkt = naa,
            periodeFraOgMed = fraOgMed,
            periodeTilOgMed = tilOgMed,
            utkast = hentUtkastStatistikk(naa),
            totaltInnsendt = innsendt.size.toLong(),
            innsendtSisteDoegn = trend.sisteDoegn,
            innsendtSiste7Dager = trend.siste7Dager,
            innsendtSiste30Dager = trend.siste30Dager,
            innsendtPerSkjemadel = Skjemadel.entries.associateWith { sd -> innsendt.count { it.skjemadel == sd }.toLong() },
            innsendtPerFlyt = Representasjonstype.entries.associateWith { f -> innsendt.count { it.flyt == f }.toLong() },
            innsendtPerSprak = Språk.entries.associateWith { sp -> innsendt.count { it.sprak == sp }.toLong() },
            saksdekning = beregnSaksdekning(innsendt),
            antallUnikePersoner = innsendt.mapTo(mutableSetOf()) { it.fnr }.size.toLong(),
            antallUnikeVirksomheter = innsendt.mapTo(mutableSetOf()) { it.orgnr }.size.toLong(),
            topplisteVirksomheter = innsendt.groupingBy { it.orgnr }.eachCount().values.sortedDescending().map { it.toLong() }
        )
    }

    private fun innenfor(tidspunkt: Instant, fra: Instant?, tilEksklusiv: Instant?): Boolean =
        (fra == null || !tidspunkt.isBefore(fra)) && (tilEksklusiv == null || tidspunkt.isBefore(tilEksklusiv))

    /**
     * Beregner saksdekning ut fra faktiske verdier: to deler hører til samme sak når de har samme
     * fnr + samme juridiske enhet + overlappende utsendelsesperiode — samme matching som mottak
     * bruker for å gruppere relaterte deler.
     */
    private fun beregnSaksdekning(innsendt: List<InnsendtSkjema>): SaksdekningDto {
        val antallKomplette = innsendt.count { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }.toLong()
        val arbeidstakerDeler = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }
        val arbeidsgiverDeler = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }
        val arbeidstakerePerSak = arbeidstakerDeler.groupBy { it.sakNokkel() }
        val arbeidsgiverePerSak = arbeidsgiverDeler.groupBy { it.sakNokkel() }

        val atMedMotpart = arbeidstakerDeler.count { at ->
            arbeidsgiverePerSak[at.sakNokkel()]?.any { at.matcher(it) } == true
        }.toLong()
        val agMedMotpart = arbeidsgiverDeler.count { ag ->
            arbeidstakerePerSak[ag.sakNokkel()]?.any { ag.matcher(it) } == true
        }.toLong()

        // Saker (unik person + juridisk enhet) med begge deler dekket – enten via et komplett skjema
        // eller via separat at-del + ag-del som overlapper. Dedupliseres på sak slik at samme sak ikke
        // telles flere ganger om den har både komplett skjema og separate deler.
        val komplettSaker = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }
            .mapTo(mutableSetOf()) { it.sakNokkel() }
        val separateSaker = arbeidstakerePerSak.keys.intersect(arbeidsgiverePerSak.keys).filterTo(mutableSetOf()) { nokkel ->
            val ats = arbeidstakerePerSak.getValue(nokkel)
            val ags = arbeidsgiverePerSak.getValue(nokkel)
            ats.any { at -> ags.any { at.matcher(it) } }
        }

        return SaksdekningDto(
            antallKomplette = antallKomplette,
            antallSakerMedBeggeDeler = (komplettSaker + separateSaker).size.toLong(),
            antallArbeidstakerDelMedMotpart = atMedMotpart,
            antallArbeidsgiverDelMedMotpart = agMedMotpart,
            antallArbeidstakerDelUtenMotpart = arbeidstakerDeler.size - atMedMotpart,
            antallArbeidsgiverDelUtenMotpart = arbeidsgiverDeler.size - agMedMotpart,
            antallMuligeDobbeltinnsendinger = antallDuplikater(arbeidstakerDeler) + antallDuplikater(arbeidsgiverDeler)
        )
    }

    /** Antall deler som overlapper med en annen gjeldende del av samme type/sak (mulig dobbeltinnsending). */
    private fun antallDuplikater(deler: List<InnsendtSkjema>): Long =
        deler.filter { !it.erstattet }
            .groupBy { it.sakNokkel() }
            .values
            .sumOf { gruppe -> gruppe.count { del -> gruppe.any { it.id != del.id && del.matcher(it) } }.toLong() }

    private data class InnsendtSkjema(
        val id: UUID,
        val fnr: String,
        val orgnr: String,
        val juridiskEnhet: String,
        val skjemadel: Skjemadel,
        val flyt: Representasjonstype,
        val sprak: Språk,
        val periode: PeriodeDto?,
        val erstattet: Boolean
    ) {
        fun sakNokkel() = fnr to juridiskEnhet
        fun matcher(annen: InnsendtSkjema): Boolean =
            fnr == annen.fnr &&
                juridiskEnhet == annen.juridiskEnhet &&
                periode != null && annen.periode != null &&
                periode.overlapper(annen.periode)
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
