package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.translations.dto.ArbeidsstedIUtlandetTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.controller.validators.addViolation
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
            ArbeidsstedType.PA_LAND -> {
                if (dto.paLand == null || dto.offshore != null || dto.paSkip != null || dto.omBordPaFly != null) {
                    context.addViolation(
                        translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedPaLand.name),
                        ArbeidsstedIUtlandetDto::paLand.name
                    )
                    false
                } else true
            }
            ArbeidsstedType.OFFSHORE -> {
                if (dto.offshore == null || dto.paLand != null || dto.paSkip != null || dto.omBordPaFly != null) {
                    context.addViolation(
                        translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiOffshoreArbeidssted.name),
                        ArbeidsstedIUtlandetDto::offshore.name
                    )
                    false
                } else true
            }
            ArbeidsstedType.PA_SKIP -> {
                if (dto.paSkip == null || dto.paLand != null || dto.offshore != null || dto.omBordPaFly != null) {
                    context.addViolation(
                        translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedPaSkip.name),
                        ArbeidsstedIUtlandetDto::paSkip.name
                    )
                    false
                } else true
            }
            ArbeidsstedType.OM_BORD_PA_FLY -> {
                if (dto.omBordPaFly == null || dto.paLand != null || dto.offshore != null || dto.paSkip != null) {
                    context.addViolation(
                        translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedOmBordPaFly.name),
                        ArbeidsstedIUtlandetDto::omBordPaFly.name
                    )
                    false
                } else true
            }
        }
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return  "${ErrorMessageTranslation::arbeidsstedIUtlandetTranslation.name}.$fieldName"
        }
    }
}