package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PaLandTranslation
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaLandDto
import org.springframework.stereotype.Component

@Component
class PaLandValidator : ConstraintValidator<GyldigPaLand, PaLandDto> {

    override fun initialize(constraintAnnotation: GyldigPaLand?) {}

    override fun isValid(
        dto: PaLandDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        return when (dto.fastEllerVekslendeArbeidssted) {
            FastEllerVekslendeArbeidssted.FAST -> {
                if (dto.fastArbeidssted == null) {
                    context.addViolation(
                        translationFieldName(PaLandTranslation::maaOppgiFastArbeidssted.name),
                        PaLandDto::fastArbeidssted.name
                    )
                    return false
                }
                if (dto.beskrivelseVekslende != null) {
                    context.addViolation(
                        translationFieldName(PaLandTranslation::beskrivelseVekslendeSkalIkkeOppgis.name),
                        PaLandDto::beskrivelseVekslende.name
                    )
                    return false
                }
                true
            }

            FastEllerVekslendeArbeidssted.VEKSLENDE -> {
                if (dto.fastArbeidssted != null) {
                    context.addViolation(
                        translationFieldName(PaLandTranslation::fastArbeidsstedSkalIkkeOppgis.name),
                        PaLandDto::fastArbeidssted.name
                    )
                    return false
                }
                if (dto.beskrivelseVekslende.isNullOrBlank()) {
                    context.addViolation(
                        translationFieldName(PaLandTranslation::maaOppgiBeskrivelseVekslende.name),
                        PaLandDto::beskrivelseVekslende.name
                    )
                    return false
                }
                true
            }
        }
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::paLandTranslation.name}.$fieldName"
        }
    }
}
