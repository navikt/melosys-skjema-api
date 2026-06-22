package no.nav.melosys.skjema.config

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.skjema.ApiTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

class OpenApiGruppeIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `full spec paa v3 api-docs inneholder bruker-endepunkter (brukes av frontend-typegen)`() {
        val spec = webTestClient.get().uri("/v3/api-docs")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!

        spec shouldContain "/api/skjema"
    }

    @Test
    fun `admin-gruppe paa v3 api-docs admin inneholder kun admin-endepunkter`() {
        val spec = webTestClient.get().uri("/v3/api-docs/admin")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!

        spec shouldContain "/admin/statistikk"
        spec shouldNotContain "/api/skjema"
    }
}
