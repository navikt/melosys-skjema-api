package no.nav.melosys.skjema.config

import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import no.nav.melosys.skjema.ApiTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

class SwaggerConfigIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `api-docs inneholder ikke null i type-arrays`() {
        val apiDocs = webTestClient.get()
            .uri("/v3/api-docs")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        // Lagre til disk for å kunne generere TypeScript-typer offline (uten
        // dev-deploy). Frontend kan peke `pnpm generate-types --path` til denne.
        Path.of("build/api-docs.json").apply {
            createParentDirectories()
            writeText(apiDocs)
        }

        // OpenAPI 3.1 ville representert nullable felter som "type":["string","null"].
        // OpenApiCustomizer i SwaggerConfig fjerner "null" fra alle slike type-arrays.
        apiDocs shouldNotContain "\"null\""
    }
}
