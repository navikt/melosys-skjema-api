package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiveren

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import no.nav.melosys.skjema.controller.validators.ErOrganisasjonsnummer

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiverenDto(
    @field:NotBlank
    @field:ErOrganisasjonsnummer
    val organisasjonsnummer: String,
    val organisasjonNavn: String
)