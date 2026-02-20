package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.controller.dto.VedleggResponse
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.integrasjon.clamav.ClamAvClient
import no.nav.melosys.skjema.integrasjon.storage.VedleggStorageClient
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.repository.VedleggRepository
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.service.NotificationService
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

class VedleggControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var vedleggRepository: VedleggRepository

    @MockkBean
    private lateinit var notificationService: NotificationService

    @MockkBean
    private lateinit var altinnService: AltinnService

    @MockkBean
    private lateinit var clamAvClient: ClamAvClient

    @MockkBean
    private lateinit var vedleggStorageClient: VedleggStorageClient

    private val pid = "12345678901"

    private fun getToken(): String {
        return mockOAuth2Server.issueToken(
            issuerId = "tokenx",
            audience = "test-client-id",
            claims = mapOf("pid" to pid)
        ).serialize()
    }

    private fun opprettSkjema(): Skjema {
        return skjemaRepository.save(
            Skjema(
                status = SkjemaStatus.UTKAST,
                fnr = pid,
                orgnr = "123456789",
                metadata = DegSelvMetadata(
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    arbeidsgiverNavn = "Test AS",
                    juridiskEnhetOrgnr = "123456789"
                ),
                opprettetAv = pid,
                endretAv = pid
            )
        )
    }

    @BeforeEach
    fun setup() {
        vedleggRepository.deleteAll()
        skjemaRepository.deleteAll()
        every { clamAvClient.scan(any()) } just Runs
        every { vedleggStorageClient.lastOpp(any(), any(), any()) } just Runs
        every { vedleggStorageClient.slett(any()) } just Runs
        every { altinnService.harBrukerTilgang(any()) } returns false
    }

    @Test
    fun `lastOpp vedlegg - returnerer 200 med VedleggResponse`() {
        val skjema = opprettSkjema()
        val token = getToken()

        val pdfBytes = "%PDF-1.4 test content".toByteArray()
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("fil", object : ByteArrayResource(pdfBytes) {
            override fun getFilename() = "test.pdf"
        }).contentType(MediaType.APPLICATION_PDF)

        val response = webTestClient.post()
            .uri("/api/skjema/${skjema.id}/vedlegg")
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .exchange()
            .expectStatus().isOk
            .expectBody(VedleggResponse::class.java)
            .returnResult()
            .responseBody

        response shouldNotBe null
        response!!.filnavn shouldBe "test.pdf"
        response.filtype.name shouldBe "PDF"
    }

    @Test
    fun `hentVedlegg - returnerer tom liste uten vedlegg`() {
        val skjema = opprettSkjema()
        val token = getToken()

        webTestClient.get()
            .uri("/api/skjema/${skjema.id}/vedlegg")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(VedleggResponse::class.java)
            .hasSize(0)
    }

    @Test
    fun `lastOpp og slett vedlegg`() {
        val skjema = opprettSkjema()
        val token = getToken()

        val pdfBytes = "%PDF-1.4 test content".toByteArray()
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("fil", object : ByteArrayResource(pdfBytes) {
            override fun getFilename() = "test.pdf"
        }).contentType(MediaType.APPLICATION_PDF)

        // Last opp
        val vedlegg = webTestClient.post()
            .uri("/api/skjema/${skjema.id}/vedlegg")
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .exchange()
            .expectStatus().isOk
            .expectBody(VedleggResponse::class.java)
            .returnResult()
            .responseBody!!

        // Slett
        webTestClient.delete()
            .uri("/api/skjema/${skjema.id}/vedlegg/${vedlegg.id}")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isNoContent

        // Verifiser at listen er tom
        webTestClient.get()
            .uri("/api/skjema/${skjema.id}/vedlegg")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(VedleggResponse::class.java)
            .hasSize(0)
    }

    @Test
    fun `lastOpp vedlegg - returnerer 403 uten tilgang`() {
        val skjema = skjemaRepository.save(
            Skjema(
                status = SkjemaStatus.UTKAST,
                fnr = "99999999999",
                orgnr = "123456789",
                metadata = DegSelvMetadata(
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    arbeidsgiverNavn = "Test AS",
                    juridiskEnhetOrgnr = "123456789"
                ),
                opprettetAv = "99999999999",
                endretAv = "99999999999"
            )
        )
        val token = getToken()

        val pdfBytes = "%PDF-1.4 test content".toByteArray()
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("fil", object : ByteArrayResource(pdfBytes) {
            override fun getFilename() = "test.pdf"
        }).contentType(MediaType.APPLICATION_PDF)

        webTestClient.post()
            .uri("/api/skjema/${skjema.id}/vedlegg")
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `lastOpp vedlegg - returnerer 404 for ukjent skjema`() {
        val token = getToken()
        val ukjentId = UUID.randomUUID()

        val pdfBytes = "%PDF-1.4 test content".toByteArray()
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("fil", object : ByteArrayResource(pdfBytes) {
            override fun getFilename() = "test.pdf"
        }).contentType(MediaType.APPLICATION_PDF)

        webTestClient.post()
            .uri("/api/skjema/$ukjentId/vedlegg")
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .exchange()
            .expectStatus().isNotFound
    }
}
