package no.nav.melosys.skjema.controller.validators.arbeidssituasjon

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import org.springframework.stereotype.Component

@Component
class ArbeidssituasjonValidator : ConstraintValidator<GyldigArbeidssituasjon, ArbeidssituasjonDto> {

    override fun initialize(constraintAnnotation: GyldigArbeidssituasjon?) {}

    override fun isValid(
        dto: ArbeidssituasjonDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (!dto.harVaertEllerSkalVaereILonnetArbeidFoerUtsending) {
            return !dto.aktivitetIMaanedenFoerUtsendingen.isNullOrBlank()
        }

        if (dto.skalJobbeForFlereVirksomheter) {
            return dto.virksomheterArbeidstakerJobberForIutsendelsesPeriode?.let {
                !it.norskeVirksomheter.isNullOrEmpty() || !it.utenlandskeVirksomheter.isNullOrEmpty()
            } ?: false
        }

        return true
    }
}
