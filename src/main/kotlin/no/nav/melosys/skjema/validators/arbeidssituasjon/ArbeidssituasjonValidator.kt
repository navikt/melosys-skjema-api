package no.nav.melosys.skjema.validators.arbeidssituasjon

import no.nav.melosys.skjema.translations.dto.ArbeidssituasjonTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.felles.UtenlandskVirksomhetValidator
import org.springframework.stereotype.Component

@Component
class ArbeidssituasjonValidator {

    /** @param statsansattFlyt Spørsmål om lønnet arbeid og aktivitet før utsending skal ikke besvares. */
    fun validate(dto: ArbeidssituasjonDto?, statsansattFlyt: Boolean = false): List<Violation> {
        if (dto == null) return listOf(Violation(
            field = "arbeidssituasjon",
            translationKey = FELT_ER_PAAKREVD
        ))

        val lonnetArbeidViolations = if (statsansattFlyt) validateStatsansatt(dto) else validateLonnetArbeidFoerUtsending(dto)
        if (lonnetArbeidViolations.isNotEmpty()) return lonnetArbeidViolations

        if (dto.skalJobbeForFlereVirksomheter) {
            val virksomheter = dto.virksomheterArbeidstakerJobberForIutsendelsesPeriode
            val hasVirksomheter = virksomheter?.let {
                !it.norskeVirksomheter.isNullOrEmpty() || !it.utenlandskeVirksomheter.isNullOrEmpty()
            } ?: false

            if (!hasVirksomheter) {
                return listOf(Violation(
                    field = "virksomheterArbeidstakerJobberForIutsendelsesPeriode",
                    translationKey = translationFieldName(ArbeidssituasjonTranslation::maaOppgiMinstEnVirksomhet.name)
                ))
            }

            val utenlandskVirksomhetViolations = UtenlandskVirksomhetValidator.validate(virksomheter?.utenlandskeVirksomheter)
            if (utenlandskVirksomhetViolations.isNotEmpty()) return utenlandskVirksomhetViolations
        }

        return emptyList()
    }

    private fun validateLonnetArbeidFoerUtsending(dto: ArbeidssituasjonDto): List<Violation> {
        val harLonnetArbeid = dto.harVaertEllerSkalVaereILonnetArbeidFoerUtsending ?: return listOf(Violation(
            field = ArbeidssituasjonDto::harVaertEllerSkalVaereILonnetArbeidFoerUtsending.name,
            translationKey = FELT_ER_PAAKREVD
        ))

        if (!harLonnetArbeid && dto.aktivitetIMaanedenFoerUtsendingen.isNullOrBlank()) {
            return listOf(Violation(
                field = ArbeidssituasjonDto::aktivitetIMaanedenFoerUtsendingen.name,
                translationKey = translationFieldName(ArbeidssituasjonTranslation::maaOppgiAktivitetFoerUtsending.name)
            ))
        }

        return emptyList()
    }

    private fun validateStatsansatt(dto: ArbeidssituasjonDto): List<Violation> {
        if (dto.harVaertEllerSkalVaereILonnetArbeidFoerUtsending != null) {
            return listOf(Violation(
                field = ArbeidssituasjonDto::harVaertEllerSkalVaereILonnetArbeidFoerUtsending.name,
                translationKey = translationFieldName(ArbeidssituasjonTranslation::lonnetArbeidFoerUtsendingSkalIkkeOppgis.name)
            ))
        }

        if (!dto.aktivitetIMaanedenFoerUtsendingen.isNullOrBlank()) {
            return listOf(Violation(
                field = ArbeidssituasjonDto::aktivitetIMaanedenFoerUtsendingen.name,
                translationKey = translationFieldName(ArbeidssituasjonTranslation::aktivitetFoerUtsendingSkalIkkeOppgis.name)
            ))
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::arbeidssituasjonTranslation.name}.$fieldName"
        }
    }
}
