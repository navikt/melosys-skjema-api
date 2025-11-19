package no.nav.melosys.skjema.dto.arbeidstaker.dineopplysninger

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.ErFodselsEllerDNummer
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DineOpplysningerDto(
    val harNorskFodselsnummer: Boolean,
    @field:ErFodselsEllerDNummer
    val fodselsnummer: String?,
    val fornavn: String?,
    val etternavn: String?,
    val fodselsdato: LocalDate?,
)