package no.nav.melosys.skjema.types.felles

import jakarta.validation.constraints.NotBlank

data class SimpleOrganisasjonDto(
    @field:NotBlank
    val orgnr: String,
    @field:NotBlank
    val navn: String
)
