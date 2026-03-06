package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FamiliemedlemmerDto(
    val skalHaMedFamiliemedlemmer: Boolean,
    @field:Valid
    val familiemedlemmer: List<Familiemedlem>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Familiemedlem(
    val fornavn: String,
    val etternavn: String,
    val harNorskFodselsnummerEllerDnummer: Boolean,
    val fodselsnummer: String?,
    val fodselsdato: LocalDate?,
)
