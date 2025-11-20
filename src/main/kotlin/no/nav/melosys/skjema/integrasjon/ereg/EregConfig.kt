package no.nav.melosys.skjema.integrasjon.ereg

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import java.time.Duration
import java.util.concurrent.TimeUnit
import no.nav.melosys.skjema.integrasjon.felles.WebClientConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient

private val log = KotlinLogging.logger { }

@Configuration
class EregConfig(
    @param:Value("\${ereg.url}") private val eregBaseUrl: String,
    @param:Value("\${ereg.timeout.connect-seconds:10}") private val connectTimeoutSeconds: Long,
    @param:Value("\${ereg.timeout.read-seconds:30}") private val readTimeoutSeconds: Long,
    @param:Value("\${ereg.timeout.write-seconds:30}") private val writeTimeoutSeconds: Long
) {

    @Bean
    fun eregClient(webClientBuilder: WebClient.Builder): WebClient {
        log.info { "Konfigurerer EregConsumer med base URL: $eregBaseUrl" }

        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (connectTimeoutSeconds * 1000).toInt())
            .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(writeTimeoutSeconds, TimeUnit.SECONDS))
            }

        return webClientBuilder
            .baseUrl(eregBaseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .filter(
                WebClientConfig.errorFilter(
                    feilmelding = "Kall mot EREG feilet",
                    ignoreStatuses = setOf(HttpStatus.NOT_FOUND.value())
                )
            )
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
