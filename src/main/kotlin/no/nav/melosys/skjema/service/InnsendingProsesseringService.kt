package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.domain.InnsendingStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

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
    private val innsendingStatusService: InnsendingStatusService
) {

    /**
     * Prosesserer en innsendt søknad asynkront.
     *
     * Kalles fra SkjemaService.submit() og fra InnsendingRetryScheduler.
     * @Async gjør at denne metoden kjører i en egen tråd fra ThreadPool,
     * og kallet returnerer umiddelbart til caller.
     *
     * Merk: Race conditions mellom pods håndteres av ShedLock på scheduleren.
     * Race mellom submit og scheduler håndteres av threshold (kun retry av gamle MOTTATT).
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

            // TODO MELOSYS-7760: Send til Kafka
            // kafkaProducer.sendSoknadMottatt(skjemaId)

            // TODO MELOSYS-7763: Varsle arbeidstaker (best effort)
            // varselService.varsleArbeidstaker(skjemaId)

            // STUB: Marker som ferdig for nå
            innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)
            log.info { "Fullført prosessering av skjema $skjemaId" }

        } catch (e: Exception) {
            log.error(e) { "Feil ved prosessering av skjema $skjemaId" }
            innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.JOURNALFORING_FEILET, feilmelding = e.message)
        }
    }
}
