package no.nav.melosys.skjema.dto

data class ArbeidsgiverensVirksomhetINorgeDto(
    val erArbeidsgiverenOffentligVirksomhet: Boolean,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean,
    val opprettholderArbeidsgivereVanligDrift: Boolean
)