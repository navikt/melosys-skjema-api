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
        // SkjemaController - new opprett-med-kontekst endpoints
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/opprett-med-kontekst"),
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/123/arbeidsgiver-view"),
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/123/arbeidstaker-view"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/123/submit"),
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/123/pdf"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/123/arbeidstakeren"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/123/arbeidsgiverens-virksomhet-i-norge"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/123/utenlandsoppdraget"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/123/arbeidstakerens-lonn"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/123/arbeidssted-i-utlandet"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/123/tilleggsopplysninger"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/123/submit"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/123/utenlandsoppdraget"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/123/arbeidssituasjon"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/123/skatteforhold-og-inntekt"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/123/familiemedlemmer"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/123/tilleggsopplysninger"),

        // PrefillController
        Arguments.of(HttpMethod.POST, "/api/preutfyll/person"),
        Arguments.of(HttpMethod.GET, "/api/preutfyll/org/123456789"),

        // RepresentasjonController
        Arguments.of(HttpMethod.GET, "/api/representasjon"),

        // AltinnController
        Arguments.of(HttpMethod.GET, "/api/hentTilganger"),
        Arguments.of(HttpMethod.GET, "/api/harTilgang/123456789")
    )
}