package no.nav.melosys.skjema.validators.utsendingsperiodeogland

import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.felles.PeriodeValidator
import org.springframework.stereotype.Component

@Component
class UtsendingsperiodeOgLandValidator {

    fun validate(dto: UtsendingsperiodeOgLandDto?): List<Violation> {
        if (dto == null) return emptyList()

        PeriodeValidator.validate(
            dto.utsendelsePeriode,
            fieldName = UtsendingsperiodeOgLandDto::utsendelsePeriode.name
        )
            .takeIf { it.isNotEmpty() }?.let { return it }

        return emptyList()
    }
}
