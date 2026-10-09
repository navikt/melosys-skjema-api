package no.nav.melosys.skjema.integrasjon.aareg

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import java.time.LocalDate
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.integrasjon.aareg.dto.Ansettelsesperiode
import no.nav.melosys.skjema.integrasjon.aareg.dto.Arbeidsforhold
import no.nav.melosys.skjema.integrasjon.aareg.dto.Arbeidsgiver
import no.nav.melosys.skjema.integrasjon.aareg.dto.Periode
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException

class AaregClientTest : ApiTestBase() {

    @Autowired
    private lateinit var aaregClient: AaregClient

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @MockkBean
    private lateinit var oAuth2AccessTokenService: OAuth2AccessTokenService

    @AfterEach
    fun teardown() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentOrdinaereArbeidsforhold sender forventet request og deserialiserer arbeidsforhold`() {
        every { oAuth2AccessTokenService.getAccessToken(any()) } returns OAuth2AccessTokenResponse(access_token = "aareg-token")
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/arbeidstaker/arbeidsforhold"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            [{
                              "navArbeidsforholdId": 123,
                              "type": "ordinaertArbeidsforhold",
                              "arbeidsgiver": { "type": "Organisasjon", "organisasjonsnummer": "910000001" },
                              "opplysningspliktig": { "type": "Organisasjon", "organisasjonsnummer": "910000000" },
                              "ansettelsesperiode": { "periode": { "fom": "2020-01-01" } },
                              "arbeidstaker": { "offentligIdent": "12345678910" }
                            }]
                            """.trimIndent()
                        )
                )
        )

        val arbeidsforhold = aaregClient.hentOrdinaereArbeidsforhold("12345678910")

        arbeidsforhold shouldBe listOf(
            Arbeidsforhold(
                type = "ordinaertArbeidsforhold",
                arbeidsgiver = Arbeidsgiver(type = "Organisasjon", organisasjonsnummer = "910000001"),
                opplysningspliktig = Arbeidsgiver(type = "Organisasjon", organisasjonsnummer = "910000000"),
                ansettelsesperiode = Ansettelsesperiode(Periode(fom = LocalDate.of(2020, 1, 1)))
            )
        )
        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/api/v1/arbeidstaker/arbeidsforhold"))
                .withQueryParam("regelverk", equalTo("ALLE"))
                .withQueryParam("arbeidsforholdType", equalTo("ordinaertArbeidsforhold"))
                .withHeader("Nav-Personident", equalTo("12345678910"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer aareg-token"))
        )
    }

    @Test
    fun `hentOrdinaereArbeidsforhold returnerer tom liste når arbeidstakeren ikke har arbeidsforhold`() {
        every { oAuth2AccessTokenService.getAccessToken(any()) } returns OAuth2AccessTokenResponse(access_token = "aareg-token")
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/arbeidstaker/arbeidsforhold"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("[]"))
        )

        aaregClient.hentOrdinaereArbeidsforhold("12345678910") shouldHaveSize 0
    }

    @Test
    fun `hentOrdinaereArbeidsforhold kaster feil når AAREG avviser kallet`() {
        every { oAuth2AccessTokenService.getAccessToken(any()) } returns OAuth2AccessTokenResponse(access_token = "aareg-token")
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/arbeidstaker/arbeidsforhold"))
                .willReturn(aResponse().withStatus(403))
        )

        assertThrows<HttpClientErrorException.Forbidden> {
            aaregClient.hentOrdinaereArbeidsforhold("12345678910")
        }
    }
}
