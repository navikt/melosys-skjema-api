package no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge

import no.nav.melosys.skjema.translations.dto.ArbeidsgiverensVirksomhetINorgeTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import org.springframework.stereotype.Component

@Component
class ArbeidsgiverensVirksomhetINorgeValidator {

    fun validate(dto: ArbeidsgiverensVirksomhetINorgeDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (dto.erArbeidsgiverenOffentligVirksomhet) {
            if (dto.erArbeidsgiverenBemanningsEllerVikarbyraa != null) {
                return listOf(Violation(
                    field = ArbeidsgiverensVirksomhetINorgeDto::erArbeidsgiverenBemanningsEllerVikarbyraa.name,
                    translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::offentligVirksomhetSkalIkkeOppgiBemanningsbyraa.name)
                ))
            }
            if (dto.opprettholderArbeidsgiverenVanligDrift != null) {
                return listOf(Violation(
                    field = ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name,
                    translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::offentligVirksomhetSkalIkkeOppgiVanligDrift.name)
                ))
            }
        } else {
            if (dto.erArbeidsgiverenBemanningsEllerVikarbyraa == null) {
                return listOf(Violation(
                    field = ArbeidsgiverensVirksomhetINorgeDto::erArbeidsgiverenBemanningsEllerVikarbyraa.name,
                    translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::maaOppgiOmBemanningsbyraa.name)
                ))
            }
            if (dto.opprettholderArbeidsgiverenVanligDrift == null) {
                return listOf(Violation(
                    field = ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name,
                    translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::maaOppgiOmVanligDrift.name)
                ))
            }
        }
        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::arbeidsgiverensVirksomhetINorgeTranslation.name}.$fieldName"
        }
    }
}