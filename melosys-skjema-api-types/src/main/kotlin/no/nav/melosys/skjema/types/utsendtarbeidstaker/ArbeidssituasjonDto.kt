package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheterMedAnsettelsesform

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidssituasjonDto(
    /** Stilles ikke til offentlig tjenesteperson. */
    val harVaertEllerSkalVaereILonnetArbeidFoerUtsending: Boolean?,
    val aktivitetIMaanedenFoerUtsendingen: String?,
    val skalJobbeForFlereVirksomheter: Boolean,
    val virksomheterArbeidstakerJobberForIutsendelsesPeriode: NorskeOgUtenlandskeVirksomheterMedAnsettelsesform?
)