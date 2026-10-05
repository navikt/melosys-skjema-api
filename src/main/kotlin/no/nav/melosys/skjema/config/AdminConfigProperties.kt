package no.nav.melosys.skjema.config

import jakarta.annotation.PostConstruct
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Tilgangsstyring for admin-endepunktene: tokenets azp må være Consoles klient-ID
 * ([consoleKlientId]) (i tillegg til gyldig Azure AD-token). Personkall må i tillegg ha
 * driftsgruppen ([driftsgruppe]) i groups-claimet.
 */
@Validated
@ConfigurationProperties(prefix = "admin")
data class AdminConfigProperties(
    @field:NotBlank(message = "admin.console-klient-id må være konfigurert")
    val consoleKlientId: String = "",
    @field:NotBlank(message = "admin.driftsgruppe må være konfigurert")
    val driftsgruppe: String = ""
) {
    @PostConstruct
    fun validateNoUnresolvedPlaceholders() {
        listOf(consoleKlientId, driftsgruppe).forEach { verdi ->
            require(!verdi.contains("\${")) {
                "Uoppløst placeholder i admin-konfigurasjon: '$verdi'. Sjekk at miljøvariabelen er satt."
            }
        }
    }
}
