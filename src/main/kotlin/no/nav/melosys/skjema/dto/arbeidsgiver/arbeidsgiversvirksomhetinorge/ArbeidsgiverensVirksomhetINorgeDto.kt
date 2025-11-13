package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiverensVirksomhetINorgeDto(
    val erArbeidsgiverenOffentligVirksomhet: Boolean,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean?,
    val opprettholderArbeidsgiverenVanligDrift: Boolean?
)