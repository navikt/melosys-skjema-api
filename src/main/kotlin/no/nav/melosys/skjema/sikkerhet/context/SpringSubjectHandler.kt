package no.nav.melosys.skjema.sikkerhet.context

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Component
class SpringSubjectHandler(
    private val contextHolder: SpringTokenValidationContextHolder
) : SubjectHandler {

    init {
        SubjectHandler.set(this)
    }

    companion object {
        private const val TOKENX = "tokenx"

        // Standard TokenX claims
        private const val JWT_TOKEN_CLAIM_PID = "pid"           // Personidentifikator (fødselsnummer)
        private const val JWT_TOKEN_CLAIM_SUB = "sub"           // Subject - vanligvis samme som pid
        private const val JWT_TOKEN_CLAIM_CLIENT_ID = "client_id"
        private const val JWT_TOKEN_CLAIM_ACR = "acr"           // Autentiseringsnivå
        private const val JWT_TOKEN_CLAIM_IDP = "idp"           // Identity Provider

        // Consumer claims - for organisasjonskontekst
        private const val JWT_TOKEN_CLAIM_CONSUMER = "consumer"
        private const val JWT_TOKEN_CLAIM_CONSUMER_AUTHORITY = "authority"
        private const val JWT_TOKEN_CLAIM_CONSUMER_ID = "ID"

        // Klient-autentiseringsmetode
        private const val JWT_TOKEN_CLAIM_CLIENT_AMR = "client_amr"
    }

    override fun getOidcTokenString(): String? {
        return if (hasValidToken()) tokenXToken()?.encodedToken else null
    }

    override fun getUserID(): String? {
        if (!hasValidToken()) return null

        val token = tokenXToken() ?: return null

        // Sjekk om dette er en maskin-til-maskin token
        if (isM2MToken(token)) {
            return getClientId(token)
        }

        // For person-tokens, bruk pid (personnummer/fødselsnummer)
        // Fallback til sub hvis pid ikke er tilstede
        return token.jwtTokenClaims.get(JWT_TOKEN_CLAIM_PID)?.toString()
            ?: token.jwtTokenClaims.get(JWT_TOKEN_CLAIM_SUB)?.toString()
    }

    override fun getUserName(): String? {
        if (!hasValidToken()) return null

        val token = tokenXToken() ?: return null

        // Sjekk om dette er en maskin-til-maskin token
        if (isM2MToken(token)) {
            return getClientId(token)
        }

        // TokenX tokens fra ID-porten har vanligvis ikke en name claim
        // Returner null eller eventuelt slå opp navnet fra en annen tjeneste ved bruk av pid
        return null
    }

    override fun getGroups(): List<String> {
        // TokenX tokens har vanligvis ikke grupper
        // Hvis du trenger autorisasjon, kan du slå dette opp fra en annen tjeneste
        // eller bruke ACR-nivået for grunnleggende autorisasjonsbeslutninger
        return emptyList()
    }

    /**
     * Hent autentiseringsnivået fra tokenet
     * ID-porten bruker Level3 og Level4 for forskjellige autentiseringsstyrker
     */
    override fun getAuthenticationLevel(): String? {
        if (!hasValidToken()) return null
        return tokenXToken()?.jwtTokenClaims?.get(JWT_TOKEN_CLAIM_ACR)?.toString()
    }

    /**
     * Hent identitetsleverandøren som autentiserte brukeren
     */
    override fun getIdentityProvider(): String? {
        if (!hasValidToken()) return null
        return tokenXToken()?.jwtTokenClaims?.get(JWT_TOKEN_CLAIM_IDP)?.toString()
    }

    /**
     * Hent consumer-organisasjonsinformasjon hvis tilstede
     * Returnerer et par med (authority, ID) eller null
     */
    override fun getConsumerInfo(): Pair<String, String>? {
        if (!hasValidToken()) return null

        val token = tokenXToken() ?: return null
        val consumerClaim = token.jwtTokenClaims.get(JWT_TOKEN_CLAIM_CONSUMER) as? Map<*, *>
            ?: return null

        val authority = consumerClaim[JWT_TOKEN_CLAIM_CONSUMER_AUTHORITY]?.toString()
        val id = consumerClaim[JWT_TOKEN_CLAIM_CONSUMER_ID]?.toString()

        return if (authority != null && id != null) {
            Pair(authority, id)
        } else null
    }

    /**
     * Hent organisasjonsnummeret fra consumer info hvis det eksisterer
     * Formatet er typisk "0192:orgNummer"
     *
     * TODO: Sjekk om noe annet enn orgnr til NAV kommer ut her.
     */
    override fun getOrganizationNumber(): String? {
        val consumerInfo = getConsumerInfo() ?: return null
        val id = consumerInfo.second

        // Ekstraher organisasjonsnummer fra format som "0192:889640782"
        return if (id.contains(":")) {
            id.substringAfter(":")
        } else {
            id
        }
    }

    /**
     * Sjekk om dette er en maskin-til-maskin token
     * M2M tokens bruker typisk client credentials flow med private_key_jwt
     */
    private fun isM2MToken(token: JwtToken): Boolean {
        val clientAmr = token.jwtTokenClaims.get(JWT_TOKEN_CLAIM_CLIENT_AMR)?.toString()
        val pid = token.jwtTokenClaims.get(JWT_TOKEN_CLAIM_PID)

        // M2M tokens har typisk private_key_jwt som client_amr og ingen pid
        return clientAmr == "private_key_jwt" && pid == null
    }

    /**
     * Hent klient-ID fra tokenet
     */
    private fun getClientId(token: JwtToken): String? {
        return token.jwtTokenClaims.get(JWT_TOKEN_CLAIM_CLIENT_ID)?.toString()
    }

    private fun hasValidToken(): Boolean {
        return try {
            RequestContextHolder.getRequestAttributes() != null &&
                    getValidationContext().hasTokenFor(TOKENX)
        } catch (_: Exception) {
            false
        }
    }

    private fun tokenXToken(): JwtToken? {
        return try {
            getValidationContext().getJwtToken(TOKENX)
        } catch (_: Exception) {
            null
        }
    }

    private fun getValidationContext(): TokenValidationContext {
        return contextHolder.getTokenValidationContext()
    }
}