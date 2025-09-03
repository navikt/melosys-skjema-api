package no.nav.melosys.skjema.sikkerhet.context

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger {}

object ThreadLocalAccessInfo {

    private val threadLocalStorage = ThreadLocal.withInitial { AccessInfo() }

    data class AccessInfo(
        var requestUri: String? = null,
        var processId: UUID? = null,
        var processName: String? = null,
        var isAdminRequest: Boolean = false,
        var brukerID: String? = null,
        var brukerNavn: String? = null
    ) {
        fun isFromWebRequest(): Boolean = requestUri != null
        fun isFromProcess(): Boolean = processId != null
        fun isFromAdminRequest(): Boolean = isAdminRequest
    }

    //TODO: Implementer admin-request-sjekk senere
    fun beforeControllerRequest(requestUri: String, isAdminRequest: Boolean = false) {
        val accessInfo = threadLocalStorage.get()
        if (accessInfo.requestUri != null) {
            throw IllegalStateException("Vi skulle ikke ha en thread local requestUri før controller request")
        }
        accessInfo.requestUri = requestUri
        accessInfo.isAdminRequest = isAdminRequest
    }

    fun afterControllerRequest(requestUri: String) {
        log.debug {
            "Etter en controller request: ${threadLocalStorage.get()}"
        }

        val accessInfo = threadLocalStorage.get()
        if (accessInfo.requestUri != requestUri) {
            throw IllegalStateException(
                "Start og slutt request skal være like\n" +
                        "${accessInfo.requestUri} != $requestUri"
            )
        }
        threadLocalStorage.remove()
    }

    fun shouldUseM2MToken(): Boolean {
        val accessInfo = threadLocalStorage.get()

        // M2M tokens brukes kun når vi eksplisitt har behov for det
        // For nå returnerer vi false siden vi primært bruker OBO-tokens
        if (accessInfo.isFromProcess()) {
            return true
        }

        if (!accessInfo.isFromWebRequest()) {
            val stackTrace = Thread.currentThread().stackTrace
            val stackTraceAsString = stackTrace.joinToString("\n") { it.toString() }
            log.warn{
                "Kall har ikke blitt registrert fra RestController eller Prosess \n $stackTraceAsString"
            }
            return false
        }

        return accessInfo.isFromAdminRequest()
    }
}
