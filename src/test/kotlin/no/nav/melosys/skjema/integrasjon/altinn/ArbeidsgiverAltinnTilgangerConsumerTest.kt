package no.nav.melosys.skjema.integrasjon.altinn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.melosys.skjema.altinnTilgangerResponseMedDefaultVerdier
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerRequest
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import org.junit.jupiter.api.Assertions.*

class ArbeidsgiverAltinnTilgangerConsumerTest {
    
    private lateinit var mockWebServer: MockWebServer
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private lateinit var consumer: ArbeidsgiverAltinnTilgangerConsumer
    
    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build()
        consumer = ArbeidsgiverAltinnTilgangerConsumer(webClient)
    }
    
    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `hentTilganger skal returnere AltinnTilgangerResponse ved vellykket kall`() {
        val altinnTilgangerResponse = altinnTilgangerResponseMedDefaultVerdier().copy(
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "123456789",
                    navn = "Test Org",
                    organisasjonsform = "AS",
                    altinn3Tilganger = setOf("test-fager", "annen-rolle")
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-fager" to setOf("123456789", "987654321"),
                "annen-rolle" to setOf("123456789")
            )
        )
        
        // Set up MockWebServer response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(altinnTilgangerResponse))
        )
        
        val result = consumer.hentTilganger()
        
        assertEquals(altinnTilgangerResponse, result)
        assertEquals(false, result.isError)
        assertEquals(1, result.hierarki.size)
        assertEquals("123456789", result.hierarki[0].orgnr)
        
        // Verify the request was made correctly
        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/altinn-tilganger", recordedRequest.path)
        assertTrue(recordedRequest.getHeader("Content-Type")?.contains("application/json") == true)
    }
    
    @Test
    fun `hentTilganger skal returnere response med isError = true når API returnerer feil`() {
        val errorResponse = altinnTilgangerResponseMedDefaultVerdier().copy(isError = true)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(errorResponse))
        )
        
        val result = consumer.hentTilganger()
        
        assertEquals(errorResponse, result)
        assertEquals(true, result.isError)
    }
    
    @Test
    fun `hentTilganger skal kaste WebClientResponseException når API returnerer 500`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )
        
        assertThrows<WebClientResponseException> {
            consumer.hentTilganger()
        }
    }
    
    @Test
    fun `hentTilganger skal kalle riktig URI med riktig body`() {
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        val expectedRequestBody = AltinnTilgangerRequest(null)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(response))
        )
        
        val result = consumer.hentTilganger()
        
        assertNotNull(result)
        assertEquals(response, result)
        
        // Verify the exact request was made
        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/altinn-tilganger", recordedRequest.path)
        assertTrue(recordedRequest.getHeader("Content-Type")?.contains("application/json") == true)
        
        val requestBodyJson = recordedRequest.body.readUtf8()
        val actualRequestBody = objectMapper.readValue(requestBodyJson, AltinnTilgangerRequest::class.java)
        assertEquals(expectedRequestBody, actualRequestBody)
    }
    
    @Test
    fun `hentTilganger skal sende riktig filter i request body`() {
        val testFilter = null // Test med null filter (default)
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        val expectedRequestBody = AltinnTilgangerRequest(testFilter)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(response))
        )
        
        consumer.hentTilganger(testFilter)
        
        // Verify the request body contains the filter
        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/altinn-tilganger", recordedRequest.path)
        
        val requestBodyJson = recordedRequest.body.readUtf8()
        val actualRequestBody = objectMapper.readValue(requestBodyJson, AltinnTilgangerRequest::class.java)
        assertEquals(expectedRequestBody, actualRequestBody)
    }
}