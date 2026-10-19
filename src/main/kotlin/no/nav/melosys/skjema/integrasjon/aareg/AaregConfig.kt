package no.nav.melosys.skjema.integrasjon.aareg

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
class AaregConfig(
    @param:Value("\${aareg.url}") private val aaregUrl: String
) {

    companion object {
        private const val CLIENT_NAME = "aareg"
    }

    @Bean
    fun aaregRestClient(
        restClientBuilder: RestClient.Builder,
        authorizationHeaderInterceptorFactory: AuthorizationHeaderInterceptorFactory
    ): RestClient {
        log.info { "Konfigurerer AAREG-klient med base URL: $aaregUrl" }

        return restClientBuilder
            .baseUrl(aaregUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .requestInterceptor(authorizationHeaderInterceptorFactory.clientCredentialsInterceptor(CLIENT_NAME))
            .build()
    }
}
