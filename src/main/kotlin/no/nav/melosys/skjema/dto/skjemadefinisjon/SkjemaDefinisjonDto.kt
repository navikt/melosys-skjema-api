package no.nav.melosys.skjema.dto.skjemadefinisjon

/**
 * Hovedklasse for en skjemadefinisjon.
 * Inneholder alle seksjoner med deres felter.
 *
 * @property type Skjematype, f.eks. "A1"
 * @property versjon Versjon av definisjonen, f.eks. "1"
 * @property seksjoner Map fra seksjons-ID til seksjonsdefinisjon
 */
data class SkjemaDefinisjonDto(
    val type: String,
    val versjon: String,
    val seksjoner: Map<String, SeksjonDefinisjonDto>
)
