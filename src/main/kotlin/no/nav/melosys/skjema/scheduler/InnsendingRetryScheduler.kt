package no.nav.melosys.skjema.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.InnsendingProsesseringService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * Scheduled job for retry av feilede innsendinger.
 *
 * Kjører hvert 5. minutt og plukker opp:
 * - Søknader som aldri ble prosessert (MOTTATT status eldre enn 5 min)
 * - Søknader som feilet men har færre enn 5 forsøk
 */
@Component
class InnsendingRetryScheduler(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingProsesseringService: InnsendingProsesseringService
) {

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    fun retryFeiledeInnsendinger() {
        log.debug { "Kjører retry-jobb for feilede innsendinger" }

        val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
        val kandidater = skjemaRepository.findRetryKandidater(grense)

        if (kandidater.isNotEmpty()) {
            log.info { "Fant ${kandidater.size} søknader for retry" }
        }

        kandidater.forEach { skjema ->
            try {
                log.info { "Starter retry av skjema ${skjema.id}" }
                innsendingProsesseringService.prosesserInnsendingAsync(skjema.id!!)
            } catch (e: Exception) {
                log.error(e) { "Feil ved retry av skjema ${skjema.id}" }
            }
        }
    }
}
