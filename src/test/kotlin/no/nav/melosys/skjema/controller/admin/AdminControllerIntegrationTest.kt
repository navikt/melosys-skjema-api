package no.nav.melosys.skjema.controller.admin

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.adminTokenMedTilgang
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.m2mTokenWithoutAccess
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.InnsendingService
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import no.nav.security.mock.oauth2.MockOAuth2Server

class AdminControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @MockkBean(relaxed = true)
    private lateinit var innsendingService: InnsendingService

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
    }

    private fun lagFeiletInnsending(referanseId: String = "FEIL01") =
        skjemaRepository.save(
            skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
        ).let { skjema ->
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    status = InnsendingStatus.KAFKA_FEILET,
                    antallForsok = 3,
                    feilmelding = "Kafka utilgjengelig",
                    referanseId = referanseId
                )
            )
        }

    @Nested
    @DisplayName("Sikkerhet")
    inner class Sikkerhet {

        @Test
        fun `skal returnere 401 naar token mangler`() {
            webTestClient.get().uri("/admin/statistikk")
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            webTestClient.get().uri("/admin/statistikk")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .exchange()
                .expectStatus().isForbidden
        }
    }

    @Nested
    @DisplayName("GET /admin/statistikk")
    inner class Statistikk {

        @Test
        fun `skal returnere antall per status`() {
            lagFeiletInnsending()

            val body = webTestClient.get().uri("/admin/statistikk")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<AdminStatistikkDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.skjemaPerStatus[SkjemaStatus.SENDT] shouldBe 1
            body.innsendingPerStatus[InnsendingStatus.KAFKA_FEILET] shouldBe 1
            body.antallFeiledeInnsendinger shouldBe 1
        }
    }

    @Nested
    @DisplayName("GET /admin/innsendinger/feilede")
    inner class FeiledeInnsendinger {

        @Test
        fun `skal returnere feilede innsendinger uten personopplysninger`() {
            val innsending = lagFeiletInnsending()

            val body = webTestClient.get().uri("/admin/innsendinger/feilede")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<List<InnsendingAdminDto>>()
                .returnResult().responseBody.shouldNotBeNull()

            body shouldHaveSize 1
            body[0].innsendingId shouldBe innsending.id
            body[0].status shouldBe InnsendingStatus.KAFKA_FEILET
            body[0].feilmelding shouldBe "Kafka utilgjengelig"
            body[0].antallForsok shouldBe 3
        }

        @Test
        fun `skal returnere antall feilede`() {
            lagFeiletInnsending("FEIL01")
            lagFeiletInnsending("FEIL02")

            val body = webTestClient.get().uri("/admin/innsendinger/feilede/antall")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<AntallDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.antall shouldBe 2
        }
    }

    @Nested
    @DisplayName("GET /admin/innsendinger/{id}")
    inner class HentInnsending {

        @Test
        fun `skal returnere innsending`() {
            val innsending = lagFeiletInnsending()

            webTestClient.get().uri("/admin/innsendinger/${innsending.id}")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<InnsendingAdminDto>()
                .returnResult().responseBody.shouldNotBeNull()
                .innsendingId shouldBe innsending.id
        }

        @Test
        fun `skal returnere 404 naar innsending ikke finnes`() {
            webTestClient.get().uri("/admin/innsendinger/${UUID.randomUUID()}")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("POST /admin/innsendinger/{id}/retry")
    inner class RetryInnsending {

        @Test
        fun `skal reprosessere innsending og returnere 200`() {
            val innsending = lagFeiletInnsending()
            val skjemaId = innsending.skjema.id!!

            webTestClient.post().uri("/admin/innsendinger/${innsending.id}/retry")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk

            verify(exactly = 1) { innsendingService.prosesserInnsending(skjemaId) }
        }

        @Test
        fun `skal returnere 404 naar innsending ikke finnes`() {
            webTestClient.post().uri("/admin/innsendinger/${UUID.randomUUID()}/retry")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("POST /admin/innsendinger/retry-feilede")
    inner class RetryAlleFeilede {

        @Test
        fun `skal reprosessere alle feilede og returnere antall`() {
            val innsending1 = lagFeiletInnsending("FEIL01")
            val innsending2 = lagFeiletInnsending("FEIL02")
            every { innsendingService.prosesserInnsending(any()) } returns Unit

            val body = webTestClient.post().uri("/admin/innsendinger/retry-feilede")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<RetryResultatDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.antallForsoekt shouldBe 2
            body.antallFeilet shouldBe 0
            verify(exactly = 1) { innsendingService.prosesserInnsending(innsending1.skjema.id!!) }
            verify(exactly = 1) { innsendingService.prosesserInnsending(innsending2.skjema.id!!) }
        }
    }
}
