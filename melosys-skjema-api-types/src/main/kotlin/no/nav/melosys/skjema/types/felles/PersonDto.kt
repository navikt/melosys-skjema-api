package no.nav.melosys.skjema.types.felles

import jakarta.validation.constraints.NotBlank

data class PersonDto(
    @field:NotBlank
    val fnr: String,
    val etternavn: String? = null  // Kun nødvendig for PDL-validering uten fullmakt
)
