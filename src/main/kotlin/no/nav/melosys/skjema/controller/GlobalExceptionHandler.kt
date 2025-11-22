package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.RateLimitConfig
import no.nav.melosys.skjema.service.exception.RateLimitExceededException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val log = KotlinLogging.logger { }

@RestControllerAdvice
class GlobalExceptionHandler(
    private val rateLimitConfig: RateLimitConfig
) {

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceeded(e: RateLimitExceededException): ResponseEntity<Map<String, String>> {
        log.warn { "Rate limit overskredet: ${e.message}" }

        val config = rateLimitConfig.getConfigFor(e.operationType)
        val retryAfterSeconds = config.getTimeWindow().seconds

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", retryAfterSeconds.toString())
            .body(mapOf("message" to "For mange søk. Prøv igjen senere."))
    }
}
