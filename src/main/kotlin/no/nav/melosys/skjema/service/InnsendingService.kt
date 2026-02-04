package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.types.common.Språk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.kafka.SkjemaMottattProducer
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding


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
    private val skjemaMottattProducer: SkjemaMottattProducer
) {

    /**
     * Oppretter en ny innsending for et skjema med referanseId.
     */
    @Transactional
    fun opprettInnsending(
        skjema: Skjema,
        referanseId: String,
        skjemaDefinisjonVersjon: String,
        innsendtSprak: Språk
    ): Innsending {
        val innsending = Innsending(
            skjema = skjema,
            status = InnsendingStatus.MOTTATT,
            referanseId = referanseId,
            skjemaDefinisjonVersjon = skjemaDefinisjonVersjon,
            innsendtSprak = innsendtSprak
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
        log.info { "Starter asynkron prosessering av skjema $skjemaId" }

        try {
            // Marker som under behandling (med sisteForsoek for hung detection)
            startProsessering(skjemaId)

            // TODO MELOSYS-7759: Journalfør til Joark
            // val journalpostId = joarkService.journalfor(skjemaId)
            // innsendingStatusService.oppdaterSkjemaJournalpostId(skjemaId, journalpostId)
            // innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.JOURNALFORT)

            // MELOSYS-7760: Send til Kafka
            skjemaMottattProducer.blokkerendeSendSkjemaMottatt(
                SkjemaMottattMelding(skjemaId = skjemaId)
            )

            // TODO MELOSYS-7763: Varsle arbeidstaker (best effort)
            // varselService.varsleArbeidstaker(skjemaId)

            oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)
            log.info { "Fullført prosessering av skjema $skjemaId" }

        } catch (e: SendSkjemaMottattMeldingFeilet) {
            log.error(e) { "Kafka-feil ved prosessering av skjema $skjemaId" }
            oppdaterStatus(skjemaId, InnsendingStatus.KAFKA_FEILET, feilmelding = e.message)
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
}
