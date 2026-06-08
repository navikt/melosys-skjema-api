package no.nav.melosys.skjema.integrasjon.felles

import com.nimbusds.oauth2.sdk.GrantType
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class OAuth2AuthorizationHeaderProviderTest : ApiTestBase() {

    @Autowired
    private lateinit var authorizationHeaderProvider: OAuth2AuthorizationHeaderProvider

    @Autowired
    private lateinit var clientConfigurationProperties: ClientConfigurationProperties

    @MockkBean
    private lateinit var oAuth2AccessTokenService: OAuth2AccessTokenService

    @Test
    fun `clientCredentialsAuthorizationHeader bruker client credentials`() {
        val clientPropertiesSlot = slot<ClientProperties>()
        every { oAuth2AccessTokenService.getAccessToken(capture(clientPropertiesSlot)) } returns
            OAuth2AccessTokenResponse(access_token = "m2m-token")

        val authorizationHeader = authorizationHeaderProvider.clientCredentialsAuthorizationHeader("pdl")

        authorizationHeader shouldBe "Bearer m2m-token"
        clientPropertiesSlot.captured.grantType shouldBe GrantType.CLIENT_CREDENTIALS
    }

    @Test
    fun `authorizationHeader bruker registrert client config for vanlig web-request`() {
        val clientPropertiesSlot = slot<ClientProperties>()
        every { oAuth2AccessTokenService.getAccessToken(capture(clientPropertiesSlot)) } returns
            OAuth2AccessTokenResponse(access_token = "obo-token")

        withControllerRequest("/api/test") {
            val authorizationHeader = authorizationHeaderProvider.authorizationHeader("repr-api")

            authorizationHeader shouldBe "Bearer obo-token"
            clientPropertiesSlot.captured.grantType shouldBe
                clientConfigurationProperties.registration["repr-api"]!!.grantType
        }
    }

    @Test
    fun `authorizationHeader bruker client credentials for admin-request`() {
        val clientPropertiesSlot = slot<ClientProperties>()
        every { oAuth2AccessTokenService.getAccessToken(capture(clientPropertiesSlot)) } returns
            OAuth2AccessTokenResponse(access_token = "m2m-token")

        withControllerRequest("/api/admin", isAdminRequest = true) {
            val authorizationHeader = authorizationHeaderProvider.authorizationHeader("repr-api")

            authorizationHeader shouldBe "Bearer m2m-token"
            clientPropertiesSlot.captured.grantType shouldBe GrantType.CLIENT_CREDENTIALS
        }
    }

    @Test
    fun `authorizationHeader feiler tydelig ved manglende client config`() {
        val exception = assertThrows<RuntimeException> {
            authorizationHeaderProvider.authorizationHeader("mangler")
        }

        exception.message shouldBe "Fant ikke OAuth2-config for mangler"
    }

    private fun withControllerRequest(
        requestUri: String,
        isAdminRequest: Boolean = false,
        block: () -> Unit
    ) {
        ThreadLocalAccessInfo.beforeControllerRequest(requestUri, isAdminRequest)
        try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterControllerRequest(requestUri)
        }
    }
}
