package no.nav.melosys.skjema.dto.arbeidstaker

import jakarta.validation.constraints.NotBlank
import no.nav.melosys.skjema.controller.validators.ErFodselsEllerDNummer

data class CreateArbeidstakerSkjemaRequest(
    @field:NotBlank
    @field:ErFodselsEllerDNummer
    val fnr: String
)