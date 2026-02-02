package no.nav.melosys.skjema.validators.familiemedlemmer

import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.FamiliemedlemmerTranslation
import org.springframework.stereotype.Component

@Component
class FamiliemedlemmerValidator(
    private val familiemedlemValidator: FamiliemedlemValidator,
) {

    fun validate(dto: FamiliemedlemmerDto?): List<Violation> {
        if (dto == null) return emptyList()

        if (!dto.skalHaMedFamiliemedlemmer && dto.familiemedlemmer.isNotEmpty()) {
            return listOf(Violation(
                field = FamiliemedlemmerDto::familiemedlemmer.name,
                translationKey = translationFieldName(FamiliemedlemmerTranslation::familiemedlemmerMaaVaereTomNarSkalHaMedFamiliemedlemmerErFalse.name)
            ))
        }

        dto.familiemedlemmer.forEach {
            val violations = familiemedlemValidator.validate(it)
            if (violations.isNotEmpty()) return violations
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::familiemedlemmerTranslation.name}.$fieldName"
        }
    }
}
