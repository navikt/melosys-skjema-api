package no.nav.melosys.skjema.integrasjon.felles

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class WebClientConfigTest {

    private fun connectionTimeout() = WebClientRequestException(
        java.net.SocketTimeoutException("Connection timed out"),
        HttpMethod.POST,
        URI.create("https://pdl-api.example.no/graphql"),
        HttpHeaders.EMPTY
    )

    @Test
    fun `retryer forbigående forbindelsesfeil og lykkes til slutt`() {
        val attempts = AtomicInteger(0)

        val resultat = Mono.defer {
            if (attempts.incrementAndGet() < 3) Mono.error(connectionTimeout()) else Mono.just("ok")
        }
            .retryWhen(WebClientConfig.defaultRetry(maxAttempts = 3, backoffDurationMillis = 10))
            .block()

        assertThat(resultat).isEqualTo("ok")
        assertThat(attempts.get()).isEqualTo(3)
    }

    @Test
    fun `retryer ikke HTTP 4xx-svar`() {
        val attempts = AtomicInteger(0)
        val badRequest = WebClientResponseException.create(
            HttpStatus.BAD_REQUEST.value(), "Bad Request", HttpHeaders.EMPTY, ByteArray(0), null
        )

        assertThatThrownBy {
            Mono.defer { attempts.incrementAndGet(); Mono.error<String>(badRequest) }
                .retryWhen(WebClientConfig.defaultRetry(maxAttempts = 3, backoffDurationMillis = 10))
                .block()
        }.isInstanceOf(WebClientResponseException::class.java)

        assertThat(attempts.get()).isEqualTo(1)
    }

    @Test
    fun `retryer ikke vilkårlige feil`() {
        val attempts = AtomicInteger(0)

        assertThatThrownBy {
            Mono.defer { attempts.incrementAndGet(); Mono.error<String>(IllegalArgumentException("nei")) }
                .retryWhen(WebClientConfig.defaultRetry(maxAttempts = 3, backoffDurationMillis = 10))
                .block()
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThat(attempts.get()).isEqualTo(1)
    }
}
