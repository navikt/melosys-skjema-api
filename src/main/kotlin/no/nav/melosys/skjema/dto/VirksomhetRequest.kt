package no.nav.melosys.skjema.dto

data class VirksomhetRequest(
    val erArbeidsgiverenOffentligVirksomhet: Boolean,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean,
    val opprettholderArbeidsgivereVanligDrift: Boolean
)