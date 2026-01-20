package no.nav.melosys.skjema.controller

import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.m2mTokenWithoutAccess
import no.nav.melosys.skjema.m2mTokenWithReadSkjemaDataAccess
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

class M2MSkjemaControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    private lateinit var testSkjema: Skjema

    @BeforeEach
    fun setUp() {
        testSkjema = skjemaRepository.save(
            Skjema(
                status = SkjemaStatus.SENDT,
                fnr = "12345678901",
                orgnr = "123456789",
                opprettetAv = "12345678901",
                endretAv = "12345678901"
            )
        )
    }

    @Nested
    @DisplayName("GET /m2m/api/skjema/{id}")
    inner class GetSkjema {

        @Test
        fun `skal returnere skjema når gyldig M2M-token med tillatt klient`() {
            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            webTestClient.get()
                .uri("/m2m/api/skjema/${testSkjema.id}/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `skal returnere 403 når azp ikke matcher tillatt klient`() {
            val token = mockOAuth2Server.m2mTokenWithoutAccess()

            webTestClient.get()
                .uri("/m2m/api/skjema/${testSkjema.id}/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `skal returnere 401 når token mangler`() {
            webTestClient.get()
                .uri("/m2m/api/skjema/${testSkjema.id}/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `skal returnere 401 når TokenX-token brukes i stedet for Azure`() {
            val tokenXToken = mockOAuth2Server.getToken(
                claims = mapOf("pid" to "12345678901")
            )

            webTestClient.get()
                .uri("/m2m/api/skjema/${testSkjema.id}/data")
                .header("Authorization", "Bearer $tokenXToken")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `skal returnere 404 når skjema ikke finnes`() {
            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()
            val ukjentId = UUID.randomUUID()

            webTestClient.get()
                .uri("/m2m/api/skjema/$ukjentId/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
        }
    }
}
