package no.nav.melosys.skjema.config.observability

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import org.slf4j.MDC

/**
 * Utility-klasse for håndtering av MDC (Mapped Diagnostic Context).
 * Brukes for å spre correlation ID og andre kontekstuelle data gjennom hele request-chainen.
 */
class MDCOperations {
    companion object {
        const val MDC_CALL_ID = "callId"
        const val MDC_CONSUMER_ID = "consumerId"
        const val CORRELATION_ID = "correlation-id"
        const val X_CORRELATION_ID = "X-Correlation-ID"
        private const val SYSTEMBRUKER = "melosys-skjema-api"

        /**
         * Henter verdi fra MDC
         */
        fun getFromMDC(key: String): String? = MDC.get(key)

        /**
         * Legger til verdi i MDC
         */
        fun putToMDC(key: String, value: String?) {
            if (value != null) {
                MDC.put(key, value)
            }
        }

        fun setCorrelationId(correlationId: String? = null) {
            putToMDC(CORRELATION_ID, correlationId ?: UUID.randomUUID().toString())
        }

        /**
         * Setter correlation ID (eller genererer ny hvis null), kjører funksjonen, og rydder opp MDC etterpå.
         */
        inline fun <T> withCorrelationId(correlationId: String? = null, block: () -> T): T {
            return try {
                setCorrelationId(correlationId ?: UUID.randomUUID().toString())
                block()
            } finally {
                remove(CORRELATION_ID)
            }
        }

        /**
         * Fjerner verdi fra MDC
         */
        fun remove(key: String) {
            MDC.remove(key)
        }

        /**
         * Genererer en unik call ID
         */
        fun generateCallId(): String = UUID.randomUUID().toString()

        /**
         * Henter correlation ID fra MDC, eller genererer en ny hvis den ikke finnes
         */
        fun getCorrelationId(): String {
            return getFromMDC(CORRELATION_ID) ?: generateCallId()
        }

        /**
         * Henter correlation ID fra HTTP request header, eller genererer en ny
         */
        fun getCorrelationId(request: HttpServletRequest): String {
            return request.getHeader(X_CORRELATION_ID) ?: generateCallId()
        }

        /**
         * Henter systembruker navn
         */
        fun getSystembruker(): String = SYSTEMBRUKER
    }
}
