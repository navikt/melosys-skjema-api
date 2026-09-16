package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.exception.UtdatertSkjemaDefinisjonVersjonException
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient

class SkjemaVersjonssjekkIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @MockkBean
    private lateinit var altinnService: AltinnService

    @MockkBean
    private lateinit var eregService: EregService

    @BeforeEach
    fun setUp() {
        clearMocks(altinnService, eregService)
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
        every { altinnService.harBrukerTilgang(any()) } returns true
        every { altinnService.hentBrukersTilganger() } returns emptyList()
        every { eregService.organisasjonsnummerEksisterer(any()) } returns true
    }

    @Test
    fun `manglende versjonsheader gir 409 og ikke 400`() {
        val skjema = lagreUtkast()

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id}")
            .header("Authorization", "Bearer ${token(skjema.fnr)}")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.error").isEqualTo(UtdatertSkjemaDefinisjonVersjonException.ERROR_CODE)
    }

    @Test
    fun `utdatert versjonsheader gir 409`() {
        val skjema = lagreUtkast()

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id}")
            .header("Authorization", "Bearer ${token(skjema.fnr)}")
            .header(SKJEMA_DEFINISJON_VERSJON_HEADER, "1")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.error").isEqualTo(UtdatertSkjemaDefinisjonVersjonException.ERROR_CODE)
    }

    @Test
    fun `utdatert klient reinitialiserer ikke utkastet`() {
        val skjema = lagreUtkast(skjemaDefinisjonVersjon = "1")

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id}")
            .header("Authorization", "Bearer ${token(skjema.fnr)}")
            .exchange()
            .expectStatus().isEqualTo(409)

        val uberørt = skjemaRepository.findByIdOrNull(skjema.id!!).shouldNotBeNull()
        uberørt.skjemaDefinisjonVersjon shouldBe "1"
        uberørt.data.shouldNotBeNull()
    }

    @Test
    fun `klient på aktiv versjon slipper gjennom`() {
        val skjema = lagreUtkast()

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id}")
            .header("Authorization", "Bearer ${token(skjema.fnr)}")
            .header(SKJEMA_DEFINISJON_VERSJON_HEADER, "2")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `skjemadefinisjonens versjon leses uten header`() {
        webTestClient.get()
            .uri("/api/skjema/definisjon/UTSENDT_ARBEIDSTAKER/versjon")
            .header("Authorization", "Bearer ${token(korrektSyntetiskFnr)}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.aktivVersjon").isEqualTo("2")
    }

    @Test
    fun `utkastlista leses uten header`() {
        lagreUtkast()

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            .header("Authorization", "Bearer ${token(korrektSyntetiskFnr)}")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `innsendt skjema hentes med sin historiske versjon uten header`() {
        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                status = SkjemaStatus.SENDT,
                data = arbeidstakersSkjemaDataDtoMedDefaultVerdier(),
                skjemaDefinisjonVersjon = "1"
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = skjema,
                status = InnsendingStatus.MOTTATT,
                skjemaDefinisjonVersjon = "1"
            )
        )

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id}/innsendt")
            .header("Authorization", "Bearer ${token(skjema.fnr)}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.skjemaDefinisjonVersjon").isEqualTo("1")
            .jsonPath("$.definisjon.versjon").isEqualTo("1")
    }

    @Test
    fun `innsendt skjema hentes via GET id selv med utdatert header`() {
        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                status = SkjemaStatus.SENDT,
                data = arbeidstakersSkjemaDataDtoMedDefaultVerdier(),
                skjemaDefinisjonVersjon = "1"
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = skjema,
                status = InnsendingStatus.MOTTATT,
                skjemaDefinisjonVersjon = "1"
            )
        )

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id}")
            .header("Authorization", "Bearer ${token(skjema.fnr)}")
            .header(SKJEMA_DEFINISJON_VERSJON_HEADER, "1")
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id}")
            .header("Authorization", "Bearer ${token(skjema.fnr)}")
            .exchange()
            .expectStatus().isOk
    }

    private fun lagreUtkast(skjemaDefinisjonVersjon: String = "2"): Skjema =
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                status = SkjemaStatus.UTKAST,
                data = arbeidstakersSkjemaDataDtoMedDefaultVerdier(),
                skjemaDefinisjonVersjon = skjemaDefinisjonVersjon
            )
        )

    private fun token(pid: String): String = mockOAuth2Server.getToken(claims = mapOf("pid" to pid))
}
