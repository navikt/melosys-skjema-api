package no.nav.melosys.skjema.validators.arbeidssituasjon

import no.nav.melosys.skjema.translations.dto.ArbeidssituasjonTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import org.springframework.stereotype.Component

@Component
class ArbeidssituasjonValidator {

    fun validate(dto: ArbeidssituasjonDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (!dto.harVaertEllerSkalVaereILonnetArbeidFoerUtsending) {
            if (dto.aktivitetIMaanedenFoerUtsendingen.isNullOrBlank()) {
                return listOf(Violation(
                    field = "aktivitetIMaanedenFoerUtsendingen",
                    translationKey = translationFieldName(ArbeidssituasjonTranslation::maaOppgiAktivitetFoerUtsending.name)
                ))
            }
        }

        if (dto.skalJobbeForFlereVirksomheter) {
            val virksomheter = dto.virksomheterArbeidstakerJobberForIutsendelsesPeriode
            val hasVirksomheter = virksomheter?.let {
                !it.norskeVirksomheter.isNullOrEmpty() || !it.utenlandskeVirksomheter.isNullOrEmpty()
            } ?: false

            if (!hasVirksomheter) {
                return listOf(Violation(
                    field = "virksomheterArbeidstakerJobberForIutsendelsesPeriode",
                    translationKey = translationFieldName(ArbeidssituasjonTranslation::maaOppgiMinstEnVirksomhet.name)
                ))
            }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::arbeidssituasjonTranslation.name}.$fieldName"
        }
    }
}
