package no.nav.melosys.skjema.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "m2m")
data class M2mConfigProperties(
    val readSkjemadata: ClientConfig = ClientConfig()
) {
    data class ClientConfig(
        val clients: List<String> = emptyList()
    )
}
