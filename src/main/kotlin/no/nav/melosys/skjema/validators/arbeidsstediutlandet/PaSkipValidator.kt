package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PaSkipTranslation
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.Farvann
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaSkipDto
import org.springframework.stereotype.Component

@Component
class PaSkipValidator {

    fun validate(dto: PaSkipDto?): List<Violation> {
        if (dto == null) return emptyList()

        when(dto.seilerI) {
            Farvann.INTERNASJONALT_FARVANN -> {
                if (dto.flaggland == null) {
                    return listOf(Violation(
                        field = PaSkipDto::flaggland.name,
                        translationKey = translationFieldName(PaSkipTranslation::duMaOppgiFlaggland.name)
                    ))
                }
                if (dto.territorialfarvannLand != null) {
                    return listOf(Violation(
                        field = PaSkipDto::territorialfarvannLand.name,
                        translationKey = translationFieldName(PaSkipTranslation::territorialfarvannLandSkalIkkeOppgis.name)
                    ))
                }
            }
            Farvann.TERRITORIALFARVANN -> {
                if (dto.territorialfarvannLand == null) {
                    return listOf(Violation(
                        field = PaSkipDto::territorialfarvannLand.name,
                        translationKey = translationFieldName(PaSkipTranslation::duMaOppgiTerritorialfarvannLand.name)
                    ))
                }
                if (dto.flaggland != null) {
                    return listOf(Violation(
                        field = PaSkipDto::flaggland.name,
                        translationKey = translationFieldName(PaSkipTranslation::flagglandSkalIkkeOppgis.name)
                    ))
                }
            }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::paSkipTranslation.name}.$fieldName"
        }
    }
}
