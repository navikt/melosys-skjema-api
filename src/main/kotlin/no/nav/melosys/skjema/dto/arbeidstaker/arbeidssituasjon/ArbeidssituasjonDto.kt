package no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.controller.validators.arbeidssituasjon.GyldigArbeidssituasjon
import no.nav.melosys.skjema.dto.felles.NorskeOgUtenlandskeVirksomheter

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigArbeidssituasjon
data class ArbeidssituasjonDto(
    val harVaertEllerSkalVaereILonnetArbeidFoerUtsending: Boolean,
    val aktivitetIMaanedenFoerUtsendingen: String?,
    val skalJobbeForFlereVirksomheter: Boolean,
    @field:Valid
    val virksomheterArbeidstakerJobberForIutsendelsesPeriode: NorskeOgUtenlandskeVirksomheter?
)
