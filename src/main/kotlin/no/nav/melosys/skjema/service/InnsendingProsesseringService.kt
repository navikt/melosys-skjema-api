package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Service for asynkron prosessering av innsendte søknader.
 *
 * Håndterer journalføring til Joark, Kafka-sending og varsling.
 * Metoder merket med @Async kjører i egen tråd fra ThreadPool.
 */
@Service
class InnsendingProsesseringService(
    private val innsendingRepository: InnsendingRepository,
    private val skjemaRepository: SkjemaRepository
) {

    /**
     * Oppretter en ny innsending for et skjema.
     * Kalles fra SkjemaService når bruker sender inn.
     */
    @Transactional
    fun opprettInnsending(skjema: Skjema): Innsending {
        val innsending = Innsending(
            skjema = skjema,
            status = InnsendingStatus.MOTTATT
        )
        return innsendingRepository.save(innsending)
    }

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
            // TODO MELOSYS-7759: Journalfør til Joark
            // val journalpostId = joarkService.journalfor(skjema)
            // oppdaterSkjemaJournalpostId(skjema, journalpostId)
            // oppdaterStatus(skjema, InnsendingStatus.JOURNALFORT)

            // TODO MELOSYS-7760: Send til Kafka
            // kafkaProducer.sendSoknadMottatt(skjema)
            // oppdaterStatus(skjema, InnsendingStatus.FERDIG)

            // TODO MELOSYS-7763: Varsle arbeidstaker (best effort)
            // varselService.varsleArbeidstaker(skjema)

            // STUB: Marker som ferdig for nå
            oppdaterStatus(skjema, InnsendingStatus.FERDIG)
            log.info { "Fullført prosessering av skjema ${skjema.id}" }

        } catch (e: Exception) {
            log.error(e) { "Feil ved prosessering av skjema ${skjema.id}" }
            oppdaterStatus(skjema, InnsendingStatus.JOURNALFORING_FEILET, feilmelding = e.message)
        }
    }

    /**
     * Oppdaterer innsendingsstatus.
     */
    @Transactional
    fun oppdaterStatus(
        skjema: Skjema,
        status: InnsendingStatus,
        feilmelding: String? = null
    ) {
        val innsending = innsendingRepository.findBySkjema(skjema)
            ?: throw IllegalArgumentException("Innsending for skjema ${skjema.id} ikke funnet")

        innsending.status = status
        innsending.antallForsok += 1
        innsending.sisteForsoek = Instant.now()

        if (feilmelding != null) {
            innsending.feilmelding = feilmelding
        }

        innsendingRepository.save(innsending)
        log.debug { "Oppdatert innsendingStatus til $status for skjema ${skjema.id}" }
    }

    /**
     * Oppdaterer journalpostId på skjemaet.
     */
    @Transactional
    fun oppdaterSkjemaJournalpostId(skjema: Skjema, journalpostId: String) {
        skjema.journalpostId = journalpostId
        skjemaRepository.save(skjema)
    }
}
