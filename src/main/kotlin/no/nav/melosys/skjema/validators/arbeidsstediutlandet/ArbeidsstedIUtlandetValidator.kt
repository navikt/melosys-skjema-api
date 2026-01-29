package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import no.nav.melosys.skjema.translations.dto.ArbeidsstedIUtlandetTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import org.springframework.stereotype.Component

@Component
class ArbeidsstedIUtlandetValidator(
    private val omBordPaFlyValidator: OmBordPaFlyValidator,
    private val paLandValidator: PaLandValidator,
    private val paSkipValidator: PaSkipValidator
) {

    fun validate(dto: ArbeidsstedIUtlandetDto?): List<Violation> {
        if (dto == null) return emptyList()

        return when(dto.arbeidsstedType) {
            ArbeidsstedType.PA_LAND -> {
                if (dto.paLand == null || dto.offshore != null || dto.paSkip != null || dto.omBordPaFly != null) {
                    listOf(Violation(
                        field = ArbeidsstedIUtlandetDto::paLand.name,
                        translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedPaLand.name)
                    ))
                } else paLandValidator.validate(dto.paLand)
            }
            ArbeidsstedType.OFFSHORE -> {
                if (dto.offshore == null || dto.paLand != null || dto.paSkip != null || dto.omBordPaFly != null) {
                    listOf(Violation(
                        field = ArbeidsstedIUtlandetDto::offshore.name,
                        translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiOffshoreArbeidssted.name)
                    ))
                } else emptyList()
            }
            ArbeidsstedType.PA_SKIP -> {
                if (dto.paSkip == null || dto.paLand != null || dto.offshore != null || dto.omBordPaFly != null) {
                    listOf(Violation(
                        field = ArbeidsstedIUtlandetDto::paSkip.name,
                        translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedPaSkip.name)
                    ))
                } else paSkipValidator.validate(dto.paSkip)
            }
            ArbeidsstedType.OM_BORD_PA_FLY -> {
                if (dto.omBordPaFly == null || dto.paLand != null || dto.offshore != null || dto.paSkip != null) {
                    listOf(Violation(
                        field = ArbeidsstedIUtlandetDto::omBordPaFly.name,
                        translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedOmBordPaFly.name)
                    ))
                } else omBordPaFlyValidator.validate(dto.omBordPaFly)
            }
        }
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::arbeidsstedIUtlandetTranslation.name}.$fieldName"
        }
    }
}