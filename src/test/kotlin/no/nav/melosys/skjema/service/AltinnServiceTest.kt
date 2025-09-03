package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import no.nav.melosys.skjema.integrasjon.altinn.dto.OrganisasjonMedTilgang

class AltinnServiceTest : FunSpec({
    
    val mockConsumer = mockk<ArbeidsgiverAltinnTilgangerConsumer>()
    val service = AltinnService(mockConsumer)
    
    test("hentBrukersTilganger skal returnere organisasjoner fra consumer") {
        val organisasjoner = listOf(
            OrganisasjonMedTilgang(
                orgnr = "123456789",
                navn = "Test AS",
                organisasjonsform = "AS"
            ),
            OrganisasjonMedTilgang(
                orgnr = "987654321",
                navn = "Test2 AS",
                organisasjonsform = "AS"
            )
        )
        
        every { mockConsumer.hentTilganger(null) } returns organisasjoner
        
        val result = service.hentBrukersTilganger()
        
        result shouldHaveSize 2
        result[0].orgnr shouldBe "123456789"
        result[0].navn shouldBe "Test AS"
        result[1].orgnr shouldBe "987654321"
        result[1].navn shouldBe "Test2 AS"
        
        verify(exactly = 1) { mockConsumer.hentTilganger(null) }
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
        
        every { mockConsumer.harTilgang(orgnr) } returns true
        
        val result = service.harBrukerTilgang(orgnr)
        
        result shouldBe true
        verify(exactly = 1) { mockConsumer.harTilgang(orgnr) }
    }
    
    test("harBrukerTilgang skal returnere false når bruker ikke har tilgang") {
        val orgnr = "987654321"
        
        every { mockConsumer.harTilgang(orgnr) } returns false
        
        val result = service.harBrukerTilgang(orgnr)
        
        result shouldBe false
        verify(exactly = 1) { mockConsumer.harTilgang(orgnr) }
    }
    
    test("harBrukerTilgang skal returnere false ved exception") {
        val orgnr = "123456789"
        
        every { 
            mockConsumer.harTilgang(orgnr) 
        } throws RuntimeException("Tilgangsfeil")
        
        val result = service.harBrukerTilgang(orgnr)
        
        result shouldBe false
    }
})