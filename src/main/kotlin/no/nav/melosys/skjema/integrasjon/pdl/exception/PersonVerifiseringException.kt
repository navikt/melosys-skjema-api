package no.nav.melosys.skjema.integrasjon.pdl.exception

/**
 * Kastes når verifisering av person feiler (fødselsnummer + etternavn matcher ikke)
 */
class PersonVerifiseringException(message: String) : RuntimeException(message)
