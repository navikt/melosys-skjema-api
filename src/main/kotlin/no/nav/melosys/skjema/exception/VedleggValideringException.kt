package no.nav.melosys.skjema.exception

/**
 * Valideringsfeil for vedlegg som skal vises til brukeren.
 * [errorCode] mappes til oversatt feilmelding i frontend (samme mønster som VIRUS_FOUND).
 */
class VedleggValideringException(message: String, val errorCode: String) : RuntimeException(message)
