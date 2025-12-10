package no.nav.melosys.skjema.controller.validators.arbeidstakerenslonn

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.dto.translations.ArbeidstakerensLonnTranslation
import no.nav.melosys.skjema.controller.dto.translations.ErrorMessageTranslation
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import org.springframework.stereotype.Component

@Component
class ArbeidstakerensLonnValidator : ConstraintValidator<GyldigArbeidstakerensLonn, ArbeidstakerensLonnDto> {

    override fun initialize(constraintAnnotation: GyldigArbeidstakerensLonn?) {}

    override fun isValid(
        dto: ArbeidstakerensLonnDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (dto.arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden) {
            if (dto.virksomheterSomUtbetalerLonnOgNaturalytelser != null) {
                context.addViolation(
                    translationFieldName(ArbeidstakerensLonnTranslation::virksomheterSkalIkkeOppgis.name),
                    ArbeidstakerensLonnDto::virksomheterSomUtbetalerLonnOgNaturalytelser.name
                )
                return false
            }
        } else {
            if (dto.virksomheterSomUtbetalerLonnOgNaturalytelser == null) {
                context.addViolation(
                    translationFieldName(ArbeidstakerensLonnTranslation::maaOppgiVirksomheter.name),
                    ArbeidstakerensLonnDto::virksomheterSomUtbetalerLonnOgNaturalytelser.name
                )
                return false
            }
        }

        return true
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::arbeidstakerensLonnTranslation.name}.$fieldName"
        }
    }
}
