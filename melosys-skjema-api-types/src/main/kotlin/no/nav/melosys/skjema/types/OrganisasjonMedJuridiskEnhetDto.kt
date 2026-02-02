package no.nav.melosys.skjema.types

data class OrganisasjonMedJuridiskEnhetDto(
    val organisasjon: SimpleOrganisasjonDto,
    val juridiskEnhet: SimpleOrganisasjonDto
)