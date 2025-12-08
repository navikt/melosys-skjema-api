package no.nav.melosys.skjema.controller.validators.skatteforholdoginntekt

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
                context.addViolation("Du må oppgi land som utbetaler pengestøtte", "landSomUtbetalerPengestotte")
                return false
            }
            if (dto.pengestotteSomMottasFraAndreLandBelop.isNullOrBlank()) {
                context.addViolation("Du må oppgi beløp for pengestøtte fra andre land", "pengestotteSomMottasFraAndreLandBelop")
                return false
            }
            if (dto.pengestotteSomMottasFraAndreLandBeskrivelse.isNullOrBlank()) {
                context.addViolation("Du må oppgi beskrivelse av pengestøtte fra andre land", "pengestotteSomMottasFraAndreLandBeskrivelse")
                return false
            }
        }

        return true
    }
}
