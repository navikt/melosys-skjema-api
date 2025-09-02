package no.nav.melosys.skjema

import com.nimbusds.jose.JOSEObjectType
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback

const val ACCEPTED_AUDIENCE = "test-client-id"
const val ISSUER_ID = "tokenx"

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
