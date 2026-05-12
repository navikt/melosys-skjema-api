package no.nav.melosys.skjema.validators.vedlegg

import no.nav.melosys.skjema.types.felles.VedleggValgDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class VedleggValgValidator {

    fun validate(dto: VedleggValgDto?): List<Violation> {
        if (dto == null) {
            return listOf(
                Violation(
                    field = "vedlegg",
                    translationKey = FELT_ER_PAAKREVD
                )
            )
        }

        return emptyList()
    }
}
