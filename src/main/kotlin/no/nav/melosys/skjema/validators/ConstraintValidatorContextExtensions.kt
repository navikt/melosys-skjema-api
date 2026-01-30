package no.nav.melosys.skjema.validators

import jakarta.validation.ConstraintValidatorContext

/**
 * Extension function to simplify adding custom constraint violations.
 *
 * @param message The custom error message
 * @param propertyName Optional property name to associate the violation with a specific field
 */
fun ConstraintValidatorContext.addViolation(message: String, propertyName: String) {
    disableDefaultConstraintViolation()

    buildConstraintViolationWithTemplate(message)
        .addPropertyNode(propertyName)
        .addConstraintViolation()
}


fun ConstraintValidatorContext.addViolation(message: String) {
    disableDefaultConstraintViolation()

    buildConstraintViolationWithTemplate(message)
        .addConstraintViolation()
}