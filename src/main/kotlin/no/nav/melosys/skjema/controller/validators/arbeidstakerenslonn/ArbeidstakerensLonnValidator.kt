package no.nav.melosys.skjema.controller.validators.arbeidstakerenslonn

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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

        return dto.arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden ==
                (dto.virksomheterSomUtbetalerLonnOgNaturalytelser == null)
    }
}
