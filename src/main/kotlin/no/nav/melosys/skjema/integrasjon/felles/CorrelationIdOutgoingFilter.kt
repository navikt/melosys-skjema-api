package no.nav.melosys.skjema.integrasjon.felles

import no.nav.melosys.skjema.integrasjon.felles.MDCOperations.Companion.X_CORRELATION_ID
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

/**
 * Filter som legger til X-Correlation-ID header på utgående requests
 * for å kunne spore requests på tvers av systemer.
 * Henter correlation ID fra MDC hvis tilgjengelig, eller genererer en ny.
 */
@Component
class CorrelationIdOutgoingFilter : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(
            ClientRequest.from(request)
                .header(X_CORRELATION_ID, MDCOperations.getCorrelationId())
                .build()
        )
    }
}
