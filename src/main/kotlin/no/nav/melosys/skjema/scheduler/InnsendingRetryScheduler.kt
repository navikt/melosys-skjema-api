package no.nav.melosys.skjema.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.melosys.skjema.config.InnsendingRetryConfig
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.service.InnsendingProsesseringService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import no.nav.melosys.skjema.config.observability.MDCOperations
import no.nav.melosys.skjema.config.observability.MDCOperations.Companion.withCorrelationId

private val log = KotlinLogging.logger {}

/**
 * Scheduled job for retry av feilede innsendinger.
 *
 * Kjører periodisk og plukker opp:
 * - Innsendinger som aldri ble prosessert (MOTTATT status eldre enn threshold)
 * - Innsendinger med UNDER_BEHANDLING som har hengt for lenge (app krasjet)
 * - Innsendinger som feilet men har færre enn maks forsøk
 *
 * Bruker ShedLock for å sikre at kun én pod kjører jobben om gangen.
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
    @SchedulerLock(name = "retryFeiledeInnsendinger", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    fun retryFeiledeInnsendinger() {
        // Verifiserer at ShedLock er riktig konfigurert (kaster exception hvis ikke)
        LockAssert.assertLocked()
        log.debug { "Kjører retry-jobb for feilede innsendinger" }

        val sisteForsoekTidspunktGrense = Instant.now().minus(retryConfig.staleThresholdMinutes, ChronoUnit.MINUTES)
        val kandidater = innsendingRepository.findRetryKandidater(sisteForsoekTidspunktGrense, retryConfig.maxAttempts)

        if (kandidater.isEmpty()) {
            return
        }

        log.info { "Fant ${kandidater.size} innsendinger for retry" }

        kandidater.forEach { innsending ->
            try {
                withCorrelationId(innsending.correlationId){
                    val skjemaId = innsending.skjema.id!!
                    log.info { "Starter retry av innsending for skjema $skjemaId" }
                    innsendingProsesseringService.prosesserInnsendingAsync(skjemaId)
                }
            } catch (e: Exception) {
                log.error(e) { "Feil ved retry av innsending for skjema ${innsending.skjema.id}" }
            }
        }
    }
}
