package no.nav.melosys.skjema

import com.nimbusds.jose.JOSEObjectType
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback

const val ACCEPTED_AUDIENCE = "test-client-id"
const val ACCEPTED_AZURE_AUDIENCE = "test-azure-client-id"
const val MELOSYS_CLIENT_ID = "test-melosys-client-id"
const val READ_ONLY_CLIENT_ID = "test-read-only-client-id"
const val MELOSYS_CONSOLE_CLIENT_ID = "test-console-client-id"
const val ISSUER_ID = "tokenx"
const val AZURE_ISSUER_ID = "azure"

/** Må stemme med GROUP_MELOSYS_INNLOGGING_VAKT i application-test.yml. */
const val DRIFTSGRUPPE_ID = "00000000-0000-0000-0000-000000000001"

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

/** Klient som kun står i read-allowlisten (se application-test.yml) – for å teste read/write-skillet. */
fun MockOAuth2Server.m2mTokenWithOnlyReadSkjemaDataAccess(): String = tokenWithAzpClaim(READ_ONLY_CLIENT_ID)

fun MockOAuth2Server.m2mTokenWithoutAccess(): String = tokenWithAzpClaim("ukjent-klient-id")

/** Personkall fra Console for en som er i driftsgruppen. */
fun MockOAuth2Server.adminTokenMedTilgang(): String = adminPersonToken(grupper = listOf(DRIFTSGRUPPE_ID))

/** Personkall (OBO-token, uten idtyp). grupper = null gir token uten groups-claim. */
fun MockOAuth2Server.adminPersonToken(
    grupper: List<String>?,
    azpName: String = MELOSYS_CONSOLE_CLIENT_ID,
): String = getToken(
    issuerId = AZURE_ISSUER_ID,
    audiences = listOf(ACCEPTED_AZURE_AUDIENCE),
    claims = buildMap {
        put("azp_name", azpName)
        put("NAVident", "Z999999")
        grupper?.let { put("groups", it) }
    }
)

/** Maskinkall (idtyp = app), slik Console henter statistikk uten innlogget bruker. */
fun MockOAuth2Server.adminMaskinToken(azpName: String = MELOSYS_CONSOLE_CLIENT_ID): String = getToken(
    issuerId = AZURE_ISSUER_ID,
    audiences = listOf(ACCEPTED_AZURE_AUDIENCE),
    claims = mapOf("azp_name" to azpName, "idtyp" to "app")
)
