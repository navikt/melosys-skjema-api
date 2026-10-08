package no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge

import kotlin.reflect.KProperty1
import no.nav.melosys.skjema.translations.dto.ArbeidsgiverensVirksomhetINorgeTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class ArbeidsgiverensVirksomhetINorgeValidator {

    /**
     * Klassifiseringen og ansattallet kommer fra Enhetsregisteret, ikke fra søkeren, og avgjør hvilke
     * oppfølgingsspørsmål seksjonen skal ha. Mangler de, er det en systemfeil og ikke en brukerfeil.
     */
    fun validate(
        dto: ArbeidsgiverensVirksomhetINorgeDto?,
        erOffentligArbeidsgiver: Boolean?,
        antallAnsatte: Int?
    ): List<Violation> {
        checkNotNull(erOffentligArbeidsgiver) { "Skjemaet mangler EREG-data om arbeidsgiveren er offentlig" }

        return if (erOffentligArbeidsgiver) {
            validerOffentligArbeidsgiver(dto)
        } else {
            validerPrivatArbeidsgiver(
                dto,
                checkNotNull(antallAnsatte) { "Skjemaet mangler EREG-data om antall ansatte" }
            )
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
        return avvisSamletVirksomhet(dto)
    }

    private fun validerPrivatArbeidsgiver(dto: ArbeidsgiverensVirksomhetINorgeDto?, antallAnsatte: Int): List<Violation> {
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
        return if (skalOppgiSamletVirksomhet(antallAnsatte, dto.erArbeidsgiverenBemanningsEllerVikarbyraa)) {
            if (dto.opprettholderArbeidsgiverenVanligDrift != null) return listOf(
                Violation(
                    field = ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name,
                    translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::skalIkkeOppgiVanligDrift.name)
                )
            )
            validerSamletVirksomhet(dto)
        } else {
            if (dto.opprettholderArbeidsgiverenVanligDrift == null) return listOf(
                Violation(
                    field = ArbeidsgiverensVirksomhetINorgeDto::opprettholderArbeidsgiverenVanligDrift.name,
                    translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::maaOppgiOmVanligDrift.name)
                )
            )
            avvisSamletVirksomhet(dto)
        }
    }

    private fun validerSamletVirksomhet(dto: ArbeidsgiverensVirksomhetINorgeDto): List<Violation> =
        ANTALL_FELTER.mapNotNull { validerFelt(it, dto, gyldig = { verdi -> verdi >= 0 }, ArbeidsgiverensVirksomhetINorgeTranslation::antallMaaVaereNullEllerMer) } +
            ANDEL_FELTER.mapNotNull { validerFelt(it, dto, gyldig = { verdi -> verdi in 0..100 }, ArbeidsgiverensVirksomhetINorgeTranslation::andelMaaVaereMellom0Og100) }

    private fun validerFelt(
        felt: KProperty1<ArbeidsgiverensVirksomhetINorgeDto, Int?>,
        dto: ArbeidsgiverensVirksomhetINorgeDto,
        gyldig: (Int) -> Boolean,
        ugyldigMelding: KProperty1<ArbeidsgiverensVirksomhetINorgeTranslation, String>
    ): Violation? {
        val verdi = felt.get(dto)
        return when {
            verdi == null -> Violation(field = felt.name, translationKey = FELT_ER_PAAKREVD)
            !gyldig(verdi) -> Violation(field = felt.name, translationKey = translationFieldName(ugyldigMelding.name))
            else -> null
        }
    }

    private fun avvisSamletVirksomhet(dto: ArbeidsgiverensVirksomhetINorgeDto): List<Violation> =
        (ANTALL_FELTER + ANDEL_FELTER)
            .filter { it.get(dto) != null }
            .map {
                Violation(
                    field = it.name,
                    translationKey = translationFieldName(ArbeidsgiverensVirksomhetINorgeTranslation::skalIkkeOppgiSamletVirksomhet.name)
                )
            }

    companion object {
        /** Under denne grensen må arbeidsgiver oppgi opplysninger om foretakets samlede virksomhet. */
        const val ANSATTGRENSE_SAMLET_VIRKSOMHET = 20

        fun skalOppgiSamletVirksomhet(antallAnsatte: Int, erBemanningsEllerVikarbyraa: Boolean?): Boolean =
            antallAnsatte < ANSATTGRENSE_SAMLET_VIRKSOMHET || erBemanningsEllerVikarbyraa == true

        private val ANTALL_FELTER = listOf(
            ArbeidsgiverensVirksomhetINorgeDto::antallAdministrativtAnsatte,
            ArbeidsgiverensVirksomhetINorgeDto::antallUtsendteArbeidstakere
        )

        private val ANDEL_FELTER = listOf(
            ArbeidsgiverensVirksomhetINorgeDto::andelAnsatteRekruttertINorge,
            ArbeidsgiverensVirksomhetINorgeDto::andelOmsetningINorge,
            ArbeidsgiverensVirksomhetINorgeDto::andelOppdragUtfortINorge,
            ArbeidsgiverensVirksomhetINorgeDto::andelOppdragskontrakterInngattINorge
        )

        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::arbeidsgiverensVirksomhetINorgeTranslation.name}.$fieldName"
        }
    }
}
