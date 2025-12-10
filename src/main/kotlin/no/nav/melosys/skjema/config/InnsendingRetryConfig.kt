package no.nav.melosys.skjema.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "innsending.retry")
class InnsendingRetryConfig {
    /** Intervall mellom retry-kjøringer i minutter */
    var fixedDelayMinutes: Long = 5

    /** Forsinkelse før første retry-kjøring i sekunder */
    var initialDelaySeconds: Long = 60

    /** Maksimalt antall retry-forsøk før oppgitt */
    var maxAttempts: Int = 5

    /** Hvor gammelt et MOTTATT-skjema må være før retry (minutter) */
    var staleThresholdMinutes: Long = 5

    fun getFixedDelayMillis(): Long = fixedDelayMinutes * 60 * 1000
    fun getInitialDelayMillis(): Long = initialDelaySeconds * 1000
}
