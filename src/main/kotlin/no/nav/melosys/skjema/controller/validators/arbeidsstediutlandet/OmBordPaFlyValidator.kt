package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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

        return if (dto.erVanligHjemmebase) {
            dto.vanligHjemmebaseLand == null && dto.vanligHjemmebaseNavn == null
        } else dto.vanligHjemmebaseLand != null && dto.vanligHjemmebaseNavn != null

    }
}
