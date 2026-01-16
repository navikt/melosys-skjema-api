package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.controller.dto.VerifiserPersonRequest
import no.nav.melosys.skjema.controller.dto.VerifiserPersonResponse
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.integrasjon.pdl.exception.PersonVerifiseringException
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.LocalDate

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

    @Test
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere person-data når verifisert")
    fun `POST verifiser-person skal returnere person-data når verifisert`() {
        clearMocks(pdlService)

        val request = VerifiserPersonRequest(
            fodselsnummer = korrektSyntetiskFnr,
            navn = "Nordmann"
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
            navn = "FeilEtternavn"
        )

        every {
            pdlService.verifiserOgHentPerson(korrektSyntetiskFnr, "FeilEtternavn")
        } throws PersonVerifiseringException("Fødselsnummer og navn matcher ikke")

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
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere 400 ved ugyldig fnr-format")
    fun `POST verifiser-person skal returnere 400 ved ugyldig fnr-format`() {
        val request = VerifiserPersonRequest(
            fodselsnummer = "123", // Ugyldig format
            navn = "Nordmann"
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
    @DisplayName("POST /api/arbeidstaker/verifiser-person skal returnere 400 ved tomt navn")
    fun `POST verifiser-person skal returnere 400 ved tomt navn`() {
        val request = VerifiserPersonRequest(
            fodselsnummer = korrektSyntetiskFnr,
            navn = ""
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
            navn = "Nordmann"
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
            navn = "Nordmann"
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
