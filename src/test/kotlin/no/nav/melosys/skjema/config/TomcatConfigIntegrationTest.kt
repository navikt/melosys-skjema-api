package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.ApiTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient

class TomcatConfigIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `request med mange språk i Accept-Language avvises ikke`() {
        val mangeSpråk = listOf("nb", "nn", "no", "en", "en-US", "en-GB", "sv", "da", "de", "fr", "es", "it")
            .joinToString(",")

        webTestClient.get()
            .uri("/actuator/health")
            .header(HttpHeaders.ACCEPT_LANGUAGE, mangeSpråk)
            .exchange()
            .expectStatus().isOk
    }
}
