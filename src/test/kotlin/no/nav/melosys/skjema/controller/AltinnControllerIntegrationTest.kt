package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.*
import no.nav.melosys.skjema.dto.OrganisasjonDto
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Integrasjonstest for AltinnController med MockOAuth2Server og WebTestClient.
 * Tester controller-laget med riktig autentisering og service-lag.
 */
class AltinnControllerIntegrationTest : ApiTestBase() {
    
    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @Autowired 
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var arbeidsgiverAltinnTilgangerConsumer: ArbeidsgiverAltinnTilgangerConsumer
    
    @Test
    fun `GET hentTilganger skal returnere liste over organisasjoner`() {
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)

        val orgnr1 = "123456789"
        val orgnr2 = "987654321"
        
        // Mock Consumer response
        val altinnTilgangerResponse = altinnTilgangerResponseMedDefaultVerdier().copy(
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = orgnr1,
                    navn = "Test Bedrift AS",
                    organisasjonsform = "AS"
                ),
                AltinnTilgang(
                    orgnr = orgnr2,
                    navn = "Annen Bedrift AS",
                    organisasjonsform = "AS"
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-ressurs" to setOf(orgnr1, orgnr2)
            )
        )
        
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger() } returns altinnTilgangerResponse
        
        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        webTestClient.get()
            .uri("/api/hentTilganger")
            .header("Authorization", "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<OrganisasjonDto>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                response.responseBody!!.map { it.orgnr }.shouldContainAll(orgnr1, orgnr2)
            }
    }
    
    @Test
    fun `GET hentTilganger skal returnere tom liste når ingen tilganger`() {
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        val response = altinnTilgangerResponseMedDefaultVerdier().copy(
            tilgangTilOrgNr = emptyMap(),
        )
        
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger() } returns response
        
        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        webTestClient.get()
            .uri("/api/hentTilganger")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<OrganisasjonDto>>()
            .consumeWith { response ->
                response.run {
                    response.responseBody.shouldNotBeNull()
                    response.responseBody!!.shouldHaveSize(0)
                }
            }
    }
    
    @Test
    @DisplayName("GET /api/harTilgang/{orgnr} skal returnere true når bruker har tilgang")
    fun `GET harTilgang skal returnere true når bruker har tilgang`() {
        // Clear any previous mocks on this bean
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        val orgnr = "123456789"
        val response = altinnTilgangerResponseMedDefaultVerdier().copy(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = orgnr,
                    navn = "Test AS", 
                    organisasjonsform = "AS"
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-ressurs" to setOf(orgnr)  // Use test-ressurs matching application-test.yml
            ),
            orgNrTilTilganger = emptyMap()
        )
        
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger() } returns response
        
        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        webTestClient.get()
            .uri("/api/harTilgang/$orgnr")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody<Boolean>()
            .isEqualTo(true)

    }
    
    @Test
    fun `GET harTilgang skal returnere false når bruker ikke har tilgang`() {
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        val orgnr = "987654321"
        val response = altinnTilgangerResponseMedDefaultVerdier().copy(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "123456789",
                    navn = "Annen AS",
                    organisasjonsform = "AS"
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-ressurs" to setOf("123456789")
            ),
            orgNrTilTilganger = emptyMap()
        )
        
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger() } returns response
        
        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        webTestClient.get()
            .uri("/api/harTilgang/$orgnr") 
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody<Boolean>()
            .isEqualTo(false)
    }
    
    @Test
    fun `GET harTilgang skal validere organisasjonsnummer format`() {
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "12345678901")
        )

        webTestClient.get()
            .uri("/api/harTilgang/ugyldig-orgnr")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest
    }
}