package no.nav.melosys.skjema.sikkerhet

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import no.nav.melosys.skjema.config.M2mConfigProperties
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val log = KotlinLogging.logger {}

/**
 * Krever en gyldig delt API-nøkkel i header for alle admin-endepunktene (under /admin),
 * i tillegg til Azure AD-token og azp_name-allowlist (se [AdminBeskyttet]).
 *
 * Tilsvarer mønsteret i melosys-api: console sender nøkkelen i [API_KEY_HEADER].
 */
@Component
class AdminApiKeyInterceptor(
    private val m2mConfigProperties: M2mConfigProperties
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (!gyldigApiNokkel(request.getHeader(API_KEY_HEADER))) {
            log.warn { "Admin: ugyldig eller manglende API-nøkkel for ${request.method} ${request.requestURI}" }
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.writer.write("Ugyldig API-nøkkel")
            return false
        }
        return true
    }

    private fun gyldigApiNokkel(oppgitt: String?): Boolean {
        if (oppgitt.isNullOrEmpty()) return false
        // Konstant-tid sammenligning for å unngå timing-angrep mot nøkkelen.
        return MessageDigest.isEqual(
            oppgitt.toByteArray(StandardCharsets.UTF_8),
            m2mConfigProperties.admin.apikey.toByteArray(StandardCharsets.UTF_8)
        )
    }

    companion object {
        const val API_KEY_HEADER = "X-MELOSYS-SKJEMA-ADMIN-APIKEY"
    }
}
