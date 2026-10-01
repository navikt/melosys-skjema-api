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

@Component
class AdminTilgangInterceptor(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    m2mConfigProperties: M2mConfigProperties
) : HandlerInterceptor {

    private val driftsgruppeId = m2mConfigProperties.admin.driftsgruppe

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val claims = gyldigeAzureClaims() ?: return true   // @AdminBeskyttet svarer 401

        if (erMaskinkall(claims)) return true   // Consoles statistikkhenting, uten innlogget bruker
        if (erMedlemAvDriftsgruppe(claims)) return true

        log.warn { "Admin-kall avvist: personkall uten driftsgruppe (${request.method})" }
        // Skrives direkte, så Console viser årsaken. AccessDeniedException gir bare «Ingen tilgang».
        response.status = 403
        response.contentType = MediaType.TEXT_PLAIN_VALUE
        response.writer.write(MANGLER_DRIFTSGRUPPE)
        return false
    }

    // Token-support legger bare validerte token i konteksten
    private fun gyldigeAzureClaims() =
        tokenValidationContextHolder.getTokenValidationContext().getJwtToken(AZURE)?.jwtTokenClaims

    // Entra setter idtyp = app bare i maskintoken. Mangler den, regnes kallet som personkall.
    private fun erMaskinkall(claims: JwtTokenClaims) = claims.getStringClaim(IDTYP_CLAIM) == IDTYP_MASKIN

    // getAsList gir null når groups mangler
    private fun erMedlemAvDriftsgruppe(claims: JwtTokenClaims) =
        driftsgruppeId in claims.getAsList(GROUPS_CLAIM).orEmpty()

    companion object {
        const val MANGLER_DRIFTSGRUPPE = "Mangler tilgang til admin-endepunkter"
        private const val AZURE = "azure"
        private const val IDTYP_CLAIM = "idtyp"
        private const val IDTYP_MASKIN = "app"
        private const val GROUPS_CLAIM = "groups"
    }
}
