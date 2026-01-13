package no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import no.nav.melosys.skjema.controller.validators.familiemedlemmer.GyldigFamiliemedlem
import no.nav.melosys.skjema.controller.validators.felles.ErFodselsEllerDNummer

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigFamiliemedlem
data class Familiemedlem(
    val fornavn: String,
    val etternavn: String,
    val harNorskFodselsnummerEllerDnummer: Boolean,
    @field:ErFodselsEllerDNummer
    val fodselsnummer: String?,
    val fodselsdato: LocalDate?,
)
