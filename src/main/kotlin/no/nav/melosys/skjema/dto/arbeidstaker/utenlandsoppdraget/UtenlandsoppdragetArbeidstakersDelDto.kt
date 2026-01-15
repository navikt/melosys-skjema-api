package no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.dto.felles.LandKode
import no.nav.melosys.skjema.dto.felles.PeriodeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandsoppdragetArbeidstakersDelDto(
    val utsendelsesLand: LandKode,
    @field:Valid
    val utsendelsePeriode: PeriodeDto
)
