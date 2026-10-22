package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PaLandTranslation
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class PaLandValidator {

    /** @param forenklet offentlig arbeidsgiver: hjemmekontor-spørsmålet stilles ikke. */
    fun validate(dto: PaLandDto?, forenklet: Boolean = false): List<Violation> {
        if (dto == null) return listOf(Violation(
            field = "paLand",
            translationKey = FELT_ER_PAAKREVD
        ))

        if (forenklet && dto.erHjemmekontor != null) {
            return listOf(Violation(
                field = PaLandDto::erHjemmekontor.name,
                translationKey = translationFieldName(PaLandTranslation::hjemmekontorSkalIkkeOppgis.name)
            ))
        }

        if (!forenklet && dto.erHjemmekontor == null) {
            return listOf(Violation(
                field = PaLandDto::erHjemmekontor.name,
                translationKey = FELT_ER_PAAKREVD
            ))
        }

        when (dto.fastEllerVekslendeArbeidssted) {
            FastEllerVekslendeArbeidssted.FAST -> {
                if (dto.fastArbeidssted == null) {
                    return listOf(Violation(
                        field = PaLandDto::fastArbeidssted.name,
                        translationKey = translationFieldName(PaLandTranslation::maaOppgiFastArbeidssted.name)
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
