package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.dto.translations.ErrorMessageTranslation
import no.nav.melosys.skjema.controller.dto.translations.OmBordPaFlyTranslation
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.OmBordPaFlyDto
import org.springframework.stereotype.Component

@Component
class OmBordPaFlyValidator : ConstraintValidator<GyldigOmBordPaFly, OmBordPaFlyDto> {

    override fun initialize(constraintAnnotation: GyldigOmBordPaFly?) {}

    override fun isValid(
        dto: OmBordPaFlyDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (dto.erVanligHjemmebase) {
            if (dto.vanligHjemmebaseLand != null) {
                context.addViolation(
                    translationFieldName(OmBordPaFlyTranslation::vanligHjemmebaseLandSkalIkkeOppgis.name),
                    OmBordPaFlyDto::vanligHjemmebaseLand.name
                )
                return false
            }
            if (dto.vanligHjemmebaseNavn != null) {
                context.addViolation(
                    translationFieldName(OmBordPaFlyTranslation::vanligHjemmebaseNavnSkalIkkeOppgis.name),
                    OmBordPaFlyDto::vanligHjemmebaseNavn.name
                )
                return false
            }
        } else {
            if (dto.vanligHjemmebaseLand == null) {
                context.addViolation(
                    translationFieldName(OmBordPaFlyTranslation::maaOppgiVanligHjemmebaseLand.name),
                    OmBordPaFlyDto::vanligHjemmebaseLand.name
                )
                return false
            }
            if (dto.vanligHjemmebaseNavn == null) {
                context.addViolation(
                    translationFieldName(OmBordPaFlyTranslation::maaOppgiVanligHjemmebaseNavn.name),
                    OmBordPaFlyDto::vanligHjemmebaseNavn.name
                )
                return false
            }
        }

        return true
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::omBordPaFlyTranslation.name}.$fieldName"
        }
    }
}
