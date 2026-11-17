package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import java.time.LocalDate
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.controller.dto.VerifiserPersonRequest
import no.nav.melosys.skjema.controller.dto.VerifiserPersonResponse
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.integrasjon.aareg.AaregService
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.integrasjon.pdl.exception.PersonVerifiseringException
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.types.felles.SimpleOrganisasjonDto
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Integrasjonstest for ArbeidstakerController med MockOAuth2Server og WebTestClient.
 * Tester /api/arbeidstaker endepunktet.
 */
class ArbeidstakerControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var pdlService: PdlService

    @MockkBean
    private lateinit var aaregService: AaregService

    @MockkBean
    private lateinit var eregService: EregService

    @Test
    @DisplayName("GET /api/arbeidstaker/arbeidsgivere-i-aareg skal returnere arbeidsgivere med navn for innlogget bruker")
    fun `GET arbeidsgivere-i-aareg skal returnere arbeidsgivere med navn for innlogget bruker`() {
        clearMocks(aaregService, eregService)

        every { aaregService.hentOpplysningspliktigeForAktiveArbeidsforhold("98765432109") } returns listOf("910000000", "920000000")
        every { eregService.hentOrganisasjon("910000000") } returns SimpleOrganisasjonDto("910000000", "Nordlys Bygg AS")
        every { eregService.hentOrganisasjon("920000000") } returns SimpleOrganisasjonDto("920000000", "Havbris Rådgivning AS")

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.get()
            .uri("/api/arbeidstaker/arbeidsgivere-i-aareg")
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody<List<SimpleOrganisasjonDto>>()
            .isEqualTo(
                listOf(
                    SimpleOrganisasjonDto("910000000", "Nordlys Bygg AS"),
                    SimpleOrganisasjonDto("920000000", "Havbris Rådgivning AS")
                )
            )
    }

    @Test
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere person-data når verifisert")
    fun `POST verifiser-person skal returnere person-data når verifisert`() {
        clearMocks(pdlService)

        val request = VerifiserPersonRequest(
            fodselsnummer = korrektSyntetiskFnr,
            etternavn = "Nordmann"
        )

        every {
            pdlService.verifiserOgHentPerson(korrektSyntetiskFnr, "Nordmann")
        } returns Pair("Ola Nordmann", LocalDate.of(1990, 1, 1))

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.post()
            .uri("/api/arbeidstaker/verifiser-person")
            .header("Authorization", "Bearer $accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<VerifiserPersonResponse>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                response.responseBody!!.navn shouldBe "Ola Nordmann"
                response.responseBody!!.fodselsdato shouldBe LocalDate.of(1990, 1, 1)
            }
    }

    @Test
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere 400 når person ikke matcher")
    fun `POST verifiser-person skal returnere 400 når person ikke matcher`() {
        clearMocks(pdlService)

        val request = VerifiserPersonRequest(
            fodselsnummer = korrektSyntetiskFnr,
            etternavn = "FeilEtternavn"
        )

        every {
            pdlService.verifiserOgHentPerson(korrektSyntetiskFnr, "FeilEtternavn")
        } throws PersonVerifiseringException("Fødselsnummer og etternavn matcher ikke")

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.post()
            .uri("/api/arbeidstaker/verifiser-person")
            .header("Authorization", "Bearer $accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere 400 ved tomt etternavn")
    fun `POST verifiser-person skal returnere 400 ved tomt etternavn`() {
        val request = VerifiserPersonRequest(
            fodselsnummer = korrektSyntetiskFnr,
            etternavn = ""
        )

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.post()
            .uri("/api/arbeidstaker/verifiser-person")
            .header("Authorization", "Bearer $accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere 401 uten autentisering")
    fun `POST verifiser-person skal returnere 401 uten autentisering`() {
        val request = VerifiserPersonRequest(
            fodselsnummer = korrektSyntetiskFnr,
            etternavn = "Nordmann"
        )

        webTestClient.post()
            .uri("/api/arbeidstaker/verifiser-person")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere 429 ved rate limit overskredet")
    fun `POST verifiser-person skal returnere 429 ved rate limit overskredet`() {
        clearMocks(pdlService)

        val request = VerifiserPersonRequest(
            fodselsnummer = korrektSyntetiskFnr,
            etternavn = "Nordmann"
        )

        every {
            pdlService.verifiserOgHentPerson(korrektSyntetiskFnr, "Nordmann")
        } returns Pair("Ola Nordmann", LocalDate.of(1990, 1, 1))

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        // Send 5 requests (limit er 5 per minutt)
        repeat(5) {
            webTestClient.post()
                .uri("/api/arbeidstaker/verifiser-person")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk
        }

        // Den 6. requesten skal feile med 429
        webTestClient.post()
            .uri("/api/arbeidstaker/verifiser-person")
            .header("Authorization", "Bearer $accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(429)
            .expectHeader().exists("Retry-After")
    }
}
