package no.nav.melosys.skjema.config

import jakarta.annotation.PostConstruct
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Tilgangsstyring for admin-endepunktene: både azp_name-allowlist ([clients]) og en delt
 * API-nøkkel ([apikey]) må stemme (i tillegg til gyldig Azure AD-token). Personkall må i
 * tillegg ha driftsgruppen ([driftsgruppe]) i groups-claimet.
 */
@Validated
@ConfigurationProperties(prefix = "admin")
data class AdminConfigProperties(
    @field:NotEmpty(message = "admin.clients må være konfigurert")
    val clients: List<@NotBlank String> = emptyList(),
    @field:NotBlank(message = "admin.apikey må være konfigurert")
    val apikey: String = "",
    @field:NotBlank(message = "admin.driftsgruppe må være konfigurert")
    val driftsgruppe: String = ""
) {
    @PostConstruct
    fun validateNoUnresolvedPlaceholders() {
        (clients + apikey + driftsgruppe).forEach { verdi ->
            require(!verdi.contains("\${")) {
                "Uoppløst placeholder i admin-konfigurasjon: '$verdi'. Sjekk at miljøvariabelen er satt."
            }
        }
    }
}
