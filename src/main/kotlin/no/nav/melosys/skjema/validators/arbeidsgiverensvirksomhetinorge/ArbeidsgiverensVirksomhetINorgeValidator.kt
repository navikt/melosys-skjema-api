package no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge

import no.nav.melosys.skjema.translations.dto.ArbeidsgiverensVirksomhetINorgeTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class ArbeidsgiverensVirksomhetINorgeValidator {

    /**
     * Klassifiseringen kommer fra Enhetsregisteret, ikke fra søkeren, og avgjør hvilke
     * oppfølgingsspørsmål seksjonen skal ha. Mangler den, er det en systemfeil og ikke en brukerfeil.
     */
    fun validate(
        dto: ArbeidsgiverensVirksomhetINorgeDto?,
        erOffentligArbeidsgiver: Boolean?
    ): List<Violation> {
        checkNotNull(erOffentligArbeidsgiver) { "Skjemaet mangler EREG-data om arbeidsgiveren er offentlig" }

        return if (erOffentligArbeidsgiver) {
            validerOffentligArbeidsgiver(dto)
        } else {
            validerPrivatArbeidsgiver(dto)
        }
    }

    /** Offentlig arbeidsgiver får ikke oppfølgingsspørsmålene, så seksjonen kan mangle helt. */
    private fun validerOffentligArbeidsgiver(dto: ArbeidsgiverensVirksomhetINorgeDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (dto.erArbeidsgiverenBemanningsEllerVikarbyraa != null) return listOf(
            Violation(
                field = ArbeidsgiverensVirksomhetINorgeDto::erArbeidsgiverenBemanningsEllerVikarbyraa.name,
                translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::offentligVirksomhetSkalIkkeOppgiBemanningsbyraa.name)
            )
        )
        if (dto.opprettholderArbeidsgiverenVanligDrift != null) return listOf(
            Violation(
                field = ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name,
                translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::offentligVirksomhetSkalIkkeOppgiVanligDrift.name)
            )
        )
        return emptyList()
    }

    private fun validerPrivatArbeidsgiver(dto: ArbeidsgiverensVirksomhetINorgeDto?): List<Violation> {
        if (dto == null) return listOf(
            Violation(
                field = "arbeidsgiverensVirksomhetINorge",
                translationKey = FELT_ER_PAAKREVD
            )
        )

        if (dto.erArbeidsgiverenBemanningsEllerVikarbyraa == null) return listOf(
            Violation(
                field = ArbeidsgiverensVirksomhetINorgeDto::erArbeidsgiverenBemanningsEllerVikarbyraa.name,
                translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::maaOppgiOmBemanningsbyraa.name)
            )
        )
        if (dto.opprettholderArbeidsgiverenVanligDrift == null) return listOf(
            Violation(
                field = ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name,
                translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::maaOppgiOmVanligDrift.name)
            )
        )
        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::arbeidsgiverensVirksomhetINorgeTranslation.name}.$fieldName"
        }
    }
}
