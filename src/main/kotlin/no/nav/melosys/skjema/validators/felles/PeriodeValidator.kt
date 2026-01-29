package no.nav.melosys.skjema.validators.felles

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.PeriodeTranslation
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.dto.felles.PeriodeDto
import org.springframework.stereotype.Component

object PeriodeValidator {

    fun validate(dto: PeriodeDto?, fieldName: String = ""): List<Violation> {
        if (dto == null) return emptyList()

        if (dto.fraDato.isAfter(dto.tilDato)) {
            return listOf(Violation(
                field = fieldName,
                translationKey = translationFieldName(PeriodeTranslation::fraDatoMaaVaereFoerTilDato.name)
            ))
        }

        return emptyList()
    }


    private fun translationFieldName(fieldName: String): String {
        return  "${ErrorMessageTranslation::periodeTranslation.name}.$fieldName"
    }

}
