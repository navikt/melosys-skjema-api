package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakerenDto(
    val harNorskFodselsnummer: Boolean,
    val fodselsnummer: String?,
    val fornavn: String?,
    val etternavn: String?,
    val fodselsdato: LocalDate?,
    val harVaertEllerSkalVaereILonnetArbeidFoerUtsending: Boolean,
    val aktivitetIMaanedenFoerUtsendingen: String?,
    val skalJobbeForFlereVirksomheter: Boolean,
    val virksomheterArbeidstakerJobberForIutsendelsesPeriode: NorskeOgUtenlandskeVirksomheter?
)