package no.nav.melosys.skjema.event

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.observability.MDCOperations.Companion.withCorrelationId
import no.nav.melosys.skjema.service.InnsendingService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = KotlinLogging.logger {}

/**
 * Event listener for innsending-hendelser.
 *
 * Skilt ut fra InnsendingService for å unngå self-invocation problemet
 * med Spring @Transactional. Når denne klassen kaller InnsendingService.prosesserInnsending(),
 * går kallet gjennom Spring-proxyen slik at @Transactional fungerer korrekt.
 */
@Component
class InnsendingEventListener(
    private val innsendingService: InnsendingService
) {

    /**
     * Lytter på InnsendingOpprettetEvent og starter prosessering ETTER at transaksjonen er committed.
     * Dette sikrer at innsending-raden finnes i databasen før vi prøver å lese den.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onInnsendingOpprettet(innsendingOpprettetEvent: InnsendingOpprettetEvent) {
        withCorrelationId(innsendingOpprettetEvent.correlationId) {
            log.info { "Mottok InnsendingOpprettetEvent for skjema ${innsendingOpprettetEvent.skjemaId}" }
            innsendingService.prosesserInnsending(innsendingOpprettetEvent.skjemaId)
        }
    }
}
