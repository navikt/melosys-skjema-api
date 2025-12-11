package no.nav.melosys.skjema.controller.validators.utenlandsoppdraget

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.UtenlandsoppdragetTranslation
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
                context.addViolation(
                    translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiBegrunnelse.name),
                    UtenlandsoppdragetDto::utenlandsoppholdetsBegrunnelse.name
                )
                return false
            }
        }

        if (dto.arbeidstakerBleAnsattForUtenlandsoppdraget) {
            if (dto.arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget == null) {
                context.addViolation(
                    translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiOmArbeidstakerVilJobbeEtterOppdraget.name),
                    UtenlandsoppdragetDto::arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget.name
                )
                return false
            }
        }

        if (!dto.arbeidstakerForblirAnsattIHelePerioden) {
            if (dto.ansettelsesforholdBeskrivelse.isNullOrBlank()) {
                context.addViolation(
                    translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiBeskrivelseAvAnsettelsesforhold.name),
                    UtenlandsoppdragetDto::ansettelsesforholdBeskrivelse.name
                )
                return false
            }
        }

        if (dto.arbeidstakerErstatterAnnenPerson) {
            if (dto.forrigeArbeidstakerUtsendelsePeriode == null) {
                context.addViolation(
                    translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiForrigeArbeidstakerUtsendelsePeriode.name),
                    UtenlandsoppdragetDto::forrigeArbeidstakerUtsendelsePeriode.name
                )
                return false
            }
        }

        return true
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::utenlandsoppdragetTranslation.name}.$fieldName"
        }
    }
}
