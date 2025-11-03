package no.nav.melosys.skjema.integrasjon.ereg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.WebClientConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }

@Configuration
class EregConfig(
    @param:Value("\${ereg.url}") private val eregBaseUrl: String,
) {

    @Bean
    fun eregClient(webClientBuilder: WebClient.Builder): WebClient {
        log.info { "Konfigurerer EregConsumer med base URL: $eregBaseUrl" }

        return webClientBuilder
            .baseUrl(eregBaseUrl)
            .filter(WebClientConfig.errorFilter("Kall mot EREG feilet"))
            .filter(headerFilter())
            .build()
    }

    private fun headerFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            Mono.just(
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Nav-Consumer-Id", "melosys-skjema-api")
                    .build()
            )
        }
    }
}
