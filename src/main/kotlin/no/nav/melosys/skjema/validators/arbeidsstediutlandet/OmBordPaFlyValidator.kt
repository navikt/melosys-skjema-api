package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.OmBordPaFlyTranslation
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.OmBordPaFlyDto
import org.springframework.stereotype.Component

@Component
class OmBordPaFlyValidator {

    fun validate(dto: OmBordPaFlyDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (dto.erVanligHjemmebase) {
            if (dto.vanligHjemmebaseLand != null) {
                return listOf(Violation(
                    field = OmBordPaFlyDto::vanligHjemmebaseLand.name,
                    translationKey = translationFieldName(OmBordPaFlyTranslation::vanligHjemmebaseLandSkalIkkeOppgis.name)
                ))
            }
            if (dto.vanligHjemmebaseNavn != null) {
                return listOf(Violation(
                    field = OmBordPaFlyDto::vanligHjemmebaseNavn.name,
                    translationKey = translationFieldName(OmBordPaFlyTranslation::vanligHjemmebaseNavnSkalIkkeOppgis.name)
                ))
            }
        } else {
            if (dto.vanligHjemmebaseLand == null) {
                return listOf(Violation(
                    field = OmBordPaFlyDto::vanligHjemmebaseLand.name,
                    translationKey = translationFieldName(OmBordPaFlyTranslation::maaOppgiVanligHjemmebaseLand.name)
                ))
            }
            if (dto.vanligHjemmebaseNavn == null) {
                return listOf(Violation(
                    field = OmBordPaFlyDto::vanligHjemmebaseNavn.name,
                    translationKey = translationFieldName(OmBordPaFlyTranslation::maaOppgiVanligHjemmebaseNavn.name)
                ))
            }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::omBordPaFlyTranslation.name}.$fieldName"
        }
    }
}
