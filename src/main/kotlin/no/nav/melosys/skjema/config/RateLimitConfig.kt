package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.service.RateLimitOperationType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
class RateLimitConfig {
    var organisasjonssok: TypeConfig = TypeConfig()

    /**
     * Henter konfigurasjon for en gitt operasjonstype.
     */
    fun getConfigFor(type: RateLimitOperationType): TypeConfig {
        return when (type) {
            RateLimitOperationType.ORGANISASJONSSOK -> organisasjonssok
        }
    }

    data class TypeConfig(
        /**
         * Maksimalt antall requests tillatt per bruker.
         */
        var maxRequests: Int = 15,

        /**
         * Tidsvindu for rate limiting i minutter.
         */
        var timeWindowMinutes: Long = 1
    ) {
        fun getTimeWindow(): Duration = Duration.ofMinutes(timeWindowMinutes)
    }
}
