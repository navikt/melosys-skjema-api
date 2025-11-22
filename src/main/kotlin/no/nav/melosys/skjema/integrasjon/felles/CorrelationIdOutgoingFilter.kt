package no.nav.melosys.skjema.integrasjon.felles

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.util.*

/**
 * Filter som legger til X-Correlation-ID header på utgående requests
 * for å kunne spore requests på tvers av systemer
 */
@Component
class CorrelationIdOutgoingFilter : ExchangeFilterFunction {

    companion object {
        private const val X_CORRELATION_ID = "X-Correlation-ID"
    }

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(
            ClientRequest.from(request)
                .header(X_CORRELATION_ID, UUID.randomUUID().toString())
                .build()
        )
    }
}
