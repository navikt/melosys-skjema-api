package no.nav.melosys.skjema.integrasjon.felles

import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

class RestClientConfigTest {

    @Test
    fun `retryer forbigående forbindelsesfeil og lykkes til slutt`() {
        val forsøk = AtomicInteger(0)

        val resultat = RestClientConfig.withRetry(maxAttempts = 3, backoffMillis = 10) {
            if (forsøk.incrementAndGet() < 3) {
                throw ResourceAccessException("Connection timed out", SocketTimeoutException())
            }
            "ok"
        }

        assertThat(resultat).isEqualTo("ok")
        assertThat(forsøk.get()).isEqualTo(3)
    }

    @Test
    fun `retryer 5xx-svar`() {
        val forsøk = AtomicInteger(0)

        val resultat = RestClientConfig.withRetry(maxAttempts = 3, backoffMillis = 10) {
            if (forsøk.incrementAndGet() < 2) {
                throw HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
            }
            "ok"
        }

        assertThat(resultat).isEqualTo("ok")
        assertThat(forsøk.get()).isEqualTo(2)
    }

    @Test
    fun `retryer ikke HTTP 4xx-svar`() {
        val forsøk = AtomicInteger(0)
        val badRequest = HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, ByteArray(0), null
        )

        assertThatThrownBy {
            RestClientConfig.withRetry(maxAttempts = 3, backoffMillis = 10) {
                forsøk.incrementAndGet()
                throw badRequest
            }
        }.isInstanceOf(HttpClientErrorException::class.java)

        assertThat(forsøk.get()).isEqualTo(1)
    }

    @Test
    fun `retryer ikke vilkårlige feil`() {
        val forsøk = AtomicInteger(0)

        assertThatThrownBy {
            RestClientConfig.withRetry(maxAttempts = 3, backoffMillis = 10) {
                forsøk.incrementAndGet()
                throw IllegalArgumentException("nei")
            }
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThat(forsøk.get()).isEqualTo(1)
    }
}

