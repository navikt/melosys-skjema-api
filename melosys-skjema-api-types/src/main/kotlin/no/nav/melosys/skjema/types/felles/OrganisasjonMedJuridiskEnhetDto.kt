package no.nav.melosys.skjema.types.felles

data class OrganisasjonMedJuridiskEnhetDto(
    val organisasjon: SimpleOrganisasjonDto,
    val juridiskEnhet: SimpleOrganisasjonDto,
    val erOffentligArbeidsgiver: Boolean,
    /** Juridisk enhet har enhetstype norskregistrert utenlandsk foretak (NUF). */
    val erNorskregistrertUtenlandskForetak: Boolean,
    /** Antall ansatte i A-registeret for juridisk enhet. 0 når EREG ikke har registrert ansatte. */
    val antallAnsatte: Int
)