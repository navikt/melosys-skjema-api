package no.nav.melosys.skjema.integrasjon.arbeidsgiver

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.BeskjedResult
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLError
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLRequest
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLResponse
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.NyBeskjedResponse
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.UUID

class ArbeidsgiverNotifikasjonConsumerTest : FunSpec({

    val mockWebClient = mockk<WebClient>()
    val mockRequestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
    val mockRequestBodySpec = mockk<WebClient.RequestBodySpec>()
    val mockResponseSpec = mockk<WebClient.ResponseSpec>()
    val merkelapp = "TestMerkelapp"
    
    val consumer = ArbeidsgiverNotifikasjonConsumer(mockWebClient, merkelapp)

    beforeEach {
        every { mockWebClient.post() } returns mockRequestBodyUriSpec
        every { mockRequestBodyUriSpec.uri(any<String>()) } returns mockRequestBodySpec
        every { mockRequestBodySpec.bodyValue(any()) } returns mockRequestBodySpec
        every { mockRequestBodySpec.retrieve() } returns mockResponseSpec
    }

    test("opprettBeskjed should create beskjed successfully and return id") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        val expectedId = "beskjed-id-123"
        val eksternId = "extern-id-123"
        
        val expectedResponse = GraphQLResponse(
            data = NyBeskjedResponse(
                nyBeskjed = BeskjedResult(
                    __typename = "NyBeskjedVellykket",
                    id = expectedId,
                    feilmelding = null
                )
            ),
            errors = null
        )
        
        val requestSlot = slot<GraphQLRequest>()
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.just(expectedResponse)
        
        val result = consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke, eksternId)
        
        result shouldBe expectedId
        
        verify { mockRequestBodySpec.bodyValue(capture(requestSlot)) }
        
        val capturedRequest = requestSlot.captured
        capturedRequest.query shouldContain "mutation OpprettNyBeskjed"
        capturedRequest.variables["eksternId"] shouldBe eksternId
        capturedRequest.variables["virksomhetsnummer"] shouldBe virksomhetsnummer
        capturedRequest.variables["tekst"] shouldBe tekst
        capturedRequest.variables["lenke"] shouldBe lenke
        capturedRequest.variables["merkelapp"] shouldBe merkelapp
    }

    test("opprettBeskjed should generate UUID when eksternId not provided") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        val expectedId = "beskjed-id-123"
        
        val expectedResponse = GraphQLResponse(
            data = NyBeskjedResponse(
                nyBeskjed = BeskjedResult(
                    __typename = "NyBeskjedVellykket",
                    id = expectedId,
                    feilmelding = null
                )
            ),
            errors = null
        )
        
        val requestSlot = slot<GraphQLRequest>()
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.just(expectedResponse)
        
        val result = consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke)
        
        result shouldBe expectedId
        
        verify { mockRequestBodySpec.bodyValue(capture(requestSlot)) }
        
        val capturedRequest = requestSlot.captured
        val generatedEksternId = capturedRequest.variables["eksternId"] as String
        
        // Verify it's a valid UUID format
        UUID.fromString(generatedEksternId) // This will throw if not valid UUID
    }

    test("opprettBeskjed should handle GraphQL errors") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        val errorResponse = GraphQLResponse<NyBeskjedResponse>(
            data = null,
            errors = listOf(
                GraphQLError("Invalid virksomhetsnummer", mapOf("code" to "VALIDATION_ERROR")),
                GraphQLError("Missing required field", mapOf("field" to "tekst"))
            )
        )
        
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.just(errorResponse)
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke)
        }
        
        exception.message shouldContain "GraphQL feil ved opprettelse av beskjed"
        exception.message shouldContain "Invalid virksomhetsnummer"
        exception.message shouldContain "Missing required field"
    }

    test("opprettBeskjed should handle business logic error response") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        val errorResponse = GraphQLResponse(
            data = NyBeskjedResponse(
                nyBeskjed = BeskjedResult(
                    __typename = "Error",
                    id = null,
                    feilmelding = "Virksomhet har ikke tilgang til denne ressursen"
                )
            ),
            errors = null
        )
        
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.just(errorResponse)
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke)
        }
        
        exception.message shouldBe "Feil ved opprettelse av beskjed: Virksomhet har ikke tilgang til denne ressursen"
    }

    test("opprettBeskjed should handle unknown response type") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        val unknownResponse = GraphQLResponse(
            data = NyBeskjedResponse(
                nyBeskjed = BeskjedResult(
                    __typename = "UnknownType",
                    id = null,
                    feilmelding = null
                )
            ),
            errors = null
        )
        
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.just(unknownResponse)
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke)
        }
        
        exception.message shouldBe "Ukjent respons type: UnknownType"
    }

    test("opprettBeskjed should handle missing id in successful response") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        val responseWithoutId = GraphQLResponse(
            data = NyBeskjedResponse(
                nyBeskjed = BeskjedResult(
                    __typename = "NyBeskjedVellykket",
                    id = null,
                    feilmelding = null
                )
            ),
            errors = null
        )
        
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.just(responseWithoutId)
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke)
        }
        
        exception.message shouldBe "Manglende id i vellykket respons"
    }

    test("opprettBeskjed should handle empty response") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.empty()
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke)
        }
        
        exception.message shouldBe "Ukjent respons type: null"
    }

    test("opprettBeskjed should handle WebClient exceptions") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        every { 
            mockResponseSpec.bodyToMono(any<ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>>()) 
        } returns Mono.error(WebClientResponseException.create(500, "Internal Server Error", HttpHeaders(), ByteArray(0), null))
        
        shouldThrow<WebClientResponseException> {
            consumer.opprettBeskjed(virksomhetsnummer, tekst, lenke)
        }
    }
})