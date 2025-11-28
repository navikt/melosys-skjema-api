package no.nav.melosys.skjema.controller.validators.skatteforholdoginntekt

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
            return !dto.landSomUtbetalerPengestotte.isNullOrBlank()
                    && !dto.pengestotteSomMottasFraAndreLandBelop.isNullOrBlank()
                    && !dto.pengestotteSomMottasFraAndreLandBeskrivelse.isNullOrBlank()
        }

        return true
    }
}
