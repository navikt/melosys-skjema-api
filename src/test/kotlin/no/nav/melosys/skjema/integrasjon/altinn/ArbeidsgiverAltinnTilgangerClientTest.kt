package no.nav.melosys.skjema.integrasjon.altinn

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnFilter
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

class ArbeidsgiverAltinnTilgangerClientTest: ApiTestBase() {

    @Autowired
    private lateinit var arbeidsgiverAltinnTilgangerClient: ArbeidsgiverAltinnTilgangerClient

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var oAuth2AccessTokenService: OAuth2AccessTokenService

    
    @AfterEach
    fun teardown() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentTilganger skal utføre forventet http-request og deserialisere json respons`() {

        val expectedAccessToken = "expectedAccessToken"

        every {
            oAuth2AccessTokenService.getAccessToken(any())
        } returns OAuth2AccessTokenResponse(access_token = expectedAccessToken)

        val responseJson = """
            {
              "isError": false,
              "hierarki": [
                {
                  "orgnr": "123456789",
                  "navn": "Test Bedrift AS",
                  "organisasjonsform": "AS",
                  "type": "Enterprise"
                }
              ],
              "orgNrTilTilganger": {
                "123456789": ["4936"]
              },
              "tilgangTilOrgNr": {
                "4936": ["123456789"]
              },
              "unknownProperty": "should be ignored"
            }
        """.trimIndent()

        wireMockServer.stubFor(post(urlPathMatching(".*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseJson)
            )
        )

        val altinnTilgangerResponse = arbeidsgiverAltinnTilgangerClient.hentTilganger(
            AltinnFilter(
                inkluderSlettede = false,
                altinn2Tilganger = setOf("1234"),
                altinn3Tilganger = setOf("4936")
            )
        )

        altinnTilgangerResponse.run {
            this.shouldBeEqual(AltinnTilgangerResponse(
                isError = false,
                hierarki = listOf(
                    AltinnTilgang(
                        orgnr = "123456789",
                        navn = "Test Bedrift AS",
                        organisasjonsform = "AS"
                    )
                ),
                orgNrTilTilganger = mapOf("123456789" to setOf("4936")),
                tilgangTilOrgNr = mapOf("4936" to setOf("123456789"))
            ))
        }

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/altinn-tilganger"))
            .withHeader("Authorization", equalTo("Bearer $expectedAccessToken"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                  "filter": {
                    "inkluderSlettede": false,
                    "altinn2Tilganger": ["1234"],
                    "altinn3Tilganger": ["4936"]
                  }
                }
            """.trimIndent()))
        )
    }

    @Test
    fun `hentTilganger skal dekomprimere deflate-komprimert respons`() {
        // Apache HttpClient 5 må pakke ut rå deflate (RFC 1951); JDK-klienten feiler med ZipException.
        every {
            oAuth2AccessTokenService.getAccessToken(any())
        } returns OAuth2AccessTokenResponse(access_token = "token")

        wireMockServer.stubFor(post(urlPathMatching(".*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Content-Encoding", "deflate")
                    .withBody(raaDeflate("""{"tilgangTilOrgNr":{"4936":["123456789"]}}"""))
            )
        )

        val respons = arbeidsgiverAltinnTilgangerClient.hentTilganger()

        respons.tilgangTilOrgNr shouldBeEqual mapOf("4936" to setOf("123456789"))
    }

    /** Komprimerer med rå DEFLATE (nowrap = true, RFC 1951) — uten zlib-header, slik enkelte tjenester svarer. */
    private fun raaDeflate(data: String): ByteArray {
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        deflater.setInput(data.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            output.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return output.toByteArray()
    }
}