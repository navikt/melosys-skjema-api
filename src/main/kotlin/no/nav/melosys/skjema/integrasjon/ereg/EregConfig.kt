package no.nav.melosys.skjema.integrasjon.ereg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger { }

@Configuration
class EregConfig(
    @param:Value("\${ereg.url}") private val eregBaseUrl: String
) {

    @Bean
    fun eregRestClient(restClientBuilder: RestClient.Builder): RestClient {
        log.info { "Konfigurerer EregClient med base URL: $eregBaseUrl" }

        return restClientBuilder
            .baseUrl(eregBaseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Nav-Consumer-Id", "melosys-skjema-api")
            .build()
    }
}
