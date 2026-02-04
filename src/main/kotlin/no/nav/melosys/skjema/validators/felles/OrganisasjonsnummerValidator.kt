package no.nav.melosys.skjema.validators.felles

import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.FellesTranslation
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class OrganisasjonsnummerValidator(
    val eregService: EregService
) {
    // https://www.brreg.no/om-oss/registrene-vare/om-enhetsregisteret/organisasjonsnummeret/
    fun validate(organisasjonsnummer: String?): List<Violation> {
        if (organisasjonsnummer == null) return emptyList()

        if (!organisasjonsnummerHarGyldigFormat(organisasjonsnummer)) {
            return listOf(Violation(
                field = "",
                translationKey = translationFieldName(FellesTranslation::organisasjonsnummerHarUgyldigFormat.name)
            ))
        }

        if (!eregService.organisasjonsnummerEksisterer(organisasjonsnummer)) {
            return listOf(Violation(
                field = "",
                translationKey = translationFieldName(FellesTranslation::organisasjonsnummerFinnesIkke.name)
            ))
        }

        return emptyList()
    }

    companion object {
        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::fellesTranslation.name}.$fieldName"
        }

        fun organisasjonsnummerHarGyldigFormat(organisasjonsnummer: String): Boolean {
            // Must be exactly 9 digits
            if (!organisasjonsnummer.matches(Regex("[0-9]{9}"))) return false

            // MOD11 validation
            val weights = listOf(3, 2, 7, 6, 5, 4, 3, 2)
            val digits = organisasjonsnummer.map { it.toString().toInt() }

            // Take first 8 digits and multiply with weights
            val sum = digits.take(8).zip(weights).sumOf { (digit, weight) -> digit * weight }

            // Calculate remainder
            val remainder = sum % 11
            val checkDigit = 11 - remainder

            // If checkDigit is 10, the organization number is invalid
            if (checkDigit == 10) return false

            // If checkDigit is 11, it should be 0
            val expectedCheckDigit = if (checkDigit == 11) 0 else checkDigit

            // Compare with the 9th digit (last digit)
            return expectedCheckDigit == digits[8]
        }
    }
}
