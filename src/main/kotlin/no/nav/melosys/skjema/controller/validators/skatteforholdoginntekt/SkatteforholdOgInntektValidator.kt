package no.nav.melosys.skjema.controller.validators.skatteforholdoginntekt

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.dto.translations.ErrorMessageTranslation
import no.nav.melosys.skjema.controller.dto.translations.SkatteforholdOgInntektTranslation
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import org.springframework.stereotype.Component

@Component
class SkatteforholdOgInntektValidator : ConstraintValidator<GyldigSkatteforholdOgInntekt, SkatteforholdOgInntektDto> {

    override fun initialize(constraintAnnotation: GyldigSkatteforholdOgInntekt?) {}

    override fun isValid(
        dto: SkatteforholdOgInntektDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (dto.mottarPengestotteFraAnnetEosLandEllerSveits) {
            if (dto.landSomUtbetalerPengestotte.isNullOrBlank()) {
                context.addViolation(
                    translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiLandSomUtbetalerPengestotte.name),
                    SkatteforholdOgInntektDto::landSomUtbetalerPengestotte.name
                )
                return false
            }
            if (dto.pengestotteSomMottasFraAndreLandBelop.isNullOrBlank()) {
                context.addViolation(
                    translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiBelopPengestotte.name),
                    SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBelop.name
                )
                return false
            }
            if (dto.pengestotteSomMottasFraAndreLandBeskrivelse.isNullOrBlank()) {
                context.addViolation(
                    translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiBeskrivelsePengestotte.name),
                    SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBeskrivelse.name
                )
                return false
            }
        }

        return true
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::skatteforholdOgInntektTranslation.name}.$fieldName"
        }
    }
}
