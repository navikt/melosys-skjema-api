package no.nav.melosys.skjema.validators.familiemedlemmer

import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class FamiliemedlemmerValidator {

    fun validate(dto: FamiliemedlemmerDto?): List<Violation> {
        if (dto == null) return listOf(Violation(
            field = "familiemedlemmer",
            translationKey = FELT_ER_PAAKREVD
        ))

        return emptyList()
    }
}

