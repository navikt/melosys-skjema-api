package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
                    context.addViolation("Du må oppgi flaggland for skip i internasjonalt farvann", "flaggland")
                    return false
                }
                if (!dto.territorialfarvannLand.isNullOrBlank()) {
                    context.addViolation("Territorialfarvann land skal ikke oppgis for internasjonalt farvann", "territorialfarvannLand")
                    return false
                }
                true
            }
            Farvann.TERRITORIALFARVANN -> {
                if (dto.territorialfarvannLand.isNullOrBlank()) {
                    context.addViolation("Du må oppgi territorialfarvann land", "territorialfarvannLand")
                    return false
                }
                if (!dto.flaggland.isNullOrBlank()) {
                    context.addViolation("Flaggland skal ikke oppgis for territorialfarvann", "flaggland")
                    return false
                }
                true
            }
        }
    }
}
