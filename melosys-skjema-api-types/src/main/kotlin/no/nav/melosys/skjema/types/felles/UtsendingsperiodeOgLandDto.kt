package no.nav.melosys.skjema.types.felles

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendingsperiodeOgLandDto(
    val utsendelseLand: LandKode,
    @field:Valid
    val utsendelsePeriode: PeriodeDto
)
