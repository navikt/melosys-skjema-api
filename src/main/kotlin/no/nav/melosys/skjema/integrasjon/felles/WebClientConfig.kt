package no.nav.melosys.skjema.integrasjon.felles

import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.core.publisher.Mono

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
}