package no.nav.melosys.skjema.types.skjemadefinisjon

/**
 * Et enkelt checkbox-alternativ i en [CheckboxGruppeFeltDefinisjon].
 *
 * @property verdi Teknisk nøkkel som identifiserer alternativet
 * @property label Visningslabel ved siden av checkboxen
 * @property valgt Om checkboxen er avhuket
 * @property beskrivelse Valgfri utfyllende beskrivelse/hjelpetekst
 */
data class CheckboxAlternativDefinisjonDto(
    val verdi: String,
    val label: String,
    val valgt: Boolean = false,
    val beskrivelse: String? = null
)


