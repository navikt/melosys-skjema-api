package no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Familiemedlem(
    val fornavn: String,
    val etternavn: String,
    val harNorskFodselsnummerEllerDnummer: Boolean,
    val fodselsnummer: String?,
    val fodselsdato: LocalDate?,
)
