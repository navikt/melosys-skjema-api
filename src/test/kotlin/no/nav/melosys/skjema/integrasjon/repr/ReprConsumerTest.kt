package no.nav.melosys.skjema.integrasjon.repr

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.fullmaktMedDefaultVerdier
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ReprConsumerTest : ApiTestBase() {

    @Autowired
    private lateinit var reprConsumer: ReprConsumer

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var oAuth2AccessTokenService: OAuth2AccessTokenService

    @AfterEach
    fun teardown() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentKanRepresentere skal utføre forventet HTTP-request og deserialisere JSON respons`() {
        val expectedAccessToken = "test-tokenx-token"

        every {
            oAuth2AccessTokenService.getAccessToken(any())
        } returns OAuth2AccessTokenResponse(access_token = expectedAccessToken)

        val responseJson = """
            [
              {
                "fullmaktsgiver": "12345678901",
                "fullmektig": "98765432109",
                "leserettigheter": ["MED", "DAG"],
                "skriverettigheter": ["MED"],
                "unknownField": "should be ignored"
              },
              {
                "fullmaktsgiver": "11111111111",
                "fullmektig": "98765432109",
                "leserettigheter": ["DAG", "FOS"],
                "skriverettigheter": ["DAG"]
              }
            ]
        """.trimIndent()

        wireMockServer.stubFor(
            get(urlPathMatching(".*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)
                )
        )

        val fullmakter = reprConsumer.hentKanRepresentere()

        fullmakter.shouldHaveSize(2)
        fullmakter[0].shouldBeEqual(
            fullmaktMedDefaultVerdier().copy(
                leserettigheter = listOf("MED", "DAG")
            )
        )
        fullmakter[1].shouldBeEqual(
            fullmaktMedDefaultVerdier().copy(
                fullmaktsgiver = "11111111111",
                leserettigheter = listOf("DAG", "FOS"),
                skriverettigheter = listOf("DAG")
            )
        )

        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/api/v2/eksternbruker/fullmakt/kan-representere"))
                .withHeader("Authorization", equalTo("Bearer $expectedAccessToken"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json"))
        )
    }

    @Test
    fun `hentKanRepresentere skal håndtere tom liste`() {
        val expectedAccessToken = "test-tokenx-token"

        every {
            oAuth2AccessTokenService.getAccessToken(any())
        } returns OAuth2AccessTokenResponse(access_token = expectedAccessToken)

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/eksternbruker/fullmakt/kan-representere"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )

        val fullmakter = reprConsumer.hentKanRepresentere()

        fullmakter.shouldBeEmpty()
    }

    @Test
    fun `hentKanRepresentere skal kaste exception ved 500 Internal Server Error`() {
        val expectedAccessToken = "test-tokenx-token"

        every {
            oAuth2AccessTokenService.getAccessToken(any())
        } returns OAuth2AccessTokenResponse(access_token = expectedAccessToken)

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/eksternbruker/fullmakt/kan-representere"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Internal Server Error"}""")
                )
        )

        assertThrows<RuntimeException> {
            reprConsumer.hentKanRepresentere()
        }
    }

    @Test
    fun `hentKanRepresentere skal kaste exception ved 401 Unauthorized`() {
        val expectedAccessToken = "test-tokenx-token"

        every {
            oAuth2AccessTokenService.getAccessToken(any())
        } returns OAuth2AccessTokenResponse(access_token = expectedAccessToken)

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/eksternbruker/fullmakt/kan-representere"))
                .willReturn(
                    aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Unauthorized"}""")
                )
        )

        assertThrows<RuntimeException> {
            reprConsumer.hentKanRepresentere()
        }
    }

    @Test
    fun `hentKanRepresentere skal kaste exception ved 403 Forbidden`() {
        val expectedAccessToken = "test-tokenx-token"

        every {
            oAuth2AccessTokenService.getAccessToken(any())
        } returns OAuth2AccessTokenResponse(access_token = expectedAccessToken)

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/eksternbruker/fullmakt/kan-representere"))
                .willReturn(
                    aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "type": "about:blank",
                              "title": "Forbidden",
                              "status": 403,
                              "detail": "Not permitted by ABAC"
                            }
                        """.trimIndent())
                )
        )

        assertThrows<RuntimeException> {
            reprConsumer.hentKanRepresentere()
        }
    }
}
