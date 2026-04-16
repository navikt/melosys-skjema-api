package no.nav.melosys.skjema.types.m2m

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegistrerSaksnummerRequest(
    @field:NotBlank(message = "Saksnummer kan ikke være tomt")
    @field:Size(max = 99, message = "Saksnummer kan ikke være lengre enn 99 tegn")
    val saksnummer: String
)
