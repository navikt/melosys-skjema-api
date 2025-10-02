package no.nav.melosys.skjema.dto

import java.time.LocalDate

data class ArbeidstakerRequest(
    val harNorskFodselsnummer: Boolean,
    val fodselsnummer: String?,
    val fornavn: String?,
    val etternavn: String?,
    val fodselsdato: LocalDate?,
    val harVaertEllerSkalVaereILonnetArbeidFoerUtsending: Boolean,
    val aktivitetIMaanedenFoerUtsendingen: String,
    val skalJobbeForFlereVirksomheter: Boolean,
    val norskeVirksomheterArbeidstakerJobberForIutsendelsesPeriode: List<NorskVirksomhet>?,
    val utenlandskeVirksomheterArbeidstakerJobberForIutsendelsesPeriode: List<UtenlandskVirksomhet>?
)