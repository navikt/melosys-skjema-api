package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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
                context.addViolation("Vanlig hjemmebase land skal ikke oppgis når det er Norge", "vanligHjemmebaseLand")
                return false
            }
            if (dto.vanligHjemmebaseNavn != null) {
                context.addViolation("Vanlig hjemmebase navn skal ikke oppgis når det er Norge", "vanligHjemmebaseNavn")
                return false
            }
        } else {
            if (dto.vanligHjemmebaseLand == null) {
                context.addViolation("Du må oppgi vanlig hjemmebase land", "vanligHjemmebaseLand")
                return false
            }
            if (dto.vanligHjemmebaseNavn == null) {
                context.addViolation("Du må oppgi vanlig hjemmebase navn", "vanligHjemmebaseNavn")
                return false
            }
        }

        return true
    }
}
