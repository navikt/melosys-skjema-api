package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PaSkipTranslation
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.Farvann
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaSkipDto
import org.springframework.stereotype.Component

@Component
class PaSkipValidator : ConstraintValidator<GyldigPaSkip, PaSkipDto> {

    override fun initialize(constraintAnnotation: GyldigPaSkip?) {}

    override fun isValid(
        dto: PaSkipDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        return when(dto.seilerI) {
            Farvann.INTERNASJONALT_FARVANN -> {
                if (dto.flaggland.isNullOrBlank()) {
                    context.addViolation(
                        translationFieldName(PaSkipTranslation::duMaOppgiFlaggland.name),
                        PaSkipDto::flaggland.name
                    )
                    return false
                }
                if (!dto.territorialfarvannLand.isNullOrBlank()) {
                    context.addViolation(
                        translationFieldName(PaSkipTranslation::territorialfarvannLandSkalIkkeOppgis.name),
                        PaSkipDto::territorialfarvannLand.name
                    )
                    return false
                }
                true
            }
            Farvann.TERRITORIALFARVANN -> {
                if (dto.territorialfarvannLand.isNullOrBlank()) {
                    context.addViolation(
                        translationFieldName(PaSkipTranslation::duMaOppgiTerritorialfarvannLand.name),
                        PaSkipDto::territorialfarvannLand.name
                    )
                    return false
                }
                if (!dto.flaggland.isNullOrBlank()) {
                    context.addViolation(
                        translationFieldName(PaSkipTranslation::flagglandSkalIkkeOppgis.name),
                        PaSkipDto::flaggland.name
                    )
                    return false
                }
                true
            }
        }
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::paSkipTranslation.name}.$fieldName"
        }
    }
}
