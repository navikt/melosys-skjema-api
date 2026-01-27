package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.event.InnsendingOpprettetEvent
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet
import no.nav.melosys.skjema.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.kafka.SkjemaMottattProducer
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID
import no.nav.melosys.skjema.config.observability.MDCOperations

private val log = KotlinLogging.logger {}

/**
 * Service for asynkron prosessering av innsendte søknader.
 *
 * Håndterer journalføring til Joark, Kafka-sending og varsling.
 * Metoder merket med @Async kjører i egen tråd fra ThreadPool.
 *
 * Transaksjonelle databaseoperasjoner er delegert til [InnsendingStatusService]
 * for å unngå self-invocation problemet med Spring @Transactional.
 */
@Service
class InnsendingProsesseringService(
    private val innsendingStatusService: InnsendingStatusService,
    private val skjemaMottattProducer: SkjemaMottattProducer
) {

    /**
     * Lytter på InnsendingOpprettetEvent og starter async prosessering ETTER at transaksjonen er committed.
     * Dette sikrer at innsending-raden finnes i databasen før vi prøver å lese den.
     *
     * @Async her sørger for at prosesseringen kjører i tråd-pool, slik at HTTP-tråden frigjøres umiddelbart.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onInnsendingOpprettet(event: InnsendingOpprettetEvent) {
        MDCOperations.setCorrelationId(event.correlationId)
        prosesserInnsendingAsync(event.skjemaId)
    }

    /**
     * Prosesserer en innsendt søknad asynkront.
     *
     * Kalles fra event listener (etter commit) og fra InnsendingRetryScheduler.
     * @Async gjør at denne metoden kjører i en egen tråd fra ThreadPool,
     * og kallet returnerer umiddelbart til caller.
     */
    @Async
    fun prosesserInnsendingAsync(skjemaId: UUID) {
        log.info { "Starter asynkron prosessering av skjema $skjemaId" }

        try {
            // Marker som under behandling (med sisteForsoek for hung detection)
            innsendingStatusService.startProsessering(skjemaId)

            // TODO MELOSYS-7759: Journalfør til Joark
            // val journalpostId = joarkService.journalfor(skjemaId)
            // innsendingStatusService.oppdaterSkjemaJournalpostId(skjemaId, journalpostId)
            // innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.JOURNALFORT)

            // MELOSYS-7760: Send til Kafka
            skjemaMottattProducer.blokkerendeSendSkjemaMottatt(SkjemaMottattMelding(skjemaId = skjemaId))

            // TODO MELOSYS-7763: Varsle arbeidstaker (best effort)
            // varselService.varsleArbeidstaker(skjemaId)

            innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)
            log.info { "Fullført prosessering av skjema $skjemaId" }

        } catch (e: SendSkjemaMottattMeldingFeilet) {
            log.error(e) { "Kafka-feil ved prosessering av skjema $skjemaId" }
            innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.KAFKA_FEILET, feilmelding = e.message)
        } catch (e: Exception) {
            log.error(e) { "Feil ved prosessering av skjema $skjemaId" }
            innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.JOURNALFORING_FEILET, feilmelding = e.message)
        }
    }
}
