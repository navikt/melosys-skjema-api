package no.nav.melosys.skjema.validators.skatteforholdoginntekt

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.SkatteforholdOgInntektTranslation
import no.nav.melosys.skjema.types.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class SkatteforholdOgInntektValidator {

    fun validate(dto: SkatteforholdOgInntektDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (dto.mottarPengestotteFraAnnetEosLandEllerSveits) {
            if (dto.landSomUtbetalerPengestotte.isNullOrBlank()) {
                return listOf(Violation(
                    field = SkatteforholdOgInntektDto::landSomUtbetalerPengestotte.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiLandSomUtbetalerPengestotte.name)
                ))
            }
            if (dto.pengestotteSomMottasFraAndreLandBelop.isNullOrBlank()) {
                return listOf(Violation(
                    field = SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBelop.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiBelopPengestotte.name)
                ))
            }
            if (dto.pengestotteSomMottasFraAndreLandBeskrivelse.isNullOrBlank()) {
                return listOf(Violation(
                    field = SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBeskrivelse.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiBeskrivelsePengestotte.name)
                ))
            }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::skatteforholdOgInntektTranslation.name}.$fieldName"
        }
    }
}
