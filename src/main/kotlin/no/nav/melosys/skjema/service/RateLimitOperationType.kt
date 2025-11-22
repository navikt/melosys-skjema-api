package no.nav.melosys.skjema.service

/**
 * Typer av operasjoner som har rate limiting.
 */
enum class RateLimitOperationType {
    ORGANISASJONSSOK,
    PERSONVERIFISERING
}
