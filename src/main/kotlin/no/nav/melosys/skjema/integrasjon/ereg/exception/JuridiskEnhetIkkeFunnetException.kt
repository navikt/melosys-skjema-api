package no.nav.melosys.skjema.integrasjon.ereg.exception

class JuridiskEnhetIkkeFunnetException(
    organisasjonsnummer: String
) : RuntimeException("Fant ikke juridisk enhet for organisasjon $organisasjonsnummer")

