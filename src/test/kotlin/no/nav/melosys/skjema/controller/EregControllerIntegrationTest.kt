package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.inngaarIJuridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.ereg.dto.OrganisasjonMedJuridiskEnhet
import no.nav.melosys.skjema.juridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.service.RateLimitOperationType
import no.nav.melosys.skjema.service.RateLimiterService
import no.nav.melosys.skjema.virksomhetMedDefaultVerdier
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Integrasjonstest for EregController med MockOAuth2Server og WebTestClient.
 * Tester controller-laget med riktig autentisering og service-lag.
 */
class EregControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var eregService: EregService

    @MockkBean
    private lateinit var rateLimiterService: RateLimiterService

    @Test
    fun `GET organisasjon skal returnere organisasjon med juridisk enhet`() {
        clearMocks(eregService, rateLimiterService)

        val juridiskEnhet = juridiskEnhetMedDefaultVerdier()
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(
                    organisasjonsnummer = juridiskEnhet.organisasjonsnummer
                )
            )
        )

        val expected = OrganisasjonMedJuridiskEnhet(
            organisasjon = virksomhet,
            juridiskEnhet = juridiskEnhet
        )

        every { rateLimiterService.isRateLimited(any(), any()) } returns false
        every { eregService.hentOrganisasjonMedJuridiskEnhet(virksomhet.organisasjonsnummer) } returns expected

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        webTestClient.get()
            .uri("/api/ereg/organisasjon/${virksomhet.organisasjonsnummer}")
            .header("Authorization", "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<OrganisasjonMedJuridiskEnhet>()
            .consumeWith { response ->
                response.responseBody shouldBe expected
            }
    }

    @Test
    fun `GET organisasjon skal returnere 429 når rate limit er overskredet`() {
        clearMocks(eregService, rateLimiterService)

        val orgnr = "889640782"

        every { rateLimiterService.isRateLimited(any(), RateLimitOperationType.ORGANISASJONSSOK) } returns true

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        webTestClient.get()
            .uri("/api/ereg/organisasjon/$orgnr")
            .header("Authorization", "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(429)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
    }
}
