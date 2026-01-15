package no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.controller.validators.familiemedlemmer.GyldigFamiliemedlemmer

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigFamiliemedlemmer
data class FamiliemedlemmerDto(
    val skalHaMedFamiliemedlemmer: Boolean,
    @field:Valid
    val familiemedlemmer: List<Familiemedlem>,
)