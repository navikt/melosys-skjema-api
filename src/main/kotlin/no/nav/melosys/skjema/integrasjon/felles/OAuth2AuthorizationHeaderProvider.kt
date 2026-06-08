package no.nav.melosys.skjema.integrasjon.felles

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.melosys.skjema.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Component

@Component
class OAuth2AuthorizationHeaderProvider(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) {

    fun authorizationHeader(clientName: String): String {
        return if (ThreadLocalAccessInfo.shouldUseM2MToken()) {
            clientCredentialsAuthorizationHeader(clientName)
        } else {
            bearerToken(clientProperties(clientName))
        }
    }

    fun clientCredentialsAuthorizationHeader(clientName: String): String {
        return bearerToken(clientPropertiesForClientCredentials(clientName))
    }

    private fun clientProperties(clientName: String): ClientProperties {
        return clientConfigurationProperties.registration[clientName]
            ?: throw RuntimeException("Fant ikke OAuth2-config for $clientName")
    }

    private fun clientPropertiesForClientCredentials(clientName: String): ClientProperties {
        val clientProperties = clientProperties(clientName)
        return ClientProperties.builder(
            grantType = GrantType.CLIENT_CREDENTIALS,
            authentication = clientProperties.authentication
        )
            .tokenEndpointUrl(clientProperties.tokenEndpointUrl)
            .scope(clientProperties.scope)
            .build()
    }

    private fun bearerToken(clientProperties: ClientProperties): String {
        return "Bearer ${oAuth2AccessTokenService.getAccessToken(clientProperties).access_token}"
    }
}
