package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import no.nav.melosys.skjema.translations.dto.ArbeidsstedIUtlandetTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OffshoreDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.validerPaakrevdeTekstfelter
import org.springframework.stereotype.Component

@Component
class ArbeidsstedIUtlandetValidator(
    private val omBordPaFlyValidator: OmBordPaFlyValidator,
    private val paLandValidator: PaLandValidator,
    private val paSkipValidator: PaSkipValidator
) {

    /**
     * @param skalOppgis false når skjemadelen ikke har arbeidssted-steget, og svar ikke skal finnes.
     * @param statsansattFlyt Krever arbeidssted på land og at hjemmekontorsvaret mangler.
     */
    fun validate(dto: ArbeidsstedIUtlandetDto?, skalOppgis: Boolean = true, statsansattFlyt: Boolean = false): List<Violation> {
        if (!skalOppgis) {
            return if (dto == null) emptyList() else listOf(Violation(
                field = "arbeidsstedIUtlandet",
                translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::arbeidsstedSkalIkkeOppgis.name)
            ))
        }

        if (dto == null) return listOf(Violation(
            field = "arbeidsstedIUtlandet",
            translationKey = FELT_ER_PAAKREVD
        ))

        if (statsansattFlyt && dto.arbeidsstedType != ArbeidsstedType.PA_LAND) {
            return listOf(Violation(
                field = ArbeidsstedIUtlandetDto::paLand.name,
                translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedPaLand.name)
            ))
        }

        return when(dto.arbeidsstedType) {
            ArbeidsstedType.PA_LAND -> {
                if (dto.paLand == null || dto.offshore != null || dto.paSkip != null || dto.omBordPaFly != null) {
                    listOf(Violation(
                        field = ArbeidsstedIUtlandetDto::paLand.name,
                        translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiArbeidsstedPaLand.name)
                    ))
                } else paLandValidator.validate(dto.paLand, statsansattFlyt)
            }
            ArbeidsstedType.OFFSHORE -> {
                val offshore = dto.offshore
                if (offshore == null || dto.paLand != null || dto.paSkip != null || dto.omBordPaFly != null) {
                    listOf(Violation(
                        field = ArbeidsstedIUtlandetDto::offshore.name,
                        translationKey = translationFieldName(ArbeidsstedIUtlandetTranslation::maaOppgiOffshoreArbeidssted.name)
                    ))
                } else validerPaakrevdeTekstfelter(
                    OffshoreDto::navnPaVirksomhet.name to offshore.navnPaVirksomhet,
                    OffshoreDto::navnPaInnretning.name to offshore.navnPaInnretning,
                )
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