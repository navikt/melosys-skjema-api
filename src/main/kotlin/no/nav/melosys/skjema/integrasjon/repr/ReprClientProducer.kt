package no.nav.melosys.skjema.integrasjon.repr

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.AuthorizationHeaderInterceptorFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger { }

@Configuration
class ReprClientProducer(
    @param:Value("\${repr.url}") private val reprBaseUrl: String
) {

    companion object {
        private const val CLIENT_NAME = "repr-api"
    }

    @Bean
    fun reprClientTokenX(
        restClientBuilder: RestClient.Builder,
        authorizationHeaderInterceptorFactory: AuthorizationHeaderInterceptorFactory
    ): RestClient {
        log.info { "Konfigurerer ReprConsumer med base URL: $reprBaseUrl" }

        return restClientBuilder
            .baseUrl(reprBaseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .requestInterceptor(authorizationHeaderInterceptorFactory.authorizationInterceptor(CLIENT_NAME))
            .build()
    }
}
