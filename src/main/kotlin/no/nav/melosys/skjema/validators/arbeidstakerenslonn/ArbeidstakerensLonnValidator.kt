package no.nav.melosys.skjema.validators.arbeidstakerenslonn

import no.nav.melosys.skjema.translations.dto.ArbeidstakerensLonnTranslation
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.felles.OrganisasjonsnummerValidator
import org.springframework.stereotype.Component

@Component
class ArbeidstakerensLonnValidator(
    private val organisasjonsnummerValidator: OrganisasjonsnummerValidator
) {

    fun validate(dto: ArbeidstakerensLonnDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (dto.arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden) {
            if (dto.virksomheterSomUtbetalerLonnOgNaturalytelser != null) {
                return listOf(Violation(
                    field = ArbeidstakerensLonnDto::virksomheterSomUtbetalerLonnOgNaturalytelser.name,
                    translationKey = translationFieldName(ArbeidstakerensLonnTranslation::virksomheterSkalIkkeOppgis.name)
                ))
            }
        } else {
            if (dto.virksomheterSomUtbetalerLonnOgNaturalytelser == null) {
                return listOf(Violation(
                    field = ArbeidstakerensLonnDto::virksomheterSomUtbetalerLonnOgNaturalytelser.name,
                    translationKey = translationFieldName(ArbeidstakerensLonnTranslation::maaOppgiVirksomheter.name)
                ))
            }
        }

        dto.virksomheterSomUtbetalerLonnOgNaturalytelser?.norskeVirksomheter?.forEach { virksomhet ->
            val violations = organisasjonsnummerValidator.validate(virksomhet.organisasjonsnummer)
            if (violations.isNotEmpty()) return violations
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::arbeidstakerensLonnTranslation.name}.$fieldName"
        }
    }
}
