package no.nav.melosys.skjema.dto.arbeidsgiver

import jakarta.validation.constraints.NotBlank
import no.nav.melosys.skjema.controller.validators.ErOrganisasjonsnummer

data class CreateArbeidsgiverSkjemaRequest(
    @field:NotBlank
    @field:ErOrganisasjonsnummer
    val orgnr: String
)