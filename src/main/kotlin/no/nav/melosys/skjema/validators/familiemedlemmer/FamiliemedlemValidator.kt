package no.nav.melosys.skjema.validators.familiemedlemmer

import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.Familiemedlem
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.FamiliemedlemmerTranslation
import no.nav.melosys.skjema.validators.felles.ErFodselsEllerDNummerValidator
import org.springframework.stereotype.Component

@Component
class FamiliemedlemValidator(
    val fodselsEllerDNummerValidator: ErFodselsEllerDNummerValidator
) {

    fun validate(familiemedlem: Familiemedlem?): List<Violation> {
        if (familiemedlem == null) return emptyList()

        if (familiemedlem.fornavn.isBlank()) {
            return listOf(Violation(
                field = Familiemedlem::fornavn.name,
                translationKey = translationFieldName(FamiliemedlemmerTranslation::fornavnMaaOppgis.name)
            ))
        }

        if (familiemedlem.etternavn.isBlank()) {
            return listOf(Violation(
                field = Familiemedlem::etternavn.name,
                translationKey = translationFieldName(FamiliemedlemmerTranslation::etternavnMaaOppgis.name)
            ))
        }

        if (familiemedlem.harNorskFodselsnummerEllerDnummer) {
            if (familiemedlem.fodselsnummer.isNullOrBlank()) {
                return listOf(Violation(
                    field = Familiemedlem::fodselsnummer.name,
                    translationKey = translationFieldName(FamiliemedlemmerTranslation::fodselsnummerMaaOppgis.name)
                ))
            }
            val ugyldigFoedselsnummerViolation = fodselsEllerDNummerValidator.validate(familiemedlem.fodselsnummer)
            if (ugyldigFoedselsnummerViolation.isNotEmpty()) return ugyldigFoedselsnummerViolation

        } else {
            if (familiemedlem.fodselsdato == null) {
                return listOf(Violation(
                    field = Familiemedlem::fodselsdato.name,
                    translationKey = translationFieldName(FamiliemedlemmerTranslation::fodselsdatoMaaOppgis.name)
                ))
            }
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::familiemedlemmerTranslation.name}.$fieldName"
        }
    }
}
