package no.nav.melosys.skjema.sikkerhet

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.M2mConfigProperties
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Aspect
@Component
class M2MProtectedAspect(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val m2mConfigProperties: M2mConfigProperties
) {
    companion object {
        private const val AZURE = "azure"
        private const val AZP_NAME_CLAIM = "azp_name"
    }

    @Before("@annotation(no.nav.melosys.skjema.sikkerhet.M2MReadSkjemadata)")
    fun validateReadSkjemadataAccess() {
        validateClientAccess(m2mConfigProperties.readSkjemadata.clients)
    }

    private fun validateClientAccess(allowedClients: List<String>) {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.getJwtToken(AZURE)
            ?: throw AccessDeniedException("Ingen gyldig Azure AD-token funnet")

        val azpName = token.jwtTokenClaims.getStringClaim(AZP_NAME_CLAIM)
            ?: throw AccessDeniedException("Token mangler azp_name-claim")

        if (azpName !in allowedClients) {
            log.warn { "Klient '$azpName' har ikke tilgang. Tillatte klienter: $allowedClients" }
            throw AccessDeniedException("Klient har ikke tilgang til denne ressursen")
        }

        log.debug { "Klient '$azpName' har gyldig tilgang" }
    }
}
