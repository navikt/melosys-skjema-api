package no.nav.melosys.skjema.controller.validators.utenlandsoppdraget

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import org.springframework.stereotype.Component

@Component
class UtenlandsoppdragetArbeidstakerValidator : ConstraintValidator<GyldigUtenlandsoppdragetArbeidstaker, UtenlandsoppdragetArbeidstakersDelDto> {

    override fun initialize(constraintAnnotation: GyldigUtenlandsoppdragetArbeidstaker?) {}

    override fun isValid(
        dto: UtenlandsoppdragetArbeidstakersDelDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (dto.utsendelsesLand.isBlank()) {
            context.addViolation("Du må oppgi utsendelsesland", UtenlandsoppdragetArbeidstakersDelDto::utsendelsesLand.name)
            return false
        }

        return true
    }
}
