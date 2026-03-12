package no.nav.melosys.skjema.types.felles

data class OrganisasjonMedJuridiskEnhetDto(
    val organisasjon: SimpleOrganisasjonDto,
    val juridiskEnhet: SimpleOrganisasjonDto
)