package no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandsoppdragetArbeidstakersDelDto(
    val utsendelsesLand: String,
    val utsendelseFraDato: LocalDate,
    val utsendelseTilDato: LocalDate
)
