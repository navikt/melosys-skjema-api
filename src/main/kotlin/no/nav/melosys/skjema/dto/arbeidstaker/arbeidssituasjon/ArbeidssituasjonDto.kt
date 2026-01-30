package no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.dto.felles.NorskeOgUtenlandskeVirksomheterMedAnsettelsesform

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidssituasjonDto(
    val harVaertEllerSkalVaereILonnetArbeidFoerUtsending: Boolean,
    val aktivitetIMaanedenFoerUtsendingen: String?,
    val skalJobbeForFlereVirksomheter: Boolean,
    val virksomheterArbeidstakerJobberForIutsendelsesPeriode: NorskeOgUtenlandskeVirksomheterMedAnsettelsesform?
)
