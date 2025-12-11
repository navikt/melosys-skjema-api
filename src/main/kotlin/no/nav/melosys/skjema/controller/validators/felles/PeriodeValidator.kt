package no.nav.melosys.skjema.controller.validators.felles

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PeriodeTranslation
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
                translationFieldName(PeriodeTranslation::fraDatoMaaVaereFoerTilDato.name),
            )
            return false
        }

        return true
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::periodeTranslation.name}.$fieldName"
        }
    }
}
