package no.nav.melosys.skjema.integrasjon.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import java.util.concurrent.atomic.AtomicInteger
import no.nav.melosys.skjema.ApiTestBase
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import reactor.core.scheduler.Schedulers

class PdlConsumerTest : ApiTestBase() {

    @Autowired
    private lateinit var pdlConsumer: PdlConsumer

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var oAuth2AccessTokenService: OAuth2AccessTokenService

    @AfterEach
    fun teardown() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentPerson retryer PDL-kall uten å hente token på non-blocking tråd`() {
        val expectedAccessToken = "pdl-token"
        val tokenCalls = AtomicInteger(0)

        every { oAuth2AccessTokenService.getAccessToken(any()) } answers {
            if (Schedulers.isInNonBlockingThread()) {
                throw AssertionError("Token skal ikke hentes på Reactor non-blocking tråd")
            }
            tokenCalls.incrementAndGet()
            OAuth2AccessTokenResponse(access_token = expectedAccessToken)
        }

        wireMockServer.stubFor(
            post(urlPathEqualTo("/"))
                .inScenario("pdl-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("andre-forsok")
        )
        wireMockServer.stubFor(
            post(urlPathEqualTo("/"))
                .inScenario("pdl-retry")
                .whenScenarioStateIs("andre-forsok")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "data": {
                                "hentPerson": {
                                  "navn": [
                                    {
                                      "fornavn": "Ola",
                                      "mellomnavn": null,
                                      "etternavn": "Nordmann"
                                    }
                                  ],
                                  "foedselsdato": [
                                    {
                                      "foedselsdato": "1990-01-01"
                                    }
                                  ]
                                }
                              },
                              "errors": null
                            }
                            """.trimIndent()
                        )
                )
        )

        val person = pdlConsumer.hentPerson("12345678901")

        person.hentFulltNavn() shouldBe "Ola Nordmann"
        wireMockServer.verify(
            2,
            postRequestedFor(urlPathEqualTo("/"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $expectedAccessToken"))
                .withHeader("Nav-Consumer-Token", equalTo("Bearer $expectedAccessToken"))
        )
        tokenCalls.get() shouldBe 1
        verify(exactly = 1) { oAuth2AccessTokenService.getAccessToken(any()) }
    }
}
