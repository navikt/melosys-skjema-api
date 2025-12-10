package no.nav.melosys.skjema.controller.validators.arbeidsgiverensvirksomhetinorge

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.dto.translations.ArbeidsgiverensVirksomhetINorgeTranslation
import no.nav.melosys.skjema.controller.dto.translations.ErrorMessageTranslation
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
                    translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::offentligVirksomhetSkalIkkeOppgiBemanningsbyraa.name),
                    ArbeidsgiverensVirksomhetINorgeDto::erArbeidsgiverenBemanningsEllerVikarbyraa.name
                )
                return false
            }
            if (dto.opprettholderArbeidsgiverenVanligDrift != null) {
                context.addViolation(
                    translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::offentligVirksomhetSkalIkkeOppgiVanligDrift.name),
                    ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name
                )
                return false
            }
        } else {
            if (dto.erArbeidsgiverenBemanningsEllerVikarbyraa == null) {
                context.addViolation(
                    translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::maaOppgiOmBemanningsbyraa.name),
                    ArbeidsgiverensVirksomhetINorgeDto::erArbeidsgiverenBemanningsEllerVikarbyraa.name
                )
                return false
            }
            if (dto.opprettholderArbeidsgiverenVanligDrift == null) {
                context.addViolation(
                    translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::maaOppgiOmVanligDrift.name),
                    ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name
                )
                return false
            }
        }

        return true
    }
    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::arbeidsgiverensVirksomhetINorgeTranslation.name}.$fieldName"
        }
    }
}