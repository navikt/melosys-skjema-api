package no.nav.melosys.skjema.config

import jakarta.annotation.PostConstruct
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "m2m")
data class M2mConfigProperties(
    @field:Valid
    val readSkjemadata: ClientConfig = ClientConfig(),
    @field:Valid
    val admin: AdminConfig = AdminConfig()
) {
    data class ClientConfig(
        @field:NotEmpty(message = "m2m-klientliste må være konfigurert")
        val clients: List<@NotBlank String> = emptyList()
    )

    /**
     * Tilgangsstyring for admin-endepunktene: både azp_name-allowlist ([clients]) og en delt
     * API-nøkkel ([apikey]) må stemme (i tillegg til gyldig Azure AD-token).
     */
    data class AdminConfig(
        @field:NotEmpty(message = "m2m.admin.clients må være konfigurert")
        val clients: List<@NotBlank String> = emptyList(),
        @field:NotBlank(message = "m2m.admin.apikey må være konfigurert")
        val apikey: String = ""
    )

    @PostConstruct
    fun validateNoUnresolvedPlaceholders() {
        (readSkjemadata.clients + admin.clients + admin.apikey).forEach { verdi ->
            require(!verdi.contains("\${")) {
                "Uoppløst placeholder i m2m-konfigurasjon: '$verdi'. Sjekk at miljøvariabelen er satt."
            }
        }
    }
}
