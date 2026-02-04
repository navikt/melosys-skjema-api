package no.nav.melosys.skjema.validators.tilleggsopplysninger

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.TilleggsopplysningerTranslation
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class TilleggsopplysningerValidator {

    fun validate(dto: TilleggsopplysningerDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (dto.harFlereOpplysningerTilSoknaden) {
            if (dto.tilleggsopplysningerTilSoknad.isNullOrBlank()) {
                return listOf(Violation(
                    field = TilleggsopplysningerDto::tilleggsopplysningerTilSoknad.name,
                    translationKey = translationFieldName(TilleggsopplysningerTranslation::maaOppgiTilleggsopplysninger.name)
                ))
            }
        } else {
            if (dto.tilleggsopplysningerTilSoknad != null) {
                return listOf(Violation(
                    field = TilleggsopplysningerDto::tilleggsopplysningerTilSoknad.name,
                    translationKey = translationFieldName(TilleggsopplysningerTranslation::tilleggsopplysningerSkalIkkeOppgis.name)
                ))
            }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::tilleggsopplysningerTranslation.name}.$fieldName"
        }
    }
}
