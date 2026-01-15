package no.nav.melosys.skjema.controller.validators.familiemedlemmer

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.FamiliemedlemmerTranslation
import org.springframework.stereotype.Component

@Component
class FamiliemedlemmerValidator : ConstraintValidator<GyldigFamiliemedlemmer, FamiliemedlemmerDto> {

    override fun initialize(constraintAnnotation: GyldigFamiliemedlemmer?) {}

    override fun isValid(
        dto: FamiliemedlemmerDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (!dto.skalHaMedFamiliemedlemmer && dto.familiemedlemmer.isNotEmpty()) {
            context.addViolation(
                translationFieldName(FamiliemedlemmerTranslation::familiemedlemmerMaaVaereTomNarSkalHaMedFamiliemedlemmerErFalse.name),
                FamiliemedlemmerDto::familiemedlemmer.name
            )
            return false
        }

        return true
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::familiemedlemmerTranslation.name}.$fieldName"
        }
    }
}
