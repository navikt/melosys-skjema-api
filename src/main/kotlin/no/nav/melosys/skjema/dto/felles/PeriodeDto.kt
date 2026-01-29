package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PeriodeDto(
    val fraDato: LocalDate,
    val tilDato: LocalDate
)
