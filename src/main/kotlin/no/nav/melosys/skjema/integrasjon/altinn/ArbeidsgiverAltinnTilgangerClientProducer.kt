package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.WebClientConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }

@Configuration
class ArbeidsgiverAltinnTilgangerClientProducer(
    @param:Value("\${arbeidsgiver.altinn.tilganger.url}") private val arbeidsgiverAltinnTilgangerBaseUrl: String,
) {

    companion object {
        private const val MAX_IN_MEMORY_SIZE_BYTES = 16 * 1024 * 1024
    }

    @Bean
    fun arbeidsgiverAltinnTilgangerClient(
        webClientBuilder: WebClient.Builder
    ): WebClient {
        log.info { "Konfigurerer ArbeidsgiverAltinnTilgangerConsumer med base URL: $arbeidsgiverAltinnTilgangerBaseUrl" }

        return webClientBuilder
            .baseUrl(arbeidsgiverAltinnTilgangerBaseUrl)
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
                    .build()
            )
            .filter(WebClientConfig.errorFilter("Kall mot arbeidsgiver-altinn-tilganger feilet"))
            .filter(headerFilter())
            .build()
    }

    private fun headerFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            Mono.just(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()
            )
        }
    }
}