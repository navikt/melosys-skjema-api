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
    
    // Remove mock SpringTokenValidationContextHolder to allow real OAuth2 integration
    
    @Bean
    @Primary
    fun testArbeidsgiverAltinnTilgangerConsumer(): ArbeidsgiverAltinnTilgangerConsumer {
        // Create relaxed mock without default behavior - let individual tests configure it
        return mockk<ArbeidsgiverAltinnTilgangerConsumer>(relaxed = true)
    }
    
    @Bean
    fun testWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl("http://localhost:8080/mock")
            .build()
    }
}