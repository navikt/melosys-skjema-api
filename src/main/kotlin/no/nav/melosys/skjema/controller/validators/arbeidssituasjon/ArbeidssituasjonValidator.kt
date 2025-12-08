package no.nav.melosys.skjema.controller.validators.arbeidssituasjon

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.addViolation
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
            if (dto.aktivitetIMaanedenFoerUtsendingen.isNullOrBlank()) {
                context.addViolation(
                    "Aktivitet i måneden før utsendingen må oppgis når arbeidstaker ikke har vært i lønnet arbeid",
                    "aktivitetIMaanedenFoerUtsendingen"
                )
                return false
            }
        }

        if (dto.skalJobbeForFlereVirksomheter) {
            val virksomheter = dto.virksomheterArbeidstakerJobberForIutsendelsesPeriode
            val hasVirksomheter = virksomheter?.let {
                !it.norskeVirksomheter.isNullOrEmpty() || !it.utenlandskeVirksomheter.isNullOrEmpty()
            } ?: false

            if (!hasVirksomheter) {
                context.addViolation(
                    "Minst én virksomhet må oppgis når arbeidstaker skal jobbe for flere virksomheter",
                    "virksomheterArbeidstakerJobberForIutsendelsesPeriode"
                )
                return false
            }
        }

        return true
    }
}
