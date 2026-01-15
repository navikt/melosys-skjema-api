package no.nav.melosys.skjema.controller.validators.familiemedlemmer

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.melosys.skjema.controller.validators.addViolation
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.Familiemedlem
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.FamiliemedlemmerTranslation
import org.springframework.stereotype.Component

@Component
class FamiliemedlemValidator : ConstraintValidator<GyldigFamiliemedlem, Familiemedlem> {

    override fun initialize(constraintAnnotation: GyldigFamiliemedlem?) {}

    override fun isValid(
        familiemedlem: Familiemedlem?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (familiemedlem == null) return true

        var isValid = true

        if (familiemedlem.fornavn.isBlank()) {
            context.addViolation(
                translationFieldName(FamiliemedlemmerTranslation::fornavnMaaOppgis.name),
                Familiemedlem::fornavn.name
            )
            isValid = false
        }

        if (familiemedlem.etternavn.isBlank()) {
            context.addViolation(
                translationFieldName(FamiliemedlemmerTranslation::etternavnMaaOppgis.name),
                Familiemedlem::etternavn.name
            )
            isValid = false
        }

        if (familiemedlem.harNorskFodselsnummerEllerDnummer) {
            if (familiemedlem.fodselsnummer.isNullOrBlank()) {
                context.addViolation(
                    translationFieldName(FamiliemedlemmerTranslation::fodselsnummerMaaOppgis.name),
                    Familiemedlem::fodselsnummer.name
                )
                isValid = false
            }
        } else {
            if (familiemedlem.fodselsdato == null) {
                context.addViolation(
                    translationFieldName(FamiliemedlemmerTranslation::fodselsdatoMaaOppgis.name),
                    Familiemedlem::fodselsdato.name
                )
                isValid = false
            }
        }

        return isValid
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::familiemedlemmerTranslation.name}.$fieldName"
        }
    }
}
