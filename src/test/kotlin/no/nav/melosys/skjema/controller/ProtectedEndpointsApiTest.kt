package no.nav.melosys.skjema.controller

import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.getToken
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.test.web.reactive.server.WebTestClient

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProtectedEndpointsApiTes: ApiTestBase() {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var oAuth2Server: MockOAuth2Server

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endepunkterSomKreverGyldigToken")
    fun `Endepunkter returnerer 401 uten gyldig access token`(httpMethod: HttpMethod, path: String) {
        val accessTokenWithInvalidAudience = oAuth2Server.getToken(
            audiences = listOf("invalid-audience")
        )

        webTestClient.method(httpMethod)
            .uri(path)
            .headers { it.setBearerAuth(accessTokenWithInvalidAudience) }
            .exchange()
            .expectStatus().isUnauthorized
    }

    fun endepunkterSomKreverGyldigToken(): List<Arguments> = listOf(
        // AuthController
        Arguments.of(HttpMethod.GET, "/api/auth/representasjoner"),

        // SkjemaController
        Arguments.of(HttpMethod.GET, "/api/skjema"),
        Arguments.of(HttpMethod.POST, "/api/skjema"),
        Arguments.of(HttpMethod.GET, "/api/skjema/123"),
        Arguments.of(HttpMethod.PUT, "/api/skjema/123"),
        Arguments.of(HttpMethod.DELETE, "/api/skjema/123"),
        Arguments.of(HttpMethod.POST, "/api/skjema/123/submit"),
        Arguments.of(HttpMethod.GET, "/api/skjema/123/pdf"),

        // PrefillController
        Arguments.of(HttpMethod.POST, "/api/preutfyll/person"),
        Arguments.of(HttpMethod.GET, "/api/preutfyll/org/123456789"),

        // FullmaktController
        Arguments.of(HttpMethod.POST, "/api/fullmakt"),
        Arguments.of(HttpMethod.GET, "/api/fullmakt/123"),
        Arguments.of(HttpMethod.POST, "/api/fullmakt/123/godkjenn"),
        Arguments.of(HttpMethod.POST, "/api/fullmakt/123/avslag")
    )
}