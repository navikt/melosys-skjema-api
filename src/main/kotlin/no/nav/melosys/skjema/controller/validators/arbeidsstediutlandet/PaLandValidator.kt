package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
                dto.fastArbeidssted != null &&
                        dto.beskrivelseVekslende == null
            }

            FastEllerVekslendeArbeidssted.VEKSLENDE -> {
                dto.fastArbeidssted == null &&
                        !dto.beskrivelseVekslende.isNullOrBlank()
            }
        }
    }
}
