package no.nav.melosys.skjema.sikkerhet

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.melosys.skjema.config.AdminConfigProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val log = KotlinLogging.logger {}

@Component
class AdminTilgangInterceptor(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    adminConfigProperties: AdminConfigProperties
) : HandlerInterceptor {

    private val driftsgruppeId = adminConfigProperties.driftsgruppe
    private val consoleKlientId = adminConfigProperties.consoleKlientId

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Avvis her i stedet for å stole på @AdminBeskyttet, så en admin-kontroller uten annotasjonen
        // ikke kjører uten token eller med token fra en annen klient
        val claims = gyldigeAzureClaims()
        if (claims == null) {
            log.warn { "Admin-kall avvist: mangler gyldig Azure-token (${request.method})" }
            return avvis(response, 401, MANGLER_TOKEN)
        }

        // Gjelder både person- og maskinkall, så klienten sjekkes før idtyp
        val azp = claims.getStringClaim(AZP_CLAIM)
        if (azp != consoleKlientId) {
            // azp er en klient-ID, ikke en personopplysning
            log.warn { "Admin-kall avvist: ukjent klient (azp=$azp, ${request.method})" }
            return avvis(response, 403, UKJENT_KLIENT)
        }

        if (erMaskinkall(claims)) return true   // Consoles statistikkhenting, uten innlogget bruker
        if (erMedlemAvDriftsgruppe(claims)) return true

        log.warn { "Admin-kall avvist: personkall uten driftsgruppe (${request.method})" }
        return avvis(response, 403, MANGLER_DRIFTSGRUPPE)
    }

    // Skrives direkte, så Console viser årsaken. AccessDeniedException gir bare «Ingen tilgang».
    private fun avvis(response: HttpServletResponse, status: Int, melding: String): Boolean {
        response.status = status
        response.contentType = MediaType.TEXT_PLAIN_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(melding)
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
        const val MANGLER_TOKEN = "Mangler gyldig token"
        const val UKJENT_KLIENT = "Kallet kommer ikke fra en godkjent klient"
        const val MANGLER_DRIFTSGRUPPE = "Mangler tilgang til admin-endepunkter"
        private const val AZURE = "azure"
        private const val AZP_CLAIM = "azp"
        private const val IDTYP_CLAIM = "idtyp"
        private const val IDTYP_MASKIN = "app"
        private const val GROUPS_CLAIM = "groups"
    }
}
