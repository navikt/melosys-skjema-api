package no.nav.melosys.skjema.types.skjemadefinisjon

/**
 * Et enkelt checkbox-alternativ i en [CheckboxGruppeFeltDefinisjon].
 *
 * @property key Teknisk nøkkel som identifiserer alternativet
 * @property label Visningslabel ved siden av checkboxen
 * @property valgt Om checkboxen er avhuket
 * @property beskrivelse Valgfri utfyllende beskrivelse/hjelpetekst
 */
data class CheckboxAlternativDefinisjonDto(
    val key: String,
    val label: String,
    val valgt: Boolean = false,
    val beskrivelse: String? = null
)


