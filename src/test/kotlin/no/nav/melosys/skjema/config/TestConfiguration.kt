package no.nav.melosys.skjema.config

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import no.nav.melosys.skjema.sikkerhet.context.SpringSubjectHandler
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
class TestConfiguration {
    
    @Bean
    @Primary
    fun testSpringTokenValidationContextHolder(): SpringTokenValidationContextHolder {
        val mockContextHolder = mockk<SpringTokenValidationContextHolder>(relaxed = true)
        val mockContext = mockk<TokenValidationContext>(relaxed = true)
        
        every { mockContextHolder.getTokenValidationContext() } returns mockContext
        every { mockContext.hasTokenFor(any()) } returns false
        
        return mockContextHolder
    }
    
    @Bean
    @Primary
    fun testSpringSubjectHandler(contextHolder: SpringTokenValidationContextHolder): SpringSubjectHandler {
        return SpringSubjectHandler(contextHolder)
    }
    
    @Bean
    @Primary
    fun testArbeidsgiverAltinnTilgangerConsumer(): ArbeidsgiverAltinnTilgangerConsumer {
        val mockConsumer = mockk<ArbeidsgiverAltinnTilgangerConsumer>(relaxed = true)
        
        // Mock default response for hentTilganger - returnerer AltinnTilgangerResponse
        every { 
            mockConsumer.hentTilganger(any()) 
        } returns AltinnTilgangerResponse(
            isError = false,
            hierarki = emptyList(),
            tilgangTilOrgNr = emptyMap(),
            orgNrTilTilganger = emptyMap()
        )
        
        return mockConsumer
    }
    
    @Bean
    fun testWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl("http://localhost:8080/mock")
            .build()
    }
}