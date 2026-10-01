package no.nav.melosys.skjema.sikkerhet

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.melosys.skjema.config.M2mConfigProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val log = KotlinLogging.logger {}

/**
 * Personkall til admin-endepunktene (under /admin) må ha driftsgruppen i groups-claimet.
 * Maskinkall (idtyp = app) slipper gjennom, slik at Consoles automatiske statistikkhenting
 * virker som før. API-nøkkelen ([AdminApiKeyInterceptor]) og klientsjekken ([AdminBeskyttet])
 * gjelder fortsatt for alle kall.
 */
@Component
class AdminTilgangInterceptor(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val m2mConfigProperties: M2mConfigProperties
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Uten gyldig Azure-token svarer @AdminBeskyttet 401
        val claims = tokenValidationContextHolder.getTokenValidationContext().getJwtToken(AZURE)?.jwtTokenClaims
            ?: return true

        if (erMaskinkall(claims)) return true
        if (erMedlemAvDriftsgruppe(claims)) return true

        log.warn { "Admin-kall avvist: personkall uten driftsgruppe (${request.method})" }
        // Skrives direkte: Spring Boot tar ikke med meldingen fra ResponseStatusException i svaret
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.TEXT_PLAIN_VALUE
        response.writer.write(MANGLER_DRIFTSGRUPPE)
        return false
    }

    // Entra setter idtyp = app bare i maskintoken. Mangler den, regnes kallet som personkall.
    private fun erMaskinkall(claims: JwtTokenClaims) = claims.getStringClaim(IDTYP_CLAIM) == IDTYP_MASKIN

    // getAsList gir null når groups mangler
    private fun erMedlemAvDriftsgruppe(claims: JwtTokenClaims) =
        m2mConfigProperties.admin.driftsgruppe in claims.getAsList(GROUPS_CLAIM).orEmpty()

    companion object {
        const val MANGLER_DRIFTSGRUPPE = "Mangler tilgang til admin-endepunkter"
        private const val AZURE = "azure"
        private const val IDTYP_CLAIM = "idtyp"
        private const val IDTYP_MASKIN = "app"
        private const val GROUPS_CLAIM = "groups"
    }
}
