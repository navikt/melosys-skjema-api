package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NorskVirksomhet(
    @field:NotBlank
    val organisasjonsnummer: String
)