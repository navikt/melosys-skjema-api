package no.nav.melosys.skjema.validators.utenlandsoppdraget

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.UtenlandsoppdragetTranslation
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.validators.felles.PeriodeValidator
import org.springframework.stereotype.Component

@Component
class UtenlandsoppdragetValidator() {

    fun validate(dto: UtenlandsoppdragetDto?): List<Violation> {
        if (dto == null) return emptyList()

        PeriodeValidator.validate(
            dto.arbeidstakerUtsendelsePeriode,
            fieldName = UtenlandsoppdragetDto::arbeidstakerUtsendelsePeriode.name
        )
            .takeIf { it.isNotEmpty() }?.let { return it }

        if (!dto.arbeidsgiverHarOppdragILandet) {
            if (dto.utenlandsoppholdetsBegrunnelse.isNullOrBlank()) {
                return listOf(Violation(
                    field = UtenlandsoppdragetDto::utenlandsoppholdetsBegrunnelse.name,
                    translationKey = translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiBegrunnelse.name)
                ))
            }
        }

        if (dto.arbeidstakerBleAnsattForUtenlandsoppdraget) {
            if (dto.arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget == null) {
                return listOf(Violation(
                    field = UtenlandsoppdragetDto::arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget.name,
                    translationKey = translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiOmArbeidstakerVilJobbeEtterOppdraget.name)
                ))
            }
        }

        if (!dto.arbeidstakerForblirAnsattIHelePerioden) {
            if (dto.ansettelsesforholdBeskrivelse.isNullOrBlank()) {
                return listOf(Violation(
                    field = UtenlandsoppdragetDto::ansettelsesforholdBeskrivelse.name,
                    translationKey = translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiBeskrivelseAvAnsettelsesforhold.name)
                ))
            }
        }

        if (dto.arbeidstakerErstatterAnnenPerson) {
            if (dto.forrigeArbeidstakerUtsendelsePeriode == null) {
                return listOf(Violation(
                    field = UtenlandsoppdragetDto::forrigeArbeidstakerUtsendelsePeriode.name,
                    translationKey = translationFieldName(UtenlandsoppdragetTranslation::duMaOppgiForrigeArbeidstakerUtsendelsePeriode.name)
                ))
            }
        }

        if (dto.forrigeArbeidstakerUtsendelsePeriode != null){
            PeriodeValidator.validate(
                dto.forrigeArbeidstakerUtsendelsePeriode,
                fieldName = UtenlandsoppdragetDto::forrigeArbeidstakerUtsendelsePeriode.name,
            )
                .takeIf { it.isNotEmpty() }?.let { return it }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::utenlandsoppdragetTranslation.name}.$fieldName"
        }
    }
}
