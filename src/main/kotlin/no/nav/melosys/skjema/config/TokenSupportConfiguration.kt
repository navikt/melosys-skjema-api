package no.nav.melosys.skjema.config

import no.nav.security.token.support.client.spring.oauth2.ClientConfigurationPropertiesMatcher
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EnableOAuth2Client(cacheEnabled = true)
@EnableJwtTokenValidation
@Configuration
class TokenSupportConfiguration {

    //TODO: Sjekk om denne er nødvendig.
    @Bean
    fun configMatcher() = object : ClientConfigurationPropertiesMatcher {}
}