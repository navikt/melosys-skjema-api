package no.nav.melosys.skjema.controller.validators.tilleggsopplysninger

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.dto.translations.ErrorMessageTranslation
import no.nav.melosys.skjema.controller.dto.translations.TilleggsopplysningerTranslation
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import org.springframework.stereotype.Component

@Component
class TilleggsopplysningerValidator : ConstraintValidator<GyldigTilleggsopplysninger, TilleggsopplysningerDto> {

    override fun initialize(constraintAnnotation: GyldigTilleggsopplysninger?) {}

    override fun isValid(
        dto: TilleggsopplysningerDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (dto.harFlereOpplysningerTilSoknaden) {
            if (dto.tilleggsopplysningerTilSoknad.isNullOrBlank()) {
                context.addViolation(
                    translationFieldName(TilleggsopplysningerTranslation::maaOppgiTilleggsopplysninger.name),
                    TilleggsopplysningerDto::tilleggsopplysningerTilSoknad.name
                )
                return false
            }
        } else {
            if (dto.tilleggsopplysningerTilSoknad != null) {
                context.addViolation(
                    translationFieldName(TilleggsopplysningerTranslation::tilleggsopplysningerSkalIkkeOppgis.name),
                    TilleggsopplysningerDto::tilleggsopplysningerTilSoknad.name
                )
                return false
            }
        }

        return true
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::tilleggsopplysningerTranslation.name}.$fieldName"
        }
    }
}
