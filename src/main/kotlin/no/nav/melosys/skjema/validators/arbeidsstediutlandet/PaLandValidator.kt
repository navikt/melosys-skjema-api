package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PaLandTranslation
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandFastArbeidsstedDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.validerPaakrevdeTekstfelter
import org.springframework.stereotype.Component

@Component
class PaLandValidator {

    /** @param statsansattFlyt Krever at hjemmekontorsvaret mangler, i stedet for at det er besvart. */
    fun validate(dto: PaLandDto?, statsansattFlyt: Boolean = false): List<Violation> {
        if (dto == null) return listOf(Violation(
            field = "paLand",
            translationKey = FELT_ER_PAAKREVD
        ))

        val navnViolations = validerPaakrevdeTekstfelter(PaLandDto::navnPaVirksomhet.name to dto.navnPaVirksomhet)
        if (navnViolations.isNotEmpty()) return navnViolations

        if (statsansattFlyt && dto.erHjemmekontor != null) {
            return listOf(Violation(
                field = PaLandDto::erHjemmekontor.name,
                translationKey = translationFieldName(PaLandTranslation::hjemmekontorSkalIkkeOppgis.name)
            ))
        }

        if (!statsansattFlyt && dto.erHjemmekontor == null) {
            return listOf(Violation(
                field = PaLandDto::erHjemmekontor.name,
                translationKey = FELT_ER_PAAKREVD
            ))
        }

        when (dto.fastEllerVekslendeArbeidssted) {
            FastEllerVekslendeArbeidssted.FAST -> {
                val fastArbeidssted = dto.fastArbeidssted ?: return listOf(Violation(
                    field = PaLandDto::fastArbeidssted.name,
                    translationKey = translationFieldName(PaLandTranslation::maaOppgiFastArbeidssted.name)
                ))
                return validerPaakrevdeTekstfelter(
                    PaLandFastArbeidsstedDto::vegadresse.name to fastArbeidssted.vegadresse,
                    PaLandFastArbeidsstedDto::nummer.name to fastArbeidssted.nummer,
                    PaLandFastArbeidsstedDto::postkode.name to fastArbeidssted.postkode,
                    PaLandFastArbeidsstedDto::bySted.name to fastArbeidssted.bySted,
                )
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
