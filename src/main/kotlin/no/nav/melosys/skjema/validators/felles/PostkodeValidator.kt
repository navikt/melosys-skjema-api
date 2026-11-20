package no.nav.melosys.skjema.validators.felles

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.FellesTranslation
import no.nav.melosys.skjema.validators.Violation

object PostkodeValidator {

    /** EESSI avviser SED-er med postkode (postalCode) over 25 tegn. */
    const val MAKS_LENGDE_POSTKODE = 25

    fun validate(felt: String, postkode: String?): List<Violation> =
        if ((postkode?.length ?: 0) > MAKS_LENGDE_POSTKODE) {
            listOf(Violation(
                field = felt,
                translationKey = "${ErrorMessageTranslation::fellesTranslation.name}.${FellesTranslation::postkodeErForLang.name}"
            ))
        } else {
            emptyList()
        }
}
