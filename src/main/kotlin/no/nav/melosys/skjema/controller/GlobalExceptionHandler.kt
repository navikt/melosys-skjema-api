package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.RateLimitConfig
import no.nav.melosys.skjema.controller.dto.ErrorResponse
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.exception.SkjemaErIkkeRedigerbartException
import no.nav.melosys.skjema.exception.SkjemaTypeMismatchException
import no.nav.melosys.skjema.exception.VedleggVirusFunnetException
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import no.nav.melosys.skjema.integrasjon.pdl.exception.PersonVerifiseringException
import no.nav.melosys.skjema.service.exception.RateLimitExceededException
import no.nav.melosys.skjema.validators.ValidationException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

private val log = KotlinLogging.logger { }

@RestControllerAdvice
class GlobalExceptionHandler(
    private val rateLimitConfig: RateLimitConfig
) {

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceeded(e: RateLimitExceededException): ResponseEntity<Map<String, String>> {
        log.warn(e) { "Rate limit overskredet: ${e.message}" }

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

    @ExceptionHandler(ValidationException::class)
    fun handleCustomValidationException(ex: ValidationException): ResponseEntity<ErrorResponse> {
        val errors = ex.violations.associate { it.field to it.translationKey }

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(message = "Valideringsfeil", errors = errors))
    }

    @ExceptionHandler(OrganisasjonEksistererIkkeException::class)
    fun handleOrganisasjonEksistererIkke(e: OrganisasjonEksistererIkkeException): ResponseEntity<Map<String, String>> {
        log.warn(e) { "Organisasjon eksisterer ikke: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("message" to e.message!!))
    }

    @ExceptionHandler(SkjemaErIkkeRedigerbartException::class)
    fun handleSkjemaErIkkeRedigerbart(e: SkjemaErIkkeRedigerbartException): ResponseEntity<ErrorResponse> {
        log.warn(e) { "Skjema er ikke redigerbart: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(message = e.message ?: ""))
    }

    @ExceptionHandler(PersonVerifiseringException::class)
    fun handlePersonVerifiseringFeilet(e: PersonVerifiseringException): ResponseEntity<Map<String, String>> {
        log.warn(e) { "Verifisering av person feilet: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to "Finner ikke person med oppgitt fødselsnummer og etternavn"))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<Map<String, String>> {
        log.warn(e) { "Tilgang nektet: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(mapOf("message" to "Ingen tilgang"))
    }

    @ExceptionHandler(VedleggVirusFunnetException::class)
    fun handleVedleggVirusFunnet(e: VedleggVirusFunnetException): ResponseEntity<Map<String, String>> {
        log.warn(e) { "Virus funnet i vedlegg: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(mapOf("message" to (e.message ?: "Virus funnet i fil"), "error" to "VIRUS_FOUND"))
    }

    @ExceptionHandler(SkjemaTypeMismatchException::class)
    fun handleSkjemaTypeMismatch(e: SkjemaTypeMismatchException): ResponseEntity<ErrorResponse> {
        log.warn(e) { "Skjematype samsvarer ikke: ${e.message}" }

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(message = e.message ?: "Skjematype samsvarer ikke"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        log.warn(e) { "Ugyldig forespørsel: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to (e.message ?: "Ugyldig forespørsel")))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<Map<String, String>> {
        log.warn(e) { "Ressurs ikke funnet: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("message" to (e.message ?: "Ressurs ikke funnet")))
    }

    // Må fanges eksplisitt slik at manglende/ugyldig token forblir 401 og ikke blir 500 av fallbacken under.
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUautorisert(e: JwtTokenUnauthorizedException): ResponseEntity<ErrorResponse> {
        log.warn(e) { "Uautorisert forespørsel: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(message = "Ikke autentisert"))
    }

    @ExceptionHandler(Exception::class)
    fun handleUventetFeil(e: Exception): ResponseEntity<ErrorResponse> {
        // Re-kast Spring sine egne exceptions og @ResponseStatus slik at riktig statuskode beholdes (400/405/415 osv.).
        if (e.javaClass.name.startsWith("org.springframework.") ||
            e.javaClass.isAnnotationPresent(ResponseStatus::class.java)
        ) {
            throw e
        }

        log.error(e) { "Uventet feil: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(message = "Det oppstod en uventet feil"))
    }
}
