package no.nav.melosys.skjema.dto.arbeidstaker.dineopplysninger

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DineOpplysningerDto(
    val harNorskFodselsnummer: Boolean,
    val fodselsnummer: String?,
    val fornavn: String?,
    val etternavn: String?,
    val fodselsdato: LocalDate?,
)