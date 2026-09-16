package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.kafka.SkjemaMottattProducer
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.extensions.overlapper
import no.nav.melosys.skjema.extensions.utsendelsePeriode
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


private val log = KotlinLogging.logger {}

/**
 * Service for transaksjonelle databaseoperasjoner på Innsending.
 *
 * Skilt ut fra InnsendingProsesseringService for å unngå self-invocation
 * problemet med Spring @Transactional.
 */
@Service
class InnsendingService(
    private val innsendingRepository: InnsendingRepository,
    private val skjemaRepository: SkjemaRepository,
    private val skjemaMottattProducer: SkjemaMottattProducer,
    private val arbeidstakerVarslingService: ArbeidstakerVarslingService
) {

    /**
     * Oppretter en ny innsending for et skjema med referanseId.
     */
    @Transactional
    fun opprettInnsending(
        skjema: Skjema,
        referanseId: String,
        skjemaDefinisjonVersjon: String,
        innsendtSprak: Språk,
        innsenderFnr: String
    ): Innsending {
        val innsending = Innsending(
            skjema = skjema,
            status = InnsendingStatus.MOTTATT,
            referanseId = referanseId,
            skjemaDefinisjonVersjon = skjemaDefinisjonVersjon,
            innsendtSprak = innsendtSprak,
            innsenderFnr = innsenderFnr
        )
        return innsendingRepository.save(innsending)
    }

    /**
     * Prosesserer en innsendt søknad.
     *
     * Kalles fra InnsendingEventListener (etter commit) og fra retryFeiledeInnsendinger.
     */
    @Transactional
    fun prosesserInnsending(skjemaId: UUID) {
        log.info { "Starter prosessering av skjema $skjemaId" }

        try {
            // Marker som under behandling (med sisteForsoek for hung detection)
            startProsessering(skjemaId)

            val relaterteSkjemaIder = samleRelaterteSkjemaIder(skjemaId)

            skjemaMottattProducer.blokkerendeSendSkjemaMottatt(
                SkjemaMottattMelding(
                    skjemaId = skjemaId,
                    relaterteSkjemaIder = relaterteSkjemaIder,
                    gruppeId = tildelEllerGjenbrukGruppeId(skjemaId, relaterteSkjemaIder)
                )
            )

            oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)
            log.info { "Fullført prosessering av skjema $skjemaId" }

        } catch (e: SendSkjemaMottattMeldingFeilet) {
            log.error(e) { "Kafka-feil ved prosessering av skjema $skjemaId" }
            oppdaterStatus(skjemaId, InnsendingStatus.KAFKA_FEILET, feilmelding = e.message)
        }

        varsleArbeidstakerHvisIkkeAlleredeVarslet(skjemaId)
    }

    private fun varsleArbeidstakerHvisIkkeAlleredeVarslet(skjemaId: UUID) {
        val innsending = innsendingRepository.findBySkjemaId(skjemaId) ?: return

        if (innsending.brukervarselSendt) {
            log.debug { "Arbeidstaker allerede varslet for skjema $skjemaId, hopper over" }
            return
        }

        try {
            arbeidstakerVarslingService.varsleArbeidstakerHvisAktuelt(skjemaId)
            innsending.brukervarselSendt = true
            innsendingRepository.save(innsending)
        } catch (e: Exception) {
            log.error(e) { "Feil ved varsling av arbeidstaker for skjema $skjemaId - fortsetter" }
        }
    }

    /**
     * Oppdaterer innsendingsstatus og inkrementerer antallForsok.
     */
    private fun oppdaterStatus(
        skjemaId: UUID,
        status: InnsendingStatus,
        feilmelding: String? = null
    ) {
        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: error("Innsending for skjema $skjemaId ikke funnet")

        innsending.status = status
        innsending.antallForsok += 1
        innsending.sisteForsoekTidspunkt = Instant.now()

        if (feilmelding != null) {
            innsending.feilmelding = feilmelding.take(2000)
        }

        innsendingRepository.save(innsending)
        log.debug { "Oppdatert innsendingStatus til $status for skjema $skjemaId" }
    }

    /**
     * Setter status til UNDER_BEHANDLING for å markere at prosessering er startet.
     * Oppdaterer også sisteForsoekTidspunkt for å kunne detektere "hengende" prosesseringer.
     */

    private fun startProsessering(skjemaId: UUID) {
        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: error("Innsending for skjema $skjemaId ikke funnet")

        innsending.status = InnsendingStatus.UNDER_BEHANDLING
        innsending.sisteForsoekTidspunkt = Instant.now()
        innsendingRepository.save(innsending)
        log.debug { "Startet prosessering av skjema $skjemaId" }
    }

    fun hentRetryKandidater(sisteForsoekTidspunktGrense: Instant, maxAttempts: Int): List<Innsending> {
        return innsendingRepository.findRetryKandidater(sisteForsoekTidspunktGrense, maxAttempts)
    }

    private fun samleRelaterteSkjemaIder(skjemaId: UUID): List<UUID> {
        val skjema = skjemaRepository.findByIdAndStatusSendt(skjemaId) ?: return emptyList()
        val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata ?: return emptyList()
        val skjemaPeriode = skjema.utsendelsePeriode()
        if (skjemaPeriode == null) {
            log.warn { "Skjema $skjemaId mangler utsendelsesperiode — kan ikke finne relaterte søknader" }
            return emptyList()
        }

        // Finn alle SENDT-søknader med samme FNR + juridisk enhet + overlappende periode
        val relaterte = skjemaRepository
            .findByFnrAndTypeAndStatus(skjema.fnr, SkjemaType.UTSENDT_ARBEIDSTAKER, SkjemaStatus.SENDT)
            .filter { it.id != skjemaId }
            .filter { (it.metadata as? UtsendtArbeidstakerMetadata)?.juridiskEnhetOrgnr == metadata.juridiskEnhetOrgnr }
            .filter { kandidat -> kandidat.utsendelsePeriode()?.let { skjemaPeriode.overlapper(it) } == true }

        val ider = relaterte.mapNotNull { it.id }.toMutableSet()

        // Inkluder også eksplisitt koblet motpart-skjema (kan ha annen juridisk enhet ved koblingsfeil)
        metadata.kobletSkjemaId?.let { ider.add(it) }

        log.info { "Fant ${ider.size} relaterte skjemaer for skjema $skjemaId" }
        return ider.toList()
    }

    /**
     * Finner gruppe-ID-en for skjemaet, og tildeler den hvis gruppen ikke har en fra før.
     *
     * Alle deler av samme søknad skal dele én stabil gruppe-ID, slik at melosys-api kan serialisere
     * behandlingen av dem (MELOSYS-8151). ID-en persisteres i stedet for å regnes ut på nytt per
     * melding, fordi en ren utregning ikke er stabil: gruppen ses via SENDT-skjemaer, så et
     * tidligere opprettet utkast som sendes inn senere ville flyttet «tidligst opprettede» og gitt
     * delene ulik gruppe-ID — nøyaktig de tilfellene serialiseringen skal fange.
     *
     * Regler:
     * 1. Har noen i gruppen (inkludert skjemaer som ennå er utkast) allerede en gruppe-ID,
     *    gjenbrukes den. Da spiller rekkefølgen ingen rolle.
     * 2. Ellers tildeles ID-en til det tidligst opprettede skjemaet i gruppen, med skjema-ID som
     *    deterministisk tie-break. To deler som sendes samtidig og ser samme gruppe kommer dermed
     *    fram til samme verdi uten koordinering.
     *
     * Returnerer null hvis gruppe-ID ikke kan utledes; da faller melosys-api tilbake til å
     * serialisere kun på skjemaId, altså dagens oppførsel.
     */
    private fun tildelEllerGjenbrukGruppeId(skjemaId: UUID, relaterteSkjemaIder: List<UUID>): UUID? {
        val skjema = skjemaRepository.findById(skjemaId).orElse(null) ?: return null

        skjema.gruppeId?.let { return it }

        // Hent relaterte uavhengig av status: et utkast som ennå ikke er sendt kan allerede ha fått
        // tildelt gruppe-ID av en annen del, og den skal gjenbrukes.
        val gruppen = (skjemaRepository.findAllById(relaterteSkjemaIder) + skjema).distinctBy { it.id }

        val eksisterende = gruppen.mapNotNull { it.gruppeId }.distinct()
        val gruppeId = when {
            eksisterende.size == 1 -> eksisterende.single()

            eksisterende.size > 1 -> {
                // To tidligere adskilte grupper har møttes (f.eks. via en ny kobling). Velg
                // deterministisk, slik at alle deler ender på samme verdi ved neste innsending.
                val valgt = eksisterende.minBy { it.toString() }
                log.warn {
                    "Skjema $skjemaId ser flere gruppe-ID-er (${eksisterende.joinToString()}) — velger $valgt"
                }
                valgt
            }

            else -> gruppen
                .sortedWith(compareBy({ it.opprettetDato }, { it.id.toString() }))
                .first().id
        }

        if (gruppeId == null) {
            log.warn { "Kunne ikke utlede gruppe-ID for skjema $skjemaId — faller tilbake til skjemaId-serialisering" }
            return null
        }

        skjema.gruppeId = gruppeId
        skjemaRepository.save(skjema)
        log.info { "Skjema $skjemaId tilknyttet gruppe $gruppeId (${gruppen.size} deler)" }
        return gruppeId
    }

}
