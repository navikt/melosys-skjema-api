package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import no.nav.melosys.skjema.controller.validators.felles.ErOrganisasjonsnummer

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NorskVirksomhet(
    @field:NotBlank
    @field:ErOrganisasjonsnummer
    val organisasjonsnummer: String
)