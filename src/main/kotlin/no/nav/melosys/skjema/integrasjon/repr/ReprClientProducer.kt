package no.nav.melosys.skjema.integrasjon.repr

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import java.time.Duration
import java.util.concurrent.TimeUnit
import no.nav.melosys.skjema.integrasjon.felles.TokenXContextExchangeFilter
import no.nav.melosys.skjema.integrasjon.felles.WebClientConfig
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient

private val log = KotlinLogging.logger { }

@Configuration
class ReprClientProducer(
    @param:Value("\${repr.url}") private val reprBaseUrl: String,
    @param:Value("\${repr.timeout.connect-seconds:10}") private val connectTimeoutSeconds: Long,
    @param:Value("\${repr.timeout.read-seconds:30}") private val readTimeoutSeconds: Long,
    @param:Value("\${repr.timeout.write-seconds:30}") private val writeTimeoutSeconds: Long
) {

    companion object {
        private const val CLIENT_NAME = "repr-api"
    }

    @Bean
    fun reprClientTokenX(
        webClientBuilder: WebClient.Builder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): WebClient {
        log.info { "Konfigurerer ReprConsumer med base URL: $reprBaseUrl" }

        val tokenXContextExchangeFilter = TokenXContextExchangeFilter(
            clientConfigurationProperties,
            oAuth2AccessTokenService,
            CLIENT_NAME
        )

        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (connectTimeoutSeconds * 1000).toInt())
            .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(writeTimeoutSeconds, TimeUnit.SECONDS))
            }

        return webClientBuilder
            .baseUrl(reprBaseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .filter(tokenXContextExchangeFilter)
            .filter(WebClientConfig.errorFilter("Kall mot repr-api feilet"))
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
