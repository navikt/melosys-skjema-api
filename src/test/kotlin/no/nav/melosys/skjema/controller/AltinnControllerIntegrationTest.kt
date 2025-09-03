package no.nav.melosys.skjema.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.skjema.*
import no.nav.melosys.skjema.dto.OrganisasjonDto
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
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
    
    @Autowired
    private lateinit var arbeidsgiverAltinnTilgangerConsumer: ArbeidsgiverAltinnTilgangerConsumer
    
    @Test
    fun `GET hentTilganger skal returnere liste over organisasjoner`() {
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        // Mock Consumer response
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "123456789",
                    navn = "Test Bedrift AS",
                    organisasjonsform = "AS"
                ),
                AltinnTilgang(
                    orgnr = "987654321", 
                    navn = "Annen Bedrift AS",
                    organisasjonsform = "AS"
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-ressurs" to setOf("123456789", "987654321")
            ),
            orgNrTilTilganger = emptyMap()
        )
        
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger() } returns response
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger(null) } returns response
        
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
                val organisasjoner = response.responseBody!!
                assert(organisasjoner.size == 2)
                assert(organisasjoner[0].orgnr == "123456789")
                assert(organisasjoner[0].navn == "Test Bedrift AS")
                assert(organisasjoner[1].orgnr == "987654321")
                assert(organisasjoner[1].navn == "Annen Bedrift AS")
            }
    }
    
    @Test
    fun `GET hentTilganger skal returnere tom liste når ingen tilganger`() {
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger() } returns response
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger(null) } returns response
        
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
                val organisasjoner = response.responseBody!!
                assert(organisasjoner.isEmpty())
            }
    }
    
    @Test
    fun `GET harTilgang skal returnere true når bruker har tilgang`() {
        // Clear any previous mocks on this bean
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        val orgnr = "123456789"
        val response = AltinnTilgangerResponse(
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
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger(null) } returns response
        
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
            
        // Verify the mock was called
        verify { arbeidsgiverAltinnTilgangerConsumer.hentTilganger() }
    }
    
    @Test
    fun `GET harTilgang skal returnere false når bruker ikke har tilgang`() {
        clearMocks(arbeidsgiverAltinnTilgangerConsumer)
        
        val orgnr = "987654321"
        val response = AltinnTilgangerResponse(
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
        every { arbeidsgiverAltinnTilgangerConsumer.hentTilganger(null) } returns response
        
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
    fun `GET api endepunkter skal kreve autentisering`() {
        webTestClient.get()
            .uri("/api/hentTilganger")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized
            
        webTestClient.get()
            .uri("/api/harTilgang/123456789")
            .accept(MediaType.APPLICATION_JSON) 
            .exchange()
            .expectStatus().isUnauthorized
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