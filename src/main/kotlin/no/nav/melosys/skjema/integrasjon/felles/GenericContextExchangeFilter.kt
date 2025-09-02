package no.nav.melosys.skjema.integrasjon.felles

import no.nav.melosys.skjema.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

abstract class GenericContextExchangeFilter(
    protected val clientConfigurationProperties: ClientConfigurationProperties,
    protected val oAuth2AccessTokenService: OAuth2AccessTokenService,
    protected val clientName: String
) : ExchangeFilterFunction {
    
    protected val clientProperties: ClientProperties = 
        clientConfigurationProperties.registration[clientName]
            ?: throw RuntimeException("Fant ikke OAuth2-config for $clientName")
    
    override fun filter(
        clientRequest: ClientRequest,
        exchangeFunction: ExchangeFunction
    ): Mono<ClientResponse> {
        return exchangeFunction.exchange(
            withClientRequestBuilder(ClientRequest.from(clientRequest)).build()
        )
    }
    
    protected fun withClientRequestBuilder(clientRequestBuilder: ClientRequest.Builder): ClientRequest.Builder {
        return clientRequestBuilder.header(HttpHeaders.AUTHORIZATION, getCorrectToken())
    }
    
    protected fun getCorrectToken(): String {
        return if (ThreadLocalAccessInfo.shouldUseM2MToken()) {
            getM2MToken()
        } else {
            getUserToken()
        }
    }
    
    protected abstract fun getM2MToken(): String
    
    protected open fun getUserToken(): String {
        return "Bearer ${oAuth2AccessTokenService.getAccessToken(clientProperties).access_token}"
    }
}