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
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
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

            skjemaMottattProducer.blokkerendeSendSkjemaMottatt(
                SkjemaMottattMelding(
                    skjemaId = skjemaId,
                    relaterteSkjemaIder = samleRelaterteSkjemaIder(skjemaId)
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
        val skjemaPeriode = hentPeriode(skjema)
        if (skjemaPeriode == null) {
            log.warn { "Skjema $skjemaId mangler utsendelsesperiode — kan ikke finne relaterte søknader" }
            return emptyList()
        }

        // Finn alle SENDT-søknader med samme FNR + juridisk enhet + overlappende periode
        val relaterte = skjemaRepository
            .findByFnrAndTypeAndStatus(skjema.fnr, SkjemaType.UTSENDT_ARBEIDSTAKER, SkjemaStatus.SENDT)
            .filter { it.id != skjemaId }
            .filter { (it.metadata as? UtsendtArbeidstakerMetadata)?.juridiskEnhetOrgnr == metadata.juridiskEnhetOrgnr }
            .filter { kandidat -> hentPeriode(kandidat)?.let { skjemaPeriode.overlapper(it) } == true }

        val ider = relaterte.mapNotNull { it.id }.toMutableSet()

        // Inkluder også eksplisitt koblet motpart-skjema (kan ha annen juridisk enhet ved koblingsfeil)
        metadata.kobletSkjemaId?.let { ider.add(it) }

        log.info { "Fant ${ider.size} relaterte skjemaer for skjema $skjemaId" }
        return ider.toList()
    }

    private fun hentPeriode(skjema: Skjema) =
        (skjema.data as? UtsendtArbeidstakerSkjemaData)?.utsendingsperiodeOgLand?.utsendelsePeriode
}
