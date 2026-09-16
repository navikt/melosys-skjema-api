package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiverensVirksomhetINorgeDto(
    val erArbeidsgiverenOffentligVirksomhet: Boolean? = null,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean? = null,
    val opprettholderArbeidsgiverenVanligDrift: Boolean? = null
)