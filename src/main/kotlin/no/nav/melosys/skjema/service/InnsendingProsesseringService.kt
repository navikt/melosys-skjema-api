package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.domain.InnsendingMetadata
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.dto.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.repository.SkjemaRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
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
    private val skjemaRepository: SkjemaRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * Prosesserer en innsendt søknad asynkront.
     *
     * Kalles fra SkjemaService.submit() og fra InnsendingRetryScheduler.
     * @Async gjør at denne metoden kjører i en egen tråd fra ThreadPool,
     * og kallet returnerer umiddelbart til caller.
     */
    @Async
    fun prosesserInnsendingAsync(skjemaId: UUID) {
        log.info { "Starter asynkron prosessering av skjema $skjemaId" }

        try {
            // TODO MELOSYS-7759: Journalfør til Joark
            // val journalpostId = joarkService.journalfor(skjemaId)
            // oppdaterStatus(skjemaId, InnsendingStatus.JOURNALFORT, journalpostId = journalpostId)

            // TODO MELOSYS-7760: Send til Kafka
            // kafkaProducer.sendSoknadMottatt(skjemaId)
            // oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)

            // TODO MELOSYS-7763: Varsle arbeidstaker (best effort)
            // varselService.varsleArbeidstaker(skjemaId)

            // STUB: Marker som ferdig for nå
            oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)
            log.info { "Fullført prosessering av skjema $skjemaId" }

        } catch (e: Exception) {
            log.error(e) { "Feil ved prosessering av skjema $skjemaId" }
            oppdaterStatus(skjemaId, InnsendingStatus.JOURNALFORING_FEILET, feilmelding = e.message)
        }
    }

    /**
     * Oppdaterer innsendingsstatus i skjemaets metadata.
     */
    fun oppdaterStatus(
        skjemaId: UUID,
        status: InnsendingStatus,
        journalpostId: String? = null,
        feilmelding: String? = null
    ) {
        val skjema = skjemaRepository.findById(skjemaId).orElseThrow {
            IllegalArgumentException("Skjema $skjemaId ikke funnet")
        }

        val eksisterendeMetadata = skjema.metadata?.let {
            objectMapper.treeToValue(it, UtsendtArbeidstakerMetadata::class.java)
        } ?: return

        val eksisterendeInnsending = eksisterendeMetadata.innsending

        val oppdatertInnsending = InnsendingMetadata(
            status = status,
            journalpostId = journalpostId ?: eksisterendeInnsending?.journalpostId,
            referanseId = eksisterendeInnsending?.referanseId,
            feilmelding = feilmelding,
            antallForsok = (eksisterendeInnsending?.antallForsok ?: 0) + 1,
            sisteForsoek = Instant.now()
        )

        val oppdatertMetadata = eksisterendeMetadata.copy(innsending = oppdatertInnsending)
        skjema.metadata = objectMapper.valueToTree(oppdatertMetadata)
        skjema.endretDato = Instant.now()

        skjemaRepository.save(skjema)
        log.debug { "Oppdatert innsendingStatus til $status for skjema $skjemaId" }
    }
}
