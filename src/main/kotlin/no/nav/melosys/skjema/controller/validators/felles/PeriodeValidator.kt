package no.nav.melosys.skjema.controller.validators.felles

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.felles.PeriodeDto
import org.springframework.stereotype.Component

@Component
class PeriodeValidator : ConstraintValidator<GyldigPeriode, PeriodeDto> {

    override fun initialize(constraintAnnotation: GyldigPeriode?) {}

    override fun isValid(
        dto: PeriodeDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (dto.fraDato.isAfter(dto.tilDato)) {
            context.addViolation(
                "Fra-dato må være før eller lik til-dato",
                "fraDato"
            )
            return false
        }

        return true
    }
}
