package no.nav.melosys.skjema.controller.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class FodselsnummerValidator : ConstraintValidator<ErFodselsnummer?, String?> {
    override fun initialize(constraintAnnotation: ErFodselsnummer?) {}

    override fun isValid(
        fodselsnummer: String?,
        cxt: ConstraintValidatorContext
    ): Boolean {
        // Null values are handled by @NotNull annotation
        if (fodselsnummer == null) return true

        return fodselsnummer.matches(Regex("[0-9]+")) && fodselsnummer.length == 11
    }
}
