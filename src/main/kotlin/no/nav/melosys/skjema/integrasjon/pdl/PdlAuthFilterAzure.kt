package no.nav.melosys.skjema.integrasjon.pdl

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Component
class PdlAuthFilterAzure(
    clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) : ExchangeFilterFunction {

    companion object {
        private const val CLIENT_NAME = "pdl"
        private const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }

    private val clientProperties: ClientProperties =
        clientConfigurationProperties.registration[CLIENT_NAME]
            ?: throw RuntimeException("Fant ikke OAuth2-config for $CLIENT_NAME")

    private val clientPropertiesForM2M: ClientProperties = ClientProperties.builder(
        grantType = GrantType.CLIENT_CREDENTIALS,
        authentication = clientProperties.authentication
    )
        .tokenEndpointUrl(clientProperties.tokenEndpointUrl)
        .scope(clientProperties.scope)
        .build()

    /**
     * Skal alltid bruke M2M token (client credentials) og Nav-Consumer-Token header
     */
    override fun filter(clientRequest: ClientRequest, exchangeFunction: ExchangeFunction): Mono<ClientResponse> {
        val token = "Bearer ${oAuth2AccessTokenService.getAccessToken(clientPropertiesForM2M).access_token}"

        val builder = ClientRequest.from(clientRequest)
            .header("Authorization", token)
            .header(NAV_CONSUMER_TOKEN, token)

        return exchangeFunction.exchange(builder.build())
    }
}
