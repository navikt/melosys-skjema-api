package no.nav.melosys.skjema.integrasjon.arbeidsgiver

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
class ArbeidsgiverNotifikasjonClientProducer(
    @param:Value("\${arbeidsgiver.notifikasjon.url}") private val arbeidsgiverNotifikasjonBaseUrl: String,
) {

    @Bean
    fun arbeidsgiverNotifikasjonWebClient(
        webClientBuilder: WebClient.Builder
    ): WebClient {
        log.info { "Konfigurerer ArbeidsgiverNotifikasjonConsumer med base URL: $arbeidsgiverNotifikasjonBaseUrl" }

        return webClientBuilder
            .baseUrl(arbeidsgiverNotifikasjonBaseUrl)
            .filter(WebClientConfig.errorFilter("Kall mot arbeidsgiver-notifikasjon feilet"))
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