package no.nav.melosys.skjema.config.observability

import no.nav.melosys.skjema.config.observability.MDCOperations.Companion.X_CORRELATION_ID
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

/**
 * Interceptor som legger til X-Correlation-ID header på utgående requests
 * for å kunne spore requests på tvers av systemer.
 * Henter correlation ID fra MDC hvis tilgjengelig, eller genererer en ny.
 */
@Component
class CorrelationIdOutgoingInterceptor : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        request.headers.set(X_CORRELATION_ID, MDCOperations.getCorrelationId())
        return execution.execute(request, body)
    }
}
