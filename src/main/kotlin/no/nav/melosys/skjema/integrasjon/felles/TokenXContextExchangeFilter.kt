package no.nav.melosys.skjema.integrasjon.felles

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties

class TokenXContextExchangeFilter(
    clientConfigurationProperties: ClientConfigurationProperties,
    oAuth2AccessTokenService: OAuth2AccessTokenService,
    clientName: String) : GenericContextExchangeFilter(clientConfigurationProperties, oAuth2AccessTokenService, clientName) {

    private val clientPropertiesForM2M: ClientProperties = ClientProperties.builder(
        grantType = GrantType.CLIENT_CREDENTIALS,
        authentication = clientProperties.authentication
    )
        .tokenEndpointUrl(clientProperties.tokenEndpointUrl)
        .scope(clientProperties.scope)
        .build()

    override fun getM2MToken(): String {
        return "Bearer ${oAuth2AccessTokenService.getAccessToken(clientPropertiesForM2M).access_token}"
    }
}