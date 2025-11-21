package no.nav.melosys.skjema.integrasjon.ereg.exception

class OrganisasjonEksistererIkkeException(
    organisasjonsnummer: String
) : RuntimeException("Organisasjon med organisasjonsnummer $organisasjonsnummer eksisterer ikke")