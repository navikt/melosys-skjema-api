package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
                    context.addViolation("Du må oppgi fast arbeidssted", "fastArbeidssted")
                    return false
                }
                if (dto.beskrivelseVekslende != null) {
                    context.addViolation("Beskrivelse av vekslende arbeidssted skal ikke oppgis for fast arbeidssted", "beskrivelseVekslende")
                    return false
                }
                true
            }

            FastEllerVekslendeArbeidssted.VEKSLENDE -> {
                if (dto.fastArbeidssted != null) {
                    context.addViolation("Fast arbeidssted skal ikke oppgis for vekslende arbeidssted", "fastArbeidssted")
                    return false
                }
                if (dto.beskrivelseVekslende.isNullOrBlank()) {
                    context.addViolation("Du må oppgi beskrivelse av vekslende arbeidssted", "beskrivelseVekslende")
                    return false
                }
                true
            }
        }
    }
}
