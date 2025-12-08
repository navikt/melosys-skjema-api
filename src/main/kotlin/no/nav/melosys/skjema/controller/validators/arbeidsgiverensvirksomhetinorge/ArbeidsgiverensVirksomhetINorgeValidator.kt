package no.nav.melosys.skjema.controller.validators.arbeidsgiverensvirksomhetinorge

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import org.springframework.stereotype.Component

@Component
class ArbeidsgiverensVirksomhetINorgeValidator :
    ConstraintValidator<GyldigArbeidsgiverensVirksomhet, ArbeidsgiverensVirksomhetINorgeDto> {

    override fun initialize(constraintAnnotation: GyldigArbeidsgiverensVirksomhet?) {}

    override fun isValid(
        dto: ArbeidsgiverensVirksomhetINorgeDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (dto.erArbeidsgiverenOffentligVirksomhet) {
            if (dto.erArbeidsgiverenBemanningsEllerVikarbyraa != null) {
                context.addViolation(
                    "Offentlige virksomheter skal ikke oppgi bemanningsbyrå",
                    "erArbeidsgiverenBemanningsEllerVikarbyraa"
                )
                return false
            }
            if (dto.opprettholderArbeidsgiverenVanligDrift != null) {
                context.addViolation(
                    "Offentlige virksomheter skal ikke oppgi vanlig drift",
                    "opprettholderArbeidsgiverenVanligDrift"
                )
                return false
            }
        } else {
            if (dto.erArbeidsgiverenBemanningsEllerVikarbyraa == null) {
                context.addViolation(
                    "Du må oppgi om arbeidsgiver er bemannings- eller vikarbyrå",
                    "erArbeidsgiverenBemanningsEllerVikarbyraa"
                )
                return false
            }
            if (dto.opprettholderArbeidsgiverenVanligDrift == null) {
                context.addViolation(
                    "Du må oppgi om arbeidsgiver opprettholder vanlig drift",
                    "opprettholderArbeidsgiverenVanligDrift"
                )
                return false
            }
        }

        return true
    }
}