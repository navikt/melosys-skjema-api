package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.controller.dto.PersonMedFullmaktDto
import no.nav.melosys.skjema.fullmaktMedDefaultVerdier
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.integrasjon.pdl.PdlConsumer
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlFoedselsdato
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlNavn
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson
import no.nav.melosys.skjema.integrasjon.repr.ReprConsumer
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Integrasjonstest for RepresentasjonController med MockOAuth2Server og WebTestClient.
 * Tester /api/representasjon endepunktet.
 */
class RepresentasjonControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var reprConsumer: ReprConsumer

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    @Test
    @DisplayName("GET /api/representasjon skal returnere fullmakter")
    fun `GET representasjon skal returnere fullmakter`() {
        clearMocks(reprConsumer)

        val fullmakter = listOf(
            fullmaktMedDefaultVerdier().copy(
                leserettigheter = listOf("MED", "DAG")
            ),
            fullmaktMedDefaultVerdier().copy(
                fullmaktsgiver = "11111111111",
                skriverettigheter = emptyList()
            )
        )

        every { reprConsumer.hentKanRepresentere() } returns fullmakter

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.get()
            .uri("/api/representasjon")
            .header("Authorization", "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<Fullmakt>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                response.responseBody!!.shouldHaveSize(2)
                response.responseBody!![0].fullmaktsgiver shouldBe "12345678901"
                response.responseBody!![1].fullmaktsgiver shouldBe "11111111111"
            }
    }

    @Test
    @DisplayName("GET /api/representasjon skal returnere tom liste når ingen fullmakter")
    fun `GET representasjon skal returnere tom liste når ingen fullmakter`() {
        clearMocks(reprConsumer)

        every { reprConsumer.hentKanRepresentere() } returns emptyList()

        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.get()
            .uri("/api/representasjon")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<Fullmakt>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                response.responseBody!!.shouldHaveSize(0)
            }
    }

    @Test
    @DisplayName("GET /api/representasjon skal returnere 401 uten autentisering")
    fun `GET representasjon skal returnere 401 uten autentisering`() {
        webTestClient.get()
            .uri("/api/representasjon")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Skal filtrere bort fullmakter uten MED-området")
    fun `skal filtrere bort fullmakter uten MED-området`() {
        clearMocks(reprConsumer)

        val fullmakter = listOf(
            fullmaktMedDefaultVerdier(),
            fullmaktMedDefaultVerdier().copy(
                fullmaktsgiver = "22222222222",
                leserettigheter = listOf("DAG", "FOS"),
                skriverettigheter = listOf("DAG")
            )
        )

        every { reprConsumer.hentKanRepresentere() } returns fullmakter

        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.get()
            .uri("/api/representasjon")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody<List<Fullmakt>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                // Kun fullmakten med MED skal returneres
                response.responseBody!!.shouldHaveSize(1)
                response.responseBody!![0].fullmaktsgiver shouldBe "12345678901"
            }
    }

    @Test
    @DisplayName("GET /api/representasjon/personer skal returnere personer med fullmakt beriket med PDL-data")
    fun `GET personer skal returnere personer med fullmakt beriket med PDL-data`() {
        clearMocks(reprConsumer, pdlConsumer)

        val fullmakter = listOf(
            fullmaktMedDefaultVerdier().copy(fullmaktsgiver = "12345678901"),
            fullmaktMedDefaultVerdier().copy(fullmaktsgiver = "11111111111")
        )

        val pdlPersoner = mapOf(
            "12345678901" to PdlPerson(
                navn = listOf(PdlNavn("Ola", null, "Nordmann")),
                foedselsdato = listOf(PdlFoedselsdato("1990-01-01"))
            ),
            "11111111111" to PdlPerson(
                navn = listOf(PdlNavn("Kari", "Anne", "Hansen")),
                foedselsdato = listOf(PdlFoedselsdato("1985-05-15"))
            )
        )

        every { reprConsumer.hentKanRepresentere() } returns fullmakter
        every { pdlConsumer.hentPersonerBolk(listOf("12345678901", "11111111111")) } returns pdlPersoner

        val accessToken = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.get()
            .uri("/api/representasjon/personer")
            .header("Authorization", "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<PersonMedFullmaktDto>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                response.responseBody!!.shouldHaveSize(2)

                response.responseBody!![0].fnr shouldBe "12345678901"
                response.responseBody!![0].navn shouldBe "Ola Nordmann"
                response.responseBody!![0].fodselsdato.toString() shouldBe "1990-01-01"

                response.responseBody!![1].fnr shouldBe "11111111111"
                response.responseBody!![1].navn shouldBe "Kari Anne Hansen"
                response.responseBody!![1].fodselsdato.toString() shouldBe "1985-05-15"
            }
    }

    @Test
    @DisplayName("GET /api/representasjon/personer skal returnere tom liste når ingen fullmakter")
    fun `GET personer skal returnere tom liste når ingen fullmakter`() {
        clearMocks(reprConsumer, pdlConsumer)

        every { reprConsumer.hentKanRepresentere() } returns emptyList()

        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.get()
            .uri("/api/representasjon/personer")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody<List<PersonMedFullmaktDto>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                response.responseBody!!.shouldHaveSize(0)
            }
    }

    @Test
    @DisplayName("GET /api/representasjon/personer skal returnere 401 uten autentisering")
    fun `GET personer skal returnere 401 uten autentisering`() {
        webTestClient.get()
            .uri("/api/representasjon/personer")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("GET /api/representasjon/personer skal filtrere bort personer som ikke finnes i PDL")
    fun `GET personer skal filtrere bort personer som ikke finnes i PDL`() {
        clearMocks(reprConsumer, pdlConsumer)

        val fullmakter = listOf(
            fullmaktMedDefaultVerdier().copy(fullmaktsgiver = "12345678901"),
            fullmaktMedDefaultVerdier().copy(fullmaktsgiver = "11111111111")
        )

        // Kun én person finnes i PDL
        val pdlPersoner = mapOf(
            "12345678901" to PdlPerson(
                navn = listOf(PdlNavn("Ola", null, "Nordmann")),
                foedselsdato = listOf(PdlFoedselsdato("1990-01-01"))
            )
        )

        every { reprConsumer.hentKanRepresentere() } returns fullmakter
        every { pdlConsumer.hentPersonerBolk(listOf("12345678901", "11111111111")) } returns pdlPersoner

        val token = mockOAuth2Server.getToken(
            claims = mapOf("pid" to "98765432109")
        )

        webTestClient.get()
            .uri("/api/representasjon/personer")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody<List<PersonMedFullmaktDto>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                // Kun personen som finnes i PDL skal returneres
                response.responseBody!!.shouldHaveSize(1)
                response.responseBody!![0].fnr shouldBe "12345678901"
            }
    }
}
