package no.nav.melosys.skjema.controller

data class VirksomhetRequest(
    val erArbeidsgiverenOffentligVirksomhet: Boolean,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean,
    val opprettholderArbeidsgivereVanligDrift: Boolean
)