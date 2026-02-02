package no.nav.melosys.skjema.types.common

/**
 * Overordnet status for et skjema.
 *
 * For detaljert sporing av asynkron prosessering, se [Skjema.innsendingStatus].
 */
enum class SkjemaStatus {
    /** Bruker jobber med søknaden - ikke sendt ennå */
    UTKAST,

    /** Bruker har sendt inn søknaden. Asynkron prosessering pågår eller er fullført. */
    SENDT
}
