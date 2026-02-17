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
    val readSkjemadata: ClientConfig = ClientConfig()
) {
    data class ClientConfig(
        @field:NotEmpty(message = "m2m.read-skjemadata.clients må være konfigurert")
        val clients: List<@NotBlank String> = emptyList()
    )

    @PostConstruct
    fun validateNoUnresolvedPlaceholders() {
        readSkjemadata.clients.forEach { client ->
            require(!client.contains("\${")) {
                "Uoppløst placeholder i m2m.read-skjemadata.clients: '$client'. Sjekk at miljøvariabelen er satt."
            }
        }
    }
}
