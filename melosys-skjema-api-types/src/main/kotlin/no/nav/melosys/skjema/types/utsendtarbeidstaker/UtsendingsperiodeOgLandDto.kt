package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendingsperiodeOgLandDto(
    val utsendelseLand: LandKode,
    @field:Valid
    val utsendelsePeriode: PeriodeDto
)