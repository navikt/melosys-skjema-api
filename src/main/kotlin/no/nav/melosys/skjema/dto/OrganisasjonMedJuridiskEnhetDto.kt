package no.nav.melosys.skjema.dto

data class OrganisasjonMedJuridiskEnhetDto(
    val organisasjon: SimpleOrganisasjonDto,
    val juridiskEnhet: SimpleOrganisasjonDto
)