package no.nav.melosys.skjema.integrasjon.clamav

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestClient

@Configuration
@Profile("!local-q1 & !local-q2")
class ClamAvConfig {

    @Bean
    fun clamAvRestClient(
        @Value("\${clamav.url}") clamAvUrl: String,
        restClientBuilder: RestClient.Builder
    ): RestClient {
        return restClientBuilder
            .baseUrl("$clamAvUrl/api/v2/scan")
            .build()
    }
}
