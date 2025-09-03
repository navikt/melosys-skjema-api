package no.nav.melosys.skjema.integrasjon.altinn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class ArbeidsgiverAltinnTilgangerConsumerTest : FunSpec({
    
    val mockWebClient = mockk<WebClient>()
    val mockRequestSpec = mockk<WebClient.RequestBodyUriSpec>()
    val mockRequestHeadersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
    val mockResponseSpec = mockk<WebClient.ResponseSpec>()
    
    val consumer = ArbeidsgiverAltinnTilgangerConsumer(mockWebClient, "test-fager")
    
    beforeTest {
        every { mockWebClient.post() } returns mockRequestSpec
        every { mockRequestSpec.uri(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.bodyValue(any()) } returns mockRequestHeadersSpec
        every { mockRequestHeadersSpec.retrieve() } returns mockResponseSpec
    }
    
    test("hentTilganger skal returnere organisasjoner med riktig rolle") {
        val response = AltinnTilgangerResponse(
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
        } returns Mono.just(response)
        
        val result = consumer.hentTilganger()
        
        result shouldHaveSize 1
        result[0].orgnr shouldBe "123456789"
        result[0].navn shouldBe "Test Org"
        result[0].organisasjonsform shouldBe "AS"
        
        verify { mockRequestSpec.uri("/altinn-tilganger") }
    }
    
    test("hentTilganger skal returnere tom liste når response har isError = true") {
        val response = AltinnTilgangerResponse(
            isError = true,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.just(response)
        
        val result = consumer.hentTilganger()
        
        result.shouldBeEmpty()
    }
    
    test("hentTilganger skal returnere tom liste ved WebClientResponseException") {
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.error(WebClientResponseException.create(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), 
            "Server Error", 
            HttpHeaders.EMPTY, 
            ByteArray(0), 
            null
        ))
        
        val result = consumer.hentTilganger()
        
        result.shouldBeEmpty()
    }
    
    test("harTilgang skal returnere true når bruker har tilgang") {
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "123456789",
                    navn = "Test Org",
                    organisasjonsform = "AS"
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-fager" to setOf("123456789")
            ),
            orgNrTilTilganger = mapOf()
        )
        
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.just(response)
        
        val result = consumer.harTilgang("123456789")
        
        result shouldBe true
    }
    
    test("harTilgang skal returnere false når bruker ikke har tilgang") {
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "987654321",
                    navn = "Annen Org",
                    organisasjonsform = "AS"
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-fager" to setOf("987654321")
            ),
            orgNrTilTilganger = mapOf()
        )
        
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.just(response)
        
        val result = consumer.harTilgang("123456789")
        
        result shouldBe false
    }
    
    test("skal finne organisasjoner i hierarki med underenheter") {
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "111111111",
                    navn = "Hovedorg",
                    organisasjonsform = "AS",
                    underenheter = listOf(
                        AltinnTilgang(
                            orgnr = "222222222",
                            navn = "Underenhet",
                            organisasjonsform = "BEDR"
                        )
                    )
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-fager" to setOf("222222222")
            ),
            orgNrTilTilganger = mapOf()
        )
        
        every { 
            mockResponseSpec.bodyToMono(AltinnTilgangerResponse::class.java) 
        } returns Mono.just(response)
        
        val result = consumer.hentTilganger()
        
        result shouldHaveSize 1
        result[0].orgnr shouldBe "222222222"
        result[0].navn shouldBe "Underenhet"
    }
})