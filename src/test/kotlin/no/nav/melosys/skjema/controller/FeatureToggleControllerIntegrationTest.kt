package no.nav.melosys.skjema.controller

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.featuretoggle.ToggleNavn
import no.nav.melosys.skjema.getToken
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class FeatureToggleControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Test
    fun `skal returnere evaluerte toggles for angitte features`() {
        val token = mockOAuth2Server.getToken(claims = mapOf("pid" to "12345678901"))

        val toggles = webTestClient.get()
            .uri("/api/featuretoggle?features=${ToggleNavn.MOTPART_CTA}&features=${ToggleNavn.INNSENDT_SAMMENDRAG}")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<Map<String, Boolean>>()
            .returnResult().responseBody.shouldNotBeNull()

        // Test-profilen bruker FakeUnleash med alle toggles på
        toggles shouldBe mapOf(
            ToggleNavn.MOTPART_CTA to true,
            ToggleNavn.INNSENDT_SAMMENDRAG to true
        )
    }

    @Test
    fun `skal filtrere bort ukjente toggle-navn`() {
        val token = mockOAuth2Server.getToken(claims = mapOf("pid" to "12345678901"))

        val toggles = webTestClient.get()
            .uri("/api/featuretoggle?features=${ToggleNavn.MOTPART_CTA}&features=melosys.annet.system.toggle")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<Map<String, Boolean>>()
            .returnResult().responseBody.shouldNotBeNull()

        toggles shouldBe mapOf(ToggleNavn.MOTPART_CTA to true)
    }

    @Test
    fun `skal returnere tomt map naar features-param mangler`() {
        val token = mockOAuth2Server.getToken(claims = mapOf("pid" to "12345678901"))

        webTestClient.get()
            .uri("/api/featuretoggle")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<Map<String, Boolean>>()
            .returnResult().responseBody shouldBe emptyMap()
    }
}
