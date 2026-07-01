package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.melosys.skjema.controller.admin.AdminStatistikkDto
import no.nav.melosys.skjema.controller.admin.BrukStatistikkDto
import no.nav.melosys.skjema.controller.admin.DelStatusDto
import no.nav.melosys.skjema.controller.admin.InnsendingAdminDto
import no.nav.melosys.skjema.controller.admin.ResendVarslerResultatDto
import no.nav.melosys.skjema.controller.admin.RetryResultatDto
import no.nav.melosys.skjema.controller.admin.SaksdekningDto
import no.nav.melosys.skjema.controller.admin.UtkastStatistikkDto
import no.nav.melosys.skjema.controller.admin.VirksomhetStatistikkDto
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

/** MELOSYS-8168: En resend-kandidat – skjema som skal få varsel på nytt, med saksnummer hvis det finnes. */
data class ResendKandidat(
    val skjemaId: UUID,
    val saksnummer: String?
)

/**
 * Administrative operasjoner som eksponeres via [no.nav.melosys.skjema.controller.admin.AdminController]
 * og konsumeres av melosys-console. Gir innsyn i og mulighet til å reprosessere feilede innsendinger.
 */
@Service
class AdminService(
    private val innsendingRepository: InnsendingRepository,
    private val skjemaRepository: SkjemaRepository,
    private val adminStatistikkRepository: AdminStatistikkRepository,
    private val innsendingService: InnsendingService,
    private val arbeidstakerVarslingService: ArbeidstakerVarslingService
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

        val innsendt = iPeriode.mapNotNull { innsending ->
            val metadata = innsending.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@mapNotNull null
            InnsendtSkjema(
                id = innsending.skjema.id!!,
                fnr = innsending.skjema.fnr,
                orgnr = innsending.skjema.orgnr,
                innsenderFnr = innsending.innsenderFnr,
                juridiskEnhet = metadata.juridiskEnhetOrgnr,
                skjemadel = metadata.skjemadel,
                flyt = metadata.representasjonstype,
                sprak = innsending.innsendtSprak,
                periode = innsending.skjema.utsendelsePeriode(),
                erstattet = innsending.skjema.id in erstattedeIder
            )
        }

        val utkast = adminStatistikkRepository.finnAlleUtkast().mapNotNull { skjema ->
            val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@mapNotNull null
            UtkastSkjema(skjema.fnr, metadata.juridiskEnhetOrgnr, metadata.skjemadel, skjema.utsendelsePeriode())
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
            utkast = hentUtkastStatistikk(naa, utkast),
            totaltInnsendt = innsendt.size.toLong(),
            innsendtSisteDoegn = trend.sisteDoegn,
            innsendtSiste7Dager = trend.siste7Dager,
            innsendtSiste30Dager = trend.siste30Dager,
            innsendtPerSkjemadel = Skjemadel.entries.associateWith { sd -> innsendt.count { it.skjemadel == sd }.toLong() },
            innsendtPerFlyt = Representasjonstype.entries.associateWith { f -> innsendt.count { it.flyt == f }.toLong() },
            innsendtPerSprak = Språk.entries.associateWith { sp -> innsendt.count { it.sprak == sp }.toLong() },
            saksdekning = beregnSaksdekning(innsendt, utkast),
            antallUnikePersoner = innsendt.mapTo(mutableSetOf()) { it.fnr }.size.toLong(),
            antallUnikeVirksomheter = innsendt.mapTo(mutableSetOf()) { it.orgnr }.size.toLong(),
            topplisteVirksomheter = beregnToppliste(innsendt)
        )
    }

    private fun innenfor(tidspunkt: Instant, fra: Instant?, tilEksklusiv: Instant?): Boolean =
        (fra == null || !tidspunkt.isBefore(fra)) && (tilEksklusiv == null || tidspunkt.isBefore(tilEksklusiv))

    /**
     * Beregner saksdekning ut fra faktiske verdier: to deler hører til samme sak når de har samme
     * fnr + samme juridiske enhet + overlappende utsendelsesperiode — samme matching som mottak
     * bruker for å gruppere relaterte deler. For ventende deler ses det også mot påbegynte utkast.
     */
    private fun beregnSaksdekning(innsendt: List<InnsendtSkjema>, utkast: List<UtkastSkjema>): SaksdekningDto {
        val arbeidstakerDeler = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }
        val arbeidsgiverDeler = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }
        val arbeidstakerePerSak = arbeidstakerDeler.groupBy { it.sakNokkel() }
        val arbeidsgiverePerSak = arbeidsgiverDeler.groupBy { it.sakNokkel() }
        val utkastArbeidstakerPerSak = utkast.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }.groupBy { it.sakNokkel() }
        val utkastArbeidsgiverPerSak = utkast.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }.groupBy { it.sakNokkel() }

        return SaksdekningDto(
            antallKomplette = innsendt.count { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }.toLong(),
            antallSakerMedBeggeDeler = antallSakerMedBeggeDeler(innsendt),
            arbeidstakerDeler = delStatus(arbeidstakerDeler, arbeidsgiverePerSak, utkastArbeidsgiverPerSak),
            arbeidsgiverDeler = delStatus(arbeidsgiverDeler, arbeidstakerePerSak, utkastArbeidstakerPerSak),
            antallMuligeDobbeltinnsendinger = antallDuplikater(arbeidstakerDeler) + antallDuplikater(arbeidsgiverDeler),
            antallSakerMedFlereVersjoner = antallSakerMedFlereVersjoner(innsendt)
        )
    }

    /**
     * Status for en deltype: har en innsendt motpart (overlappende periode), eller venter.
     * For de som venter skilles det på om motparten har påbegynt et utkast eller ikke.
     *
     * Merk – med vilje to ulike matchekriterier:
     * - innsendt motpart krever overlappende periode (en reell, fullført sak).
     * - utkast-motpart matcher kun på samme person + juridisk enhet (ikke periode), fordi et utkast
     *   under arbeid ofte ikke har fylt inn periode ennå. Hensikten er «har motparten startet noe».
     */
    private fun delStatus(
        deler: List<InnsendtSkjema>,
        motpartSendtPerSak: Map<Pair<String, String>, List<InnsendtSkjema>>,
        motpartUtkastPerSak: Map<Pair<String, String>, List<UtkastSkjema>>
    ): DelStatusDto {
        var medMotpart = 0L
        var venterMotpartHarUtkast = 0L
        var venterIngenMotpart = 0L
        for (del in deler) {
            when {
                motpartSendtPerSak[del.sakNokkel()]?.any { del.matcher(it) } == true -> medMotpart++
                // Bevisst kun person + juridisk enhet (ikke periode): se kommentar over.
                motpartUtkastPerSak[del.sakNokkel()]?.isNotEmpty() == true -> venterMotpartHarUtkast++
                else -> venterIngenMotpart++
            }
        }
        return DelStatusDto(deler.size.toLong(), medMotpart, venterMotpartHarUtkast, venterIngenMotpart)
    }

    /** Saker (person + juridisk enhet) der begge deler er dekket – komplett eller matchende separate deler. */
    private fun antallSakerMedBeggeDeler(innsendt: List<InnsendtSkjema>): Long {
        val komplettSaker = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }
            .mapTo(mutableSetOf()) { it.sakNokkel() }
        val ats = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }.groupBy { it.sakNokkel() }
        val ags = innsendt.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }.groupBy { it.sakNokkel() }
        val separateSaker = ats.keys.intersect(ags.keys).filterTo(mutableSetOf()) { nokkel ->
            ats.getValue(nokkel).any { at -> ags.getValue(nokkel).any { at.matcher(it) } }
        }
        return (komplettSaker + separateSaker).size.toLong()
    }

    /** Saker der minst én deltype er sendt i flere overlappende versjoner (inkl. erstatninger). */
    private fun antallSakerMedFlereVersjoner(innsendt: List<InnsendtSkjema>): Long =
        innsendt.groupBy { it.sakNokkel() }.count { (_, saksdeler) ->
            saksdeler.groupBy { it.skjemadel }.values.any { sammeDel ->
                sammeDel.any { del -> sammeDel.any { it.id != del.id && del.matcher(it) } }
            }
        }.toLong()

    /** Antall deler som overlapper med en annen gjeldende del av samme type/sak (mulig dobbeltinnsending). */
    private fun antallDuplikater(deler: List<InnsendtSkjema>): Long =
        deler.filter { !it.erstattet }
            .groupBy { it.sakNokkel() }
            .values
            .sumOf { gruppe -> gruppe.count { del -> gruppe.any { it.id != del.id && del.matcher(it) } }.toLong() }

    /** Anonym toppliste per virksomhet (orgnr), sortert synkende på antall innsendinger. */
    private fun beregnToppliste(innsendt: List<InnsendtSkjema>): List<VirksomhetStatistikkDto> =
        innsendt.groupBy { it.orgnr }.values
            .map { deler ->
                VirksomhetStatistikkDto(
                    antallInnsendinger = deler.size.toLong(),
                    antallUnikeInnsendere = deler.mapTo(mutableSetOf()) { it.innsenderFnr }.size.toLong(),
                    antallArbeidstakerDel = deler.count { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }.toLong(),
                    antallArbeidsgiverDel = deler.count { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }.toLong(),
                    antallKomplett = deler.count { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }.toLong(),
                    antallSakerMedBeggeDeler = antallSakerMedBeggeDeler(deler)
                )
            }
            .sortedByDescending { it.antallInnsendinger }

    private interface SakDel {
        val fnr: String
        val juridiskEnhet: String
        val periode: PeriodeDto?
    }

    private fun SakDel.sakNokkel(): Pair<String, String> = fnr to juridiskEnhet
    private fun SakDel.matcher(annen: SakDel): Boolean {
        val minPeriode = periode ?: return false
        val annenPeriode = annen.periode ?: return false
        return fnr == annen.fnr && juridiskEnhet == annen.juridiskEnhet && minPeriode.overlapper(annenPeriode)
    }

    private data class InnsendtSkjema(
        val id: UUID,
        override val fnr: String,
        val orgnr: String,
        val innsenderFnr: String,
        override val juridiskEnhet: String,
        val skjemadel: Skjemadel,
        val flyt: Representasjonstype,
        val sprak: Språk,
        override val periode: PeriodeDto?,
        val erstattet: Boolean
    ) : SakDel

    private data class UtkastSkjema(
        override val fnr: String,
        override val juridiskEnhet: String,
        val skjemadel: Skjemadel,
        override val periode: PeriodeDto?
    ) : SakDel

    private fun hentUtkastStatistikk(naa: Instant, utkast: List<UtkastSkjema>): UtkastStatistikkDto {
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
            eldsteOpprettetDato = adminStatistikkRepository.eldsteUtkastOpprettetDato(),
            perSkjemadel = Skjemadel.entries.associateWith { sd -> utkast.count { it.skjemadel == sd }.toLong() }
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

    /**
     * MELOSYS-8168 (midlertidig): Resender brukervarsel (nå med SMS) til arbeidstakere som ikke fikk SMS
     * før SMS-prodsettingen. Kandidatene finnes i koden – ingen kuratert liste – og er bevisst litt
     * over-inkluderende (kan treffe et par ekstra), men ingen som faktisk trenger SMS skal utelates:
     *
     * Kandidat = handlingspliktig AG-del (arbeidsgiver/rådgiver uten fullmakt) som ble sendt inn FØR
     * [SMS_AKTIVERT_TIDSPUNKT] og som fortsatt venter på arbeidstakers del (ingen innsendt arbeidstaker-/
     * kombinert-del matcher på samme fnr + juridisk enhet + overlappende periode).
     *
     * Selve sendingen delegeres til [ArbeidstakerVarslingService.resendVarselTilArbeidstaker], som i tillegg
     * hopper over arbeidstakere med påbegynt utkast. Sendingen skjer utenfor lese-transaksjonen (jf.
     * [retryAlleFeilede]) så vi ikke holder en DB-connection åpen gjennom Kafka-sendingen. Returnerer antall
     * sendte varsler og saksnumrene som faktisk fikk et nytt varsel (for sporbarhet på fagsiden).
     */
    fun resendVarsler(): ResendVarslerResultatDto {
        val kandidater = finnResendKandidater()
        log.info { "Admin: Resend – fant ${kandidater.size} kandidat(er) (handlingspliktig AG-del før $SMS_AKTIVERT_TIDSPUNKT som venter på AT-del)" }

        val sendteSaksnumre = mutableListOf<String>()
        kandidater.forEach { kandidat ->
            try {
                if (arbeidstakerVarslingService.resendVarselTilArbeidstaker(kandidat.skjemaId)) {
                    // Saker uten saksnummer representeres med skjema-id-en, så ingen sending blir usynlig.
                    sendteSaksnumre += kandidat.saksnummer ?: kandidat.skjemaId.toString()
                }
            } catch (e: Exception) {
                log.error(e) { "Admin: Resend feilet for skjema ${kandidat.skjemaId}" }
            }
        }
        log.info { "Admin: Resend ferdig – ${sendteSaksnumre.size} varsler sendt" }
        return ResendVarslerResultatDto(antallSendt = sendteSaksnumre.size, saksnumre = sendteSaksnumre)
    }

    /** Finner resend-kandidatene med skjema-id og saksnummer (se [resendVarsler] for kriteriene). */
    @Transactional(readOnly = true)
    fun finnResendKandidater(): List<ResendKandidat> {
        val alleInnsendte = innsendingRepository.finnAlleInnsendteMedSkjema()
        val arbeidstakerDeler = alleInnsendte.filter { erArbeidstakerDel(it) }
        val erstattedeIder: Set<UUID> = alleInnsendte
            .mapNotNull { (it.skjema.metadata as? UtsendtArbeidstakerMetadata)?.erstatterSkjemaId }
            .toSet()
        return alleInnsendte
            .filter { innsending ->
                innsending.skjema.id !in erstattedeIder &&
                    erHandlingspliktigAgDel(innsending) &&
                    innsending.opprettetDato.isBefore(SMS_AKTIVERT_TIDSPUNKT) &&
                    venterPaaArbeidstakerDel(innsending, arbeidstakerDeler)
            }
            .map { ResendKandidat(skjemaId = it.skjema.id!!, saksnummer = it.saksnummer) }
    }

    /** Handlingspliktig AG/rådgiver-del uten fullmakt – arbeidstaker må sende inn sin egen del. */
    private fun erHandlingspliktigAgDel(innsending: Innsending): Boolean {
        val metadata = innsending.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return false
        return metadata.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL &&
            metadata.representasjonstype in HANDLINGSPLIKTIGE_REPRESENTASJONSTYPER
    }

    /** Innsendt arbeidstaker-del – enten egen del eller kombinert skjema som dekker arbeidstakers del. */
    private fun erArbeidstakerDel(innsending: Innsending): Boolean {
        val skjemadel = (innsending.skjema.metadata as? UtsendtArbeidstakerMetadata)?.skjemadel
        return skjemadel == Skjemadel.ARBEIDSTAKERS_DEL || skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
    }

    /**
     * AT-del mangler hvis ingen innsendt arbeidstaker-del matcher på samme fnr + juridisk enhet +
     * overlappende periode (samme matching som mottak/saksdekning bruker). Mangler periode på AG-delen
     * regnes som "venter" (vi sender da heller én ekstra enn å utelate noen som trenger SMS).
     */
    private fun venterPaaArbeidstakerDel(agDel: Innsending, arbeidstakerDeler: List<Innsending>): Boolean {
        val agMeta = agDel.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return false
        val agPeriode = agDel.skjema.utsendelsePeriode() ?: return true
        return arbeidstakerDeler.none { atDel ->
            val atMeta = atDel.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@none false
            val atPeriode = atDel.skjema.utsendelsePeriode() ?: return@none false
            agDel.skjema.fnr == atDel.skjema.fnr &&
                agMeta.juridiskEnhetOrgnr == atMeta.juridiskEnhetOrgnr &&
                agPeriode.overlapper(atPeriode)
        }
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

    companion object {
        /**
         * MELOSYS-8168: Tidspunktet SMS ble aktivert for handlingspliktige varsler. Handlingspliktige
         * AG-deler innsendt FØR dette fikk varsel på nav.no, men ingen SMS, og er kandidater for resend.
         */
        private val SMS_AKTIVERT_TIDSPUNKT: Instant = Instant.parse("2026-06-29T10:41:46Z")

        /** Representasjonstyper der arbeidstaker selv må sende inn sin del (uten fullmakt). */
        private val HANDLINGSPLIKTIGE_REPRESENTASJONSTYPER = setOf(
            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.RADGIVER
        )
    }
}
