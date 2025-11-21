package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
            Farvann.INTERNASJONALT_FARVANN -> !dto.flaggland.isNullOrBlank() && dto.territorialfarvannLand.isNullOrBlank()
            Farvann.TERRITORIALFARVANN -> !dto.territorialfarvannLand.isNullOrBlank() && dto.flaggland.isNullOrBlank()
        }
    }
}
