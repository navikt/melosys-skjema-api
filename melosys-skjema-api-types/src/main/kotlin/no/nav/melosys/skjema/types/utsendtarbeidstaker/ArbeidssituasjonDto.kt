package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheterMedAnsettelsesform

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidssituasjonDto(
    /** Skal ikke besvares i statsansattflyten, heller ikke i kombinert skjema. */
    val harVaertEllerSkalVaereILonnetArbeidFoerUtsending: Boolean?,
    val aktivitetIMaanedenFoerUtsendingen: String?,
    val skalJobbeForFlereVirksomheter: Boolean,
    val virksomheterArbeidstakerJobberForIutsendelsesPeriode: NorskeOgUtenlandskeVirksomheterMedAnsettelsesform?
)