package no.nav.melosys.skjema

import com.nimbusds.jose.JOSEObjectType
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback

const val ACCEPTED_AUDIENCE = "test-client-id"
const val ACCEPTED_AZURE_AUDIENCE = "test-azure-client-id"
const val MELOSYS_CLIENT_ID = "test-melosys-client-id"
const val ISSUER_ID = "tokenx"
const val AZURE_ISSUER_ID = "azure"

fun MockOAuth2Server.getToken(
    issuerId: String = ISSUER_ID,
    audiences: List<String> = listOf(ACCEPTED_AUDIENCE),
    claims: Map<String, Any> = emptyMap(),
): String = this
    .issueToken(
        issuerId = issuerId,
        clientId = "clientId",
        tokenCallback =
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = "subjectId",
                typeHeader = JOSEObjectType.JWT.type,
                audience = audiences,
                claims = claims,
                expiry = 36000,
            ),
    ).serialize()

fun MockOAuth2Server.tokenWithAzpClaim(azpName: String): String = getToken(
    issuerId = AZURE_ISSUER_ID,
    audiences = listOf(ACCEPTED_AZURE_AUDIENCE),
    claims = mapOf("azp_name" to azpName)
)

fun MockOAuth2Server.m2mTokenWithReadSkjemaDataAccess(): String = tokenWithAzpClaim(MELOSYS_CLIENT_ID)

fun MockOAuth2Server.m2mTokenWithoutAccess(): String = tokenWithAzpClaim("ukjent-klient-id")
