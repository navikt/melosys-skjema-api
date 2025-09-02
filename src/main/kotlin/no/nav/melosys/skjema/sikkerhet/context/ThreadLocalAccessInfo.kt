package no.nav.melosys.skjema.sikkerhet.context

import org.slf4j.LoggerFactory
import java.util.UUID

object ThreadLocalAccessInfo {
    private val log = LoggerFactory.getLogger(ThreadLocalAccessInfo::class.java)

    private val threadLocalStorage = ThreadLocal.withInitial { AccessInfo() }

    data class AccessInfo(
        var requestUri: String? = null,
        var processId: UUID? = null,
        var processName: String? = null,
        var isAdminRequest: Boolean = false,
        var saksbehandler: String? = null,
        var saksbehandlerNavn: String? = null
    ) {
        fun isFromWebRequest(): Boolean = requestUri != null
        fun isFromProcess(): Boolean = processId != null
        fun isFromAdminRequest(): Boolean = isAdminRequest
    }

    fun getProcessId(): UUID? {
        return threadLocalStorage.get().processId
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
        log.debug("Etter en controller request: {}", threadLocalStorage.get())

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
            log.warn("Kall har ikke blitt registrert fra RestController eller Prosess\n$stackTraceAsString")
            return false
        }

        return accessInfo.isFromAdminRequest()
    }

    fun getInfo(): String {
        return threadLocalStorage.get().toString()
    }
}
