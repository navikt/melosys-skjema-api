package no.nav.melosys.skjema.integrasjon.arbeidsgiver

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.BeskjedResult
import org.springframework.web.reactive.function.client.WebClient

class ArbeidsgiverNotifikasjonConsumerTest : FunSpec({

    val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    val merkelapp = "TestMerkelapp"
    val ressursId = "test-ressurs"
    
    lateinit var consumer: ArbeidsgiverNotifikasjonConsumer

    beforeEach {
        wireMockServer.start()
        val webClient = WebClient.builder()
            .baseUrl("http://localhost:${wireMockServer.port()}")
            .build()
        consumer = ArbeidsgiverNotifikasjonConsumer(webClient, merkelapp, ressursId)
    }

    afterEach {
        wireMockServer.stop()
    }

    test("opprettBeskjed should create beskjed successfully and return id") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        val expectedId = "beskjed-id-123"
        val eksternId = "extern-id-123"
        
        wireMockServer.stubFor(
            post(urlEqualTo("/api/graphql"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "data": {
                                    "nyBeskjed": {
                                        "__typename": "NyBeskjedVellykket",
                                        "id": "$expectedId"
                                    }
                                }
                            }
                            """.trimIndent()
                        )
                )
        )
        
        val result = consumer.opprettBeskjed(
            BeskjedRequest(
                virksomhetsnummer = virksomhetsnummer,
                tekst = tekst,
                lenke = lenke,
                eksternId = eksternId,
            )
        )
        
        result shouldBe expectedId
        
        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/api/graphql"))
                .withRequestBody(matchingJsonPath("$.variables.eksternId", equalTo(eksternId)))
                .withRequestBody(matchingJsonPath("$.variables.virksomhetsnummer", equalTo(virksomhetsnummer)))
                .withRequestBody(matchingJsonPath("$.variables.tekst", equalTo(tekst)))
                .withRequestBody(matchingJsonPath("$.variables.lenke", equalTo(lenke)))
                .withRequestBody(matchingJsonPath("$.variables.merkelapp", equalTo(merkelapp)))
                .withRequestBody(matchingJsonPath("$.variables.ressursId", equalTo("test-ressurs")))
        )
    }

    test("opprettBeskjed should handle GraphQL errors") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        wireMockServer.stubFor(
            post(urlEqualTo("/api/graphql"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "data": null,
                                "errors": [
                                    {
                                        "message": "Invalid virksomhetsnummer",
                                        "extensions": {
                                            "code": "VALIDATION_ERROR"
                                        }
                                    },
                                    {
                                        "message": "Missing required field",
                                        "extensions": {
                                            "field": "tekst"
                                        }
                                    }
                                ]
                            }
                            """.trimIndent()
                        )
                )
        )
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(
                BeskjedRequest(
                    virksomhetsnummer = virksomhetsnummer,
                    tekst = tekst,
                    lenke = lenke,
                )
            )
        }
        
        exception.message shouldContain "GraphQL feil ved opprettelse av beskjed"
        exception.message shouldContain "Invalid virksomhetsnummer"
        exception.message shouldContain "Missing required field"
    }

    test("opprettBeskjed should handle unknown response type") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        
        wireMockServer.stubFor(
            post(urlEqualTo("/api/graphql"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "data": {
                                    "nyBeskjed": {
                                        "__typename": "UnknownType",
                                        "someField": "someValue"
                                    }
                                }
                            }
                            """.trimIndent()
                        )
                )
        )
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(
                BeskjedRequest(
                    virksomhetsnummer = virksomhetsnummer,
                    tekst = tekst,
                    lenke = lenke,
                )
            )
        }
        
        exception.message shouldBe "Ukjent respons type: UnknownType"
    }

    test("opprettBeskjed should handle Error response type") {
        val virksomhetsnummer = "123456789"
        val tekst = "Test beskjed"
        val lenke = "https://test.nav.no/beskjed/123"
        val feilmelding = "Noe gikk galt"
        
        wireMockServer.stubFor(
            post(urlEqualTo("/api/graphql"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "data": {
                                    "nyBeskjed": {
                                        "__typename": "Error",
                                        "feilmelding": "$feilmelding"
                                    }
                                }
                            }
                            """.trimIndent()
                        )
                )
        )
        
        val exception = shouldThrow<RuntimeException> {
            consumer.opprettBeskjed(
                BeskjedRequest(
                    virksomhetsnummer = virksomhetsnummer,
                    tekst = tekst,
                    lenke = lenke,
                )
            )
        }
        
        exception.message shouldBe "Feil ved opprettelse av beskjed: $feilmelding"
    }
})