package no.nav.melosys.skjema.controller.validators.arbeidstakerenslonn

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
                context.addViolation("Virksomheter som utbetaler lønn skal ikke oppgis når arbeidsgiver betaler alt", ArbeidstakerensLonnDto::virksomheterSomUtbetalerLonnOgNaturalytelser.name)
                return false
            }
        } else {
            if (dto.virksomheterSomUtbetalerLonnOgNaturalytelser == null) {
                context.addViolation("Du må oppgi virksomheter som utbetaler lønn og naturalytelser", ArbeidstakerensLonnDto::virksomheterSomUtbetalerLonnOgNaturalytelser.name)
                return false
            }
        }

        return true
    }
}
