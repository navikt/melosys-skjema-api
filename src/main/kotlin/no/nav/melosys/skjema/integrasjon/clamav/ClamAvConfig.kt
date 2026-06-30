package no.nav.melosys.skjema.integrasjon.clamav

import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
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
        @Value("\${clamav.read-timeout}") readTimeout: Duration,
        restClientBuilder: RestClient.Builder,
        requestFactoryBuilder: ClientHttpRequestFactoryBuilder<*>,
        httpClientSettings: HttpClientSettings
    ): RestClient {
        // Virusscanning kan ta lengre tid enn global read-timeout. Beholder global
        // connect-timeout/SSL, men hever read-timeout for denne klienten.
        val requestFactory = requestFactoryBuilder.build(httpClientSettings.withReadTimeout(readTimeout))

        return restClientBuilder
            .baseUrl("$clamAvUrl/api/v2/scan")
            .requestFactory(requestFactory)
            .build()
    }
}
