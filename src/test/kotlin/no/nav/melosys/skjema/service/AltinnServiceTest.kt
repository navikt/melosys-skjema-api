package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse

class AltinnServiceTest : FunSpec({
    
    val mockConsumer = mockk<ArbeidsgiverAltinnTilgangerConsumer>()
    val altinnRessurs = "test-fager"
    val service = AltinnService(mockConsumer, altinnRessurs)
    
    test("hentBrukersTilganger skal returnere organisasjoner med riktig ressurs") {
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "123456789",
                    navn = "Test AS",
                    organisasjonsform = "AS",
                    altinn3Tilganger = setOf("test-fager", "annen-rolle")
                ),
                AltinnTilgang(
                    orgnr = "987654321",
                    navn = "Test2 AS", 
                    organisasjonsform = "AS",
                    altinn3Tilganger = setOf("annen-rolle")
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-fager" to setOf("123456789"),
                "annen-rolle" to setOf("123456789", "987654321")
            ),
            orgNrTilTilganger = emptyMap()
        )
        
        every { mockConsumer.hentTilganger(null) } returns response
        
        val result = service.hentBrukersTilganger()
        
        result shouldHaveSize 1
        result[0].orgnr shouldBe "123456789"
        result[0].navn shouldBe "Test AS"
        result[0].organisasjonsform shouldBe "AS"
        
        verify(exactly = 1) { mockConsumer.hentTilganger(null) }
    }
    
    test("hentBrukersTilganger skal returnere tom liste når isError = true") {
        val response = AltinnTilgangerResponse(
            isError = true,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        every { mockConsumer.hentTilganger(null) } returns response
        
        val result = service.hentBrukersTilganger()
        
        result.shouldBeEmpty()
    }
    
    test("hentBrukersTilganger skal returnere tom liste ved exception") {
        every { 
            mockConsumer.hentTilganger(null) 
        } throws RuntimeException("Nettverksfeil")
        
        val result = service.hentBrukersTilganger()
        
        result.shouldBeEmpty()
    }
    
    test("harBrukerTilgang skal returnere true når bruker har tilgang") {
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
                "test-fager" to setOf(orgnr)
            ),
            orgNrTilTilganger = emptyMap()
        )
        
        every { mockConsumer.hentTilganger(null) } returns response
        
        val result = service.harBrukerTilgang(orgnr)
        
        result shouldBe true
    }
    
    test("harBrukerTilgang skal returnere false når bruker ikke har tilgang") {
        val orgnr = "123456789"
        val response = AltinnTilgangerResponse(
            isError = false,
            hierarki = listOf(
                AltinnTilgang(
                    orgnr = "987654321",
                    navn = "Annen AS",
                    organisasjonsform = "AS"
                )
            ),
            tilgangTilOrgNr = mapOf(
                "test-fager" to setOf("987654321")
            ),
            orgNrTilTilganger = emptyMap()
        )
        
        every { mockConsumer.hentTilganger(null) } returns response
        
        val result = service.harBrukerTilgang(orgnr)
        
        result shouldBe false
    }
    
    test("harBrukerTilgang skal returnere false ved exception") {
        val orgnr = "123456789"
        
        every { 
            mockConsumer.hentTilganger(null) 
        } throws RuntimeException("Tilgangsfeil")
        
        val result = service.harBrukerTilgang(orgnr)
        
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
            orgNrTilTilganger = emptyMap()
        )
        
        every { mockConsumer.hentTilganger(null) } returns response
        
        val result = service.hentBrukersTilganger()
        
        result shouldHaveSize 1
        result[0].orgnr shouldBe "222222222"
        result[0].navn shouldBe "Underenhet"
        result[0].organisasjonsform shouldBe "BEDR"
    }
})