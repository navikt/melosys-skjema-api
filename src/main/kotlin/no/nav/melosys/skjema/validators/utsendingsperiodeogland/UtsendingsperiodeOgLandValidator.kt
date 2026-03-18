package no.nav.melosys.skjema.validators.utsendingsperiodeogland

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.UtsendingsperiodeOgLandTranslation
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.felles.PeriodeValidator
import org.springframework.stereotype.Component

@Component
class UtsendingsperiodeOgLandValidator {

    fun validate(dto: UtsendingsperiodeOgLandDto?): List<Violation> {
        if (dto == null) return listOf(Violation(
            field = "utsendingsperiodeOgLand",
            translationKey = FELT_ER_PAAKREVD
        ))

        if (dto.utsendelseLand == LandKode.NO) return listOf(Violation(
            field = UtsendingsperiodeOgLandDto::utsendelseLand.name,
            translationKey = translationFieldName(UtsendingsperiodeOgLandTranslation::norgeErIkkeGyldigSomUtsendelsesland.name)
        ))

        PeriodeValidator.validate(
            dto.utsendelsePeriode,
            fieldName = UtsendingsperiodeOgLandDto::utsendelsePeriode.name
        )
            .takeIf { it.isNotEmpty() }?.let { return it }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::utsendingsperiodeOgLandTranslation.name}.$fieldName"
        }
    }
}
