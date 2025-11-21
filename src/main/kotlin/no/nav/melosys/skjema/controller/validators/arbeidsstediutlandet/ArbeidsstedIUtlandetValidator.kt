package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet.GyldigArbeidsstedIUtlandet
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import org.springframework.stereotype.Component

@Component
class ArbeidsstedIUtlandetValidator : ConstraintValidator<GyldigArbeidsstedIUtlandet, ArbeidsstedIUtlandetDto> {

    override fun initialize(constraintAnnotation: GyldigArbeidsstedIUtlandet?) {}

    override fun isValid(
        dto: ArbeidsstedIUtlandetDto?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (dto == null) return true

        return when(dto.arbeidsstedType) {
            ArbeidsstedType.PA_LAND -> dto.paLand != null && dto.offshore == null && dto.paSkip == null && dto.omBordPaFly == null
            ArbeidsstedType.OFFSHORE -> dto.offshore != null && dto.paLand == null && dto.paSkip == null && dto.omBordPaFly == null
            ArbeidsstedType.PA_SKIP -> dto.paSkip != null && dto.paLand == null && dto.offshore == null && dto.omBordPaFly == null
            ArbeidsstedType.OM_BORD_PA_FLY -> dto.omBordPaFly != null && dto.paLand == null && dto.offshore == null && dto.paSkip == null
        }
    }
}