package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.GyldigArbeidsgiverensVirksomhet

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigArbeidsgiverensVirksomhet
data class ArbeidsgiverensVirksomhetINorgeDto(
    val erArbeidsgiverenOffentligVirksomhet: Boolean,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean? = null,
    val opprettholderArbeidsgiverenVanligDrift: Boolean? = null
)