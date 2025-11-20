package no.nav.melosys.skjema.controller.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import org.springframework.stereotype.Component

@Component
class ArbeidsgiverensVirksomhetINorgeValidator : ConstraintValidator<GyldigArbeidsgiverensVirksomhet, ArbeidsgiverensVirksomhetINorgeDto> {

    override fun initialize(constraintAnnotation: GyldigArbeidsgiverensVirksomhet?) {}

    override fun isValid(
        dto: ArbeidsgiverensVirksomhetINorgeDto?,
        context: ConstraintValidatorContext
    ): Boolean = when {
        dto == null -> true
        dto.erArbeidsgiverenOffentligVirksomhet ->
            dto.erArbeidsgiverenBemanningsEllerVikarbyraa == null && dto.opprettholderArbeidsgiverenVanligDrift == null
        else ->
            dto.erArbeidsgiverenBemanningsEllerVikarbyraa != null && dto.opprettholderArbeidsgiverenVanligDrift != null
    }
}
