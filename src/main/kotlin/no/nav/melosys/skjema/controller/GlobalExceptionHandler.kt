package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.RateLimitConfig
import no.nav.melosys.skjema.controller.dto.ErrorResponse
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.exception.SkjemaAlleredeSendtException
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import no.nav.melosys.skjema.integrasjon.pdl.exception.PersonVerifiseringException
import no.nav.melosys.skjema.service.exception.RateLimitExceededException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = mutableMapOf<String, String>()

        // Get all constraint violations to extract property paths
        val violations = ex.bindingResult.allErrors.mapNotNull { error ->
            error.unwrap(jakarta.validation.ConstraintViolation::class.java)
        }

        violations.forEach { violation ->
            val fieldName = violation.propertyPath.toString().takeIf { it.isNotEmpty() } ?: "global"
            errors[fieldName] = violation.message
        }

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(message = "Valideringsfeil", errors = errors))
    }

    @ExceptionHandler(OrganisasjonEksistererIkkeException::class)
    fun handleOrganisasjonEksistererIkke(e: OrganisasjonEksistererIkkeException): ResponseEntity<Map<String, String>> {
        log.warn { "Organisasjon eksisterer ikke: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("message" to e.message!!))
    }

    @ExceptionHandler(SkjemaAlleredeSendtException::class)
    fun handleSkjemaAlleredeSendt(e: SkjemaAlleredeSendtException): ResponseEntity<ErrorResponse> {
        log.warn { "Organisasjon eksisterer ikke: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(message = e.message ?: ""))
    }

    @ExceptionHandler(PersonVerifiseringException::class)
    fun handlePersonVerifiseringFeilet(e: PersonVerifiseringException): ResponseEntity<Map<String, String>> {
        log.warn { "Verifisering av person feilet: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to "Finner ikke person med oppgitt fødselsnummer og etternavn"))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<Map<String, String>> {
        log.warn { "Tilgang nektet: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(mapOf("message" to "Ingen tilgang"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        log.warn { "Ugyldig forespørsel: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to (e.message ?: "Ugyldig forespørsel")))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<Map<String, String>> {
        log.warn { "Ressurs ikke funnet: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("message" to (e.message ?: "Ressurs ikke funnet")))
    }
}
