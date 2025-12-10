package no.nav.melosys.skjema.controller.validators.utenlandsoppdraget

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import org.springframework.stereotype.Component

@Component
class UtenlandsoppdragetValidator : ConstraintValidator<GyldigUtenlandsoppdrag, UtenlandsoppdragetDto> {

    override fun initialize(constraintAnnotation: GyldigUtenlandsoppdrag?) {}

    override fun isValid(
        dto: UtenlandsoppdragetDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        if (!dto.arbeidsgiverHarOppdragILandet) {
            if (dto.utenlandsoppholdetsBegrunnelse.isNullOrBlank()) {
                context.addViolation("Du må oppgi begrunnelse for utenlandsoppholdet", UtenlandsoppdragetDto::utenlandsoppholdetsBegrunnelse.name)
                return false
            }
        }

        if (dto.arbeidstakerBleAnsattForUtenlandsoppdraget) {
            if (dto.arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget == null) {
                context.addViolation("Du må oppgi om arbeidstaker vil jobbe for virksomhet i Norge etter oppdraget", UtenlandsoppdragetDto::arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget.name)
                return false
            }
        }

        if (!dto.arbeidstakerForblirAnsattIHelePerioden) {
            if (dto.ansettelsesforholdBeskrivelse.isNullOrBlank()) {
                context.addViolation("Du må oppgi beskrivelse av ansettelsesforholdet", UtenlandsoppdragetDto::ansettelsesforholdBeskrivelse.name)
                return false
            }
        }

        if (dto.arbeidstakerErstatterAnnenPerson) {
            if (dto.forrigeArbeidstakerUtsendelsePeriode == null) {
                context.addViolation("Du må oppgi forrige arbeidstakers utsendelseperiode", UtenlandsoppdragetDto::forrigeArbeidstakerUtsendelsePeriode.name)
                return false
            }
        }

        return true
    }
}
