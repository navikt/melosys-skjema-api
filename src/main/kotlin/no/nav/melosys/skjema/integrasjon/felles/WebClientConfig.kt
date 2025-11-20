package no.nav.melosys.skjema.integrasjon.felles

import java.time.Duration
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import reactor.util.retry.Retry.RetrySignal
import reactor.util.retry.RetryBackoffSpec

object WebClientConfig {
    fun errorFilter(feilmelding: String): ExchangeFilterFunction {
        return errorFilter(feilmelding, emptySet())
    }

    fun errorFilter(feilmelding: String, ignoreStatuses: Set<Int>): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofResponseProcessor { response ->
            if (response.statusCode().isError && response.statusCode().value() !in ignoreStatuses) {
                response.bodyToMono(String::class.java)
                    .defaultIfEmpty(response.statusCode().toString())
                    .flatMap { errorBody ->
                        Mono.error(lagException(feilmelding, response.statusCode(), errorBody))
                    }
            } else {
                Mono.just(response)
            }
        }
    }

    fun lagException(feilmelding: String, statusCode: HttpStatusCode, errorBody: String): Exception {
        return RuntimeException("$feilmelding $statusCode - $errorBody")
    }

    fun defaultRetry(maxAttempts: Int = 3, backoffDurationMillis: Int = 2000): Retry {
        return Retry
            .backoff(maxAttempts.toLong(), Duration.ofMillis(backoffDurationMillis.toLong()))
            .jitter(0.75)
            .filter { throwable: Throwable? ->
                if (throwable is WebClientResponseException) {
                    throwable.statusCode.is5xxServerError
                } else {
                    false
                }
            }
            .onRetryExhaustedThrow { retryBackoffSpec: RetryBackoffSpec?, retrySignal: RetrySignal ->
                val throwable = retrySignal.failure()
                if (throwable is WebClientResponseException) {
                    throw throwable
                }
                throw RuntimeException(throwable)
            }
    }
}