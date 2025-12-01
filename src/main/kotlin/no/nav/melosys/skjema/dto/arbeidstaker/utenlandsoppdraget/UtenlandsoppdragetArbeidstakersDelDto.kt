package no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.controller.validators.utenlandsoppdraget.GyldigUtenlandsoppdragetArbeidstaker
import no.nav.melosys.skjema.dto.felles.PeriodeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigUtenlandsoppdragetArbeidstaker
data class UtenlandsoppdragetArbeidstakersDelDto(
    val utsendelsesLand: String,
    @field:Valid
    val utsendelsePeriode: PeriodeDto
)
