package no.nav.melosys.skjema.validators

data class ValidationResult(
    val violations: List<Violation> = emptyList()
) {
    val isValid: Boolean get() = violations.isEmpty()

    fun merge(other: ValidationResult): ValidationResult {
        return ValidationResult(violations + other.violations)
    }

    companion object {
        fun valid() = ValidationResult()

        fun invalid(violation: Violation) = ValidationResult(listOf(violation))

        fun invalid(violations: List<Violation>) = ValidationResult(violations)

        fun fromResults(results: List<ValidationResult>): ValidationResult {
            return ValidationResult(results.flatMap { it.violations })
        }
    }
}

data class Violation(
    val field: String,
    val translationKey: String,
    val message: String = ""
)

class ValidationException(
    val violations: List<Violation>
) : RuntimeException("Validation failed")
