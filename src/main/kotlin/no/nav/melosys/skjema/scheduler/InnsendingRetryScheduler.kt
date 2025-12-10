package no.nav.melosys.skjema.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.InnsendingRetryConfig
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.service.InnsendingProsesseringService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * Scheduled job for retry av feilede innsendinger.
 *
 * Kjører periodisk og plukker opp:
 * - Innsendinger som aldri ble prosessert (MOTTATT status eldre enn threshold)
 * - Innsendinger som feilet men har færre enn maks forsøk
 *
 * Konfigurasjon via application.yml under `innsending.retry.*`
 */
@Component
class InnsendingRetryScheduler(
    private val innsendingRepository: InnsendingRepository,
    private val innsendingProsesseringService: InnsendingProsesseringService,
    private val retryConfig: InnsendingRetryConfig
) {

    @Scheduled(
        fixedDelayString = "#{@innsendingRetryConfig.getFixedDelayMillis()}",
        initialDelayString = "#{@innsendingRetryConfig.getInitialDelayMillis()}"
    )
    fun retryFeiledeInnsendinger() {
        log.debug { "Kjører retry-jobb for feilede innsendinger" }

        val grense = Instant.now().minus(retryConfig.staleThresholdMinutes, ChronoUnit.MINUTES)
        val kandidater = innsendingRepository.findRetryKandidater(grense, retryConfig.maxAttempts)

        if (kandidater.isNotEmpty()) {
            log.info { "Fant ${kandidater.size} innsendinger for retry" }
        }

        kandidater.forEach { innsending ->
            try {
                log.info { "Starter retry av innsending for skjema ${innsending.skjema.id}" }
                innsendingProsesseringService.prosesserInnsendingAsync(innsending.skjema)
            } catch (e: Exception) {
                log.error(e) { "Feil ved retry av innsending for skjema ${innsending.skjema.id}" }
            }
        }
    }
}
