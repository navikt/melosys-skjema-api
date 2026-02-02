package no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FamiliemedlemmerDto(
    val skalHaMedFamiliemedlemmer: Boolean,
    @field:Valid
    val familiemedlemmer: List<Familiemedlem>,
)