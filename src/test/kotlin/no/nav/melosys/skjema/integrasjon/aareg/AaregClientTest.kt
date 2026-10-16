package no.nav.melosys.skjema.integrasjon.aareg

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
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
import no.nav.melosys.skjema.integrasjon.aareg.dto.Enhet
import no.nav.melosys.skjema.integrasjon.aareg.dto.Ident
import no.nav.melosys.skjema.integrasjon.aareg.dto.Kodeverksentitet
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
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            [{
                              "id": "V910000001R1L0001",
                              "type": { "kode": "ordinaertArbeidsforhold", "beskrivelse": "Ordinært arbeidsforhold" },
                              "arbeidstaker": { "identer": [{ "type": "FOLKEREGISTERIDENT", "ident": "12345678910", "gjeldende": true }] },
                              "arbeidssted": { "type": "Underenhet", "identer": [{ "type": "ORGANISASJONSNUMMER", "ident": "910000001" }] },
                              "opplysningspliktig": { "type": "Hovedenhet", "identer": [{ "type": "ORGANISASJONSNUMMER", "ident": "910000000" }] },
                              "ansettelsesperiode": { "startdato": "2020-01-01" }
                            }]
                            """.trimIndent()
                        )
                )
        )

        val arbeidsforhold = aaregClient.hentOrdinaereArbeidsforhold("12345678910")

        arbeidsforhold shouldBe listOf(
            Arbeidsforhold(
                type = Kodeverksentitet(kode = "ordinaertArbeidsforhold"),
                arbeidssted = Enhet(type = "Underenhet", identer = listOf(Ident(type = "ORGANISASJONSNUMMER", ident = "910000001"))),
                opplysningspliktig = Enhet(type = "Hovedenhet", identer = listOf(Ident(type = "ORGANISASJONSNUMMER", ident = "910000000"))),
                ansettelsesperiode = Ansettelsesperiode(startdato = LocalDate.of(2020, 1, 1))
            )
        )
        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/api/v2/arbeidstaker/arbeidsforhold"))
                .withQueryParam("arbeidsforholdtype", equalTo("ordinaertArbeidsforhold"))
                .withQueryParam("arbeidsforholdstatus", equalTo("AKTIV,AVSLUTTET,FREMTIDIG"))
                .withHeader("Nav-Personident", equalTo("12345678910"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer aareg-token"))
        )
    }

    @Test
    fun `hentOrdinaereArbeidsforhold returnerer tom liste når arbeidstakeren ikke har arbeidsforhold`() {
        every { oAuth2AccessTokenService.getAccessToken(any()) } returns OAuth2AccessTokenResponse(access_token = "aareg-token")
        wireMockServer.stubFor(
            any(anyUrl())
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("[]"))
        )

        aaregClient.hentOrdinaereArbeidsforhold("12345678910") shouldHaveSize 0
    }

    @Test
    fun `hentOrdinaereArbeidsforhold kaster feil når AAREG avviser kallet`() {
        every { oAuth2AccessTokenService.getAccessToken(any()) } returns OAuth2AccessTokenResponse(access_token = "aareg-token")
        wireMockServer.stubFor(
            any(anyUrl())
                .willReturn(aResponse().withStatus(403))
        )

        assertThrows<HttpClientErrorException.Forbidden> {
            aaregClient.hentOrdinaereArbeidsforhold("12345678910")
        }
    }
}
