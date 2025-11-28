package no.nav.melosys.skjema.controller.validators.utenlandsoppdraget

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
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

        if (dto.arbeidstakerUtsendelseFraDato.isAfter(dto.arbeidstakerUtsendelseTilDato)) {
            return false
        }

        if (!dto.arbeidsgiverHarOppdragILandet) {
            return !dto.utenlandsoppholdetsBegrunnelse.isNullOrBlank()
        }

        if (dto.arbeidstakerBleAnsattForUtenlandsoppdraget) {
            return dto.arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget != null
        }

        if (!dto.arbeidstakerForblirAnsattIHelePerioden) {
            return !dto.ansettelsesforholdBeskrivelse.isNullOrBlank()
        }

        if (dto.arbeidstakerErstatterAnnenPerson) {
            if (dto.forrigeArbeidstakerUtsendelseFradato == null || dto.forrigeArbeidstakerUtsendelseTilDato == null) {
                return false
            }
            if (dto.forrigeArbeidstakerUtsendelseFradato.isAfter(dto.forrigeArbeidstakerUtsendelseTilDato)) {
                return false
            }
        }

        return true
    }
}
