package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.TokenXContextExchangeFilter
import no.nav.melosys.skjema.integrasjon.felles.WebClientConfig
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }

@Configuration
class ArbeidsgiverAltinnTilgangerClientProducer(
    @param:Value("\${arbeidsgiver.altinn.tilganger.url}") private val arbeidsgiverAltinnTilgangerBaseUrl: String,
) {

    companion object {
        private const val CLIENT_NAME = "arbeidsgiver-altinn-tilganger"
    }

    @Bean
    fun arbeidsgiverAltinnTilgangerClient(
        webClientBuilder: WebClient.Builder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): WebClient {
        log.info { "Konfigurerer ArbeidsgiverAltinnTilgangerConsumer med base URL: $arbeidsgiverAltinnTilgangerBaseUrl" }

        val tokenXContextExchangeFilter = TokenXContextExchangeFilter(
            clientConfigurationProperties,
            oAuth2AccessTokenService,
            CLIENT_NAME
        )

        return webClientBuilder
            .baseUrl(arbeidsgiverAltinnTilgangerBaseUrl)
            .filter(tokenXContextExchangeFilter)
            .filter(WebClientConfig.errorFilter("Kall mot arbeidsgiver-altinn-tilganger feilet"))
            .filter(headerFilter())
            .build()
    }

    private fun headerFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            Mono.just(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()
            )
        }
    }
}