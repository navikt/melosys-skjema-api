package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Skjema
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

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
     */
    @Async
    fun prosesserInnsendingAsync(skjema: Skjema) {
        log.info { "Starter asynkron prosessering av skjema ${skjema.id}" }

        try {
            // Sett UNDER_BEHANDLING for å forhindre at scheduler plukker opp samme innsending
            innsendingStatusService.oppdaterStatusUtenInkrementForsok(skjema, InnsendingStatus.UNDER_BEHANDLING)

            // TODO MELOSYS-7759: Journalfør til Joark
            // val journalpostId = joarkService.journalfor(skjema)
            // innsendingStatusService.oppdaterSkjemaJournalpostId(skjema, journalpostId)
            // innsendingStatusService.oppdaterStatus(skjema, InnsendingStatus.JOURNALFORT)

            // TODO MELOSYS-7760: Send til Kafka
            // kafkaProducer.sendSoknadMottatt(skjema)
            // innsendingStatusService.oppdaterStatus(skjema, InnsendingStatus.FERDIG)

            // TODO MELOSYS-7763: Varsle arbeidstaker (best effort)
            // varselService.varsleArbeidstaker(skjema)

            // STUB: Marker som ferdig for nå
            innsendingStatusService.oppdaterStatus(skjema, InnsendingStatus.FERDIG)
            log.info { "Fullført prosessering av skjema ${skjema.id}" }

        } catch (e: Exception) {
            log.error(e) { "Feil ved prosessering av skjema ${skjema.id}" }
            innsendingStatusService.oppdaterStatus(skjema, InnsendingStatus.JOURNALFORING_FEILET, feilmelding = e.message)
        }
    }
}
