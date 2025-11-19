package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakeren

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import no.nav.melosys.skjema.controller.validators.ErFodselsEllerDNummer

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakerenDto(
    @field:NotBlank
    @field:ErFodselsEllerDNummer
    val fodselsnummer: String
)
