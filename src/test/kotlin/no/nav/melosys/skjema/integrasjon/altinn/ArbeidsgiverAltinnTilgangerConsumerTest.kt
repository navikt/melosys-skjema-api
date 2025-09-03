package no.nav.melosys.skjema.integrasjon.altinn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class ArbeidsgiverAltinnTilgangerConsumerTest : FunSpec({
    
    val mockWebClient = mockk<WebClient>()
    val mockRequestSpec = mockk<WebClient.RequestBodyUriSpec>()
    val mockRequestHeadersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
    val mockResponseSpec = mockk<WebClient.ResponseSpec>()
    
    val consumer = ArbeidsgiverAltinnTilgangerConsumer(mockWebClient)
    
    beforeTest {
        every { mockWebClient.post() } returns mockRequestSpec
        every { mockRequestSpec.uri(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.bodyValue(any()) } returns mockRequestHeadersSpec
        every { mockRequestHeadersSpec.retrieve() } returns mockResponseSpec
    }
    
    test("hentTilganger skal returnere AltinnTilgangerResponse ved vellykket kall") {
        val expectedResponse = AltinnTilgangerResponse(
            isError = false,
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
            ),
            orgNrTilTilganger = mapOf()
        )
        
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.just(expectedResponse)
        
        val result = consumer.hentTilganger()
        
        result shouldBe expectedResponse
        result.isError shouldBe false
        result.hierarki.size shouldBe 1
        result.hierarki[0].orgnr shouldBe "123456789"
        
        verify { mockRequestSpec.uri("/altinn-tilganger") }
    }
    
    test("hentTilganger skal returnere response med isError = true når API returnerer feil") {
        val errorResponse = AltinnTilgangerResponse(
            isError = true,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.just(errorResponse)
        
        val result = consumer.hentTilganger()
        
        result shouldBe errorResponse
        result.isError shouldBe true
    }
    
    test("hentTilganger skal kaste RuntimeException når response er null") {
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.empty()
        
        shouldThrow<RuntimeException> {
            consumer.hentTilganger()
        }.message shouldBe "Fikk null response fra arbeidsgiver-altinn-tilganger"
    }
    
    test("hentTilganger skal kalle riktig URI med riktig body") {
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.just(response)
        
        val result = consumer.hentTilganger()
        
        result shouldNotBe null
        verify { mockRequestSpec.uri("/altinn-tilganger") }
        verify { mockRequestSpec.bodyValue(any()) }
    }
})