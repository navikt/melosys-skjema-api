package no.nav.melosys.skjema.dto.skjemadefinisjon

/**
 * Definisjon av en seksjon i et skjema.
 *
 * @property tittel Visningstittel for seksjonen
 * @property beskrivelse Valgfri beskrivelse/hjelpetekst for seksjonen
 * @property felter Map fra felt-ID til feltdefinisjon
 */
data class SeksjonDefinisjonDto(
    val tittel: String,
    val beskrivelse: String? = null,
    val felter: Map<String, FeltDefinisjonDto>
)
