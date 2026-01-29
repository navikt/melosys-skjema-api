package no.nav.melosys.skjema.dto.skjemadefinisjon

/**
 * Et alternativ for et SELECT-felt.
 *
 * @property verdi Den tekniske verdien som lagres
 * @property label Visningslabel for alternativet
 * @property beskrivelse Valgfri beskrivelse/hjelpetekst
 */
data class AlternativDefinisjonDto(
    val verdi: String,
    val label: String,
    val beskrivelse: String? = null
)
