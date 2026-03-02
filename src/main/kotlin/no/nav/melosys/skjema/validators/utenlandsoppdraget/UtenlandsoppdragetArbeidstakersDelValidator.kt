package no.nav.melosys.skjema.validators.utenlandsoppdraget

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.types.felles.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.felles.PeriodeValidator
import org.springframework.stereotype.Component

@Component
class UtenlandsoppdragetArbeidstakersDelValidator() {

    fun validate(dto: UtsendingsperiodeOgLandDto?): List<Violation> {
        if (dto == null) return emptyList()

        PeriodeValidator.validate(
            dto.utsendelsePeriode,
            fieldName = UtsendingsperiodeOgLandDto::utsendelsePeriode.name
        )
            .takeIf { it.isNotEmpty() }?.let { return it }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::utenlandsoppdragetTranslation.name}.$fieldName"
        }
    }
}
