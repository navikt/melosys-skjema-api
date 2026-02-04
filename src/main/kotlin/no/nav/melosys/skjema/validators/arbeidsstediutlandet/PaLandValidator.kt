package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PaLandTranslation
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaLandDto
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class PaLandValidator {

    fun validate(dto: PaLandDto?): List<Violation> {
        if (dto == null) return emptyList()

        when (dto.fastEllerVekslendeArbeidssted) {
            FastEllerVekslendeArbeidssted.FAST -> {
                if (dto.fastArbeidssted == null) {
                    return listOf(Violation(
                        field = PaLandDto::fastArbeidssted.name,
                        translationKey = translationFieldName(PaLandTranslation::maaOppgiFastArbeidssted.name)
                    ))
                }
                if (dto.beskrivelseVekslende != null) {
                    return listOf(Violation(
                        field = PaLandDto::beskrivelseVekslende.name,
                        translationKey = translationFieldName(PaLandTranslation::beskrivelseVekslendeSkalIkkeOppgis.name)
                    ))
                }
            }

            FastEllerVekslendeArbeidssted.VEKSLENDE -> {
                if (dto.fastArbeidssted != null) {
                    return listOf(Violation(
                        field = PaLandDto::fastArbeidssted.name,
                        translationKey = translationFieldName(PaLandTranslation::fastArbeidsstedSkalIkkeOppgis.name)
                    ))
                }
                if (dto.beskrivelseVekslende.isNullOrBlank()) {
                    return listOf(Violation(
                        field = PaLandDto::beskrivelseVekslende.name,
                        translationKey = translationFieldName(PaLandTranslation::maaOppgiBeskrivelseVekslende.name)
                    ))
                }
            }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::paLandTranslation.name}.$fieldName"
        }
    }
}
