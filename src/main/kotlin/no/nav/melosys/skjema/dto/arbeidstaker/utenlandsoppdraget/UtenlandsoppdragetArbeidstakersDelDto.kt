package no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import no.nav.melosys.skjema.controller.validators.utenlandsoppdraget.GyldigUtenlandsoppdragetArbeidstaker

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigUtenlandsoppdragetArbeidstaker
data class UtenlandsoppdragetArbeidstakersDelDto(
    val utsendelsesLand: String,
    val utsendelseFraDato: LocalDate,
    val utsendelseTilDato: LocalDate
)
