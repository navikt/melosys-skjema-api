package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import no.nav.melosys.skjema.controller.validators.felles.GyldigPeriode

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigPeriode
data class PeriodeDto(
    val fraDato: LocalDate,
    val tilDato: LocalDate
)
