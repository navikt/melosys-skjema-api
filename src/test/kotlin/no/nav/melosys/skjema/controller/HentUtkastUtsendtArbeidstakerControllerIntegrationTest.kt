package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.radgiverfirmaInfoMedDefaultVerdier
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.OrganisasjonDto
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.UtkastListeResponse
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import tools.jackson.databind.json.JsonMapper

/**
 * Integrasjonstester for henting av utkast.
 * Tester hele flyten fra controller til database.
 */
class HentUtkastUtsendtArbeidstakerControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockkBean
    private lateinit var altinnService: AltinnService

    @BeforeEach
    fun setUp() {
        clearMocks(altinnService)
        skjemaRepository.deleteAll()
    }

    private fun createTokenForUser(pid: String): String {
        return mockOAuth2Server.getToken(
            claims = mapOf("pid" to pid)
        )
    }

    @Test
    @DisplayName("Skal hente utkast for DEG_SELV")
    fun `skal hente utkast for DEG_SELV`() {
        val userFnr = korrektSyntetiskFnr
        val token = createTokenForUser(userFnr)

        // Opprett et utkast for brukeren
        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val skjema = skjemaMedDefaultVerdier(
            fnr = userFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadata,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjema)

        // Hent utkast
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 1
        response.utkast shouldHaveSize 1
        response.utkast[0].id shouldBe skjema.id
        // Verifiser at fnr er maskert (første 6 siffer + asterisker)
        response.utkast[0].arbeidstakerFnrMaskert.shouldNotBeNull()
        response.utkast[0].arbeidstakerFnrMaskert!!.length shouldBe 11
        response.utkast[0].arbeidstakerFnrMaskert!! shouldBe "${userFnr.take(6)}*****"
    }

    @Test
    @DisplayName("Skal returnere tom liste når ingen utkast finnes")
    fun `skal returnere tom liste når ingen utkast finnes`() {
        val userFnr = korrektSyntetiskFnr
        val token = createTokenForUser(userFnr)

        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 0
        response.utkast.shouldBeEmpty()
    }

    @Test
    @DisplayName("Skal hente utkast for ARBEIDSGIVER basert på Altinn-tilganger")
    fun `skal hente utkast for ARBEIDSGIVER basert på Altinn-tilganger`() {
        val userFnr = korrektSyntetiskFnr
        val orgnr1 = "111222333"
        val orgnr2 = "444555666"
        val token = createTokenForUser(userFnr)

        // Mock Altinn-tilganger
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr1, "Bedrift A AS", "AS"),
            OrganisasjonDto(orgnr2, "Bedrift B AS", "AS")
        )

        // Opprett utkast for organisasjon med tilgang
        val metadata1 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER,
            arbeidsgiverNavn = "Bedrift A AS"
        )
        val skjema1 = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = orgnr1,
            status = SkjemaStatus.UTKAST,
            metadata = metadata1,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjema1)

        // Opprett utkast for organisasjon uten tilgang (skal ikke returneres)
        val metadata2 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )
        val skjema2 = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = "777888999",
            status = SkjemaStatus.UTKAST,
            metadata = metadata2,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjema2)

        // Hent utkast
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=ARBEIDSGIVER")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 1
        response.utkast shouldHaveSize 1
        response.utkast[0].id shouldBe skjema1.id
        response.utkast[0].arbeidsgiverOrgnr shouldBe orgnr1
    }

    @Test
    @DisplayName("Skal hente utkast for RADGIVER basert på spesifikt rådgiverfirma")
    fun `skal hente utkast for RADGIVER basert på spesifikt rådgiverfirma`() {
        val userFnr = korrektSyntetiskFnr
        val radgiverfirmaOrgnr = "987654321"
        val token = createTokenForUser(userFnr)

        // Opprett utkast med rådgiverfirma i metadata
        val metadata1 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = radgiverfirmaOrgnr)
        )
        val skjema1 = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadata1,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjema1)

        // Opprett utkast med annet rådgiverfirma (skal ikke returneres)
        val metadata2 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = "111111111")
        )
        val skjema2 = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadata2,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjema2)

        // Hent utkast
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=RADGIVER&radgiverfirmaOrgnr=$radgiverfirmaOrgnr")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 1
        response.utkast shouldHaveSize 1
        response.utkast[0].id shouldBe skjema1.id
    }

    @Test
    @DisplayName("Skal ikke returnere utkast fra andre brukere")
    fun `skal ikke returnere utkast fra andre brukere`() {
        val userFnr = korrektSyntetiskFnr
        val annenBrukerFnr = etAnnetKorrektSyntetiskFnr
        val token = createTokenForUser(userFnr)

        // Opprett utkast for en annen bruker
        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val skjema = skjemaMedDefaultVerdier(
            fnr = annenBrukerFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadata,
            opprettetAv = annenBrukerFnr
        )
        skjemaRepository.save(skjema)

        // Hent utkast
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 0
        response.utkast.shouldBeEmpty()
    }

    @Test
    @DisplayName("Skal ikke returnere søknader med status SENDT")
    fun `skal ikke returnere søknader med status SENDT`() {
        val userFnr = korrektSyntetiskFnr
        val token = createTokenForUser(userFnr)

        // Opprett en SENDT søknad (skal ikke returneres som utkast)
        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val skjema = skjemaMedDefaultVerdier(
            fnr = userFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            metadata = metadata,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjema)

        // Hent utkast
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 0
        response.utkast.shouldBeEmpty()
    }

    @Test
    @DisplayName("Skal feile når radgiverfirmaOrgnr mangler for RADGIVER")
    fun `skal feile når radgiverfirmaOrgnr mangler for RADGIVER`() {
        val userFnr = korrektSyntetiskFnr
        val token = createTokenForUser(userFnr)

        // Forsøk å hente utkast uten radgiverfirmaOrgnr
        // Backend skal kaste IllegalArgumentException som resulterer i 400 BAD REQUEST
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=RADGIVER")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("Skal kun returnere utkast med riktig representasjonstype for DEG_SELV")
    fun `skal kun returnere utkast med riktig representasjonstype for DEG_SELV`() {
        val userFnr = korrektSyntetiskFnr
        val token = createTokenForUser(userFnr)

        // Opprett utkast med DEG_SELV
        val metadataDegSelv = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val skjemaDegSelv = skjemaMedDefaultVerdier(
            fnr = userFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadataDegSelv,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjemaDegSelv)

        // Opprett utkast med ARBEIDSGIVER (skal ikke returneres for DEG_SELV-query)
        val metadataArbeidsgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )
        val skjemaArbeidsgiver = skjemaMedDefaultVerdier(
            fnr = userFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadataArbeidsgiver,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjemaArbeidsgiver)

        // Hent utkast for DEG_SELV
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 1
        response.utkast shouldHaveSize 1
        response.utkast[0].id shouldBe skjemaDegSelv.id
    }

    @Test
    @DisplayName("Skal håndtere flere utkast for samme bruker")
    fun `skal håndtere flere utkast for samme bruker`() {
        val userFnr = korrektSyntetiskFnr
        val token = createTokenForUser(userFnr)

        // Opprett 3 utkast for samme bruker
        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV
        )
        repeat(3) {
            val skjema = skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.UTKAST,
                metadata = metadata,
                opprettetAv = userFnr
            )
            skjemaRepository.save(skjema)
        }

        // Hent utkast
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 3
        response.utkast shouldHaveSize 3
    }

    @Test
    @DisplayName("Skal kreve autentisering")
    fun `skal kreve autentisering`() {
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=DEG_SELV")
            // Ingen Authorization header
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Skal kun returnere utkast med riktig representasjonstype for ARBEIDSGIVER")
    fun `skal kun returnere utkast med riktig representasjonstype for ARBEIDSGIVER`() {
        val userFnr = korrektSyntetiskFnr
        val orgnr = "111222333"
        val token = createTokenForUser(userFnr)

        // Mock Altinn-tilganger
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr, "Bedrift A AS", "AS")
        )

        // Opprett utkast med ARBEIDSGIVER
        val metadataArbeidsgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER,
            arbeidsgiverNavn = "Bedrift A AS"
        )
        val skjemaArbeidsgiver = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = orgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadataArbeidsgiver,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjemaArbeidsgiver)

        // Opprett utkast med DEG_SELV for samme orgnr (skal ikke returneres)
        val metadataDegSelv = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val skjemaDegSelv = skjemaMedDefaultVerdier(
            fnr = userFnr,
            orgnr = orgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadataDegSelv,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjemaDegSelv)

        // Hent utkast for ARBEIDSGIVER
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=ARBEIDSGIVER")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 1
        response.utkast shouldHaveSize 1
        response.utkast[0].id shouldBe skjemaArbeidsgiver.id
    }

    @Test
    @DisplayName("Skal kun returnere utkast med riktig representasjonstype for RADGIVER")
    fun `skal kun returnere utkast med riktig representasjonstype for RADGIVER`() {
        val userFnr = korrektSyntetiskFnr
        val radgiverfirmaOrgnr = "987654321"
        val token = createTokenForUser(userFnr)

        // Opprett utkast med RADGIVER
        val metadataRadgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = radgiverfirmaOrgnr)
        )
        val skjemaRadgiver = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadataRadgiver,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjemaRadgiver)

        // Opprett utkast med ARBEIDSGIVER for samme rådgiverfirma (skal ikke returneres)
        val metadataArbeidsgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER,
            radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = radgiverfirmaOrgnr)
        )
        val skjemaArbeidsgiver = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            metadata = metadataArbeidsgiver,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjemaArbeidsgiver)

        // Hent utkast for RADGIVER
        val response = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/utkast?representasjonstype=RADGIVER&radgiverfirmaOrgnr=$radgiverfirmaOrgnr")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<UtkastListeResponse>()
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        response.antall shouldBe 1
        response.utkast shouldHaveSize 1
        response.utkast[0].id shouldBe skjemaRadgiver.id
    }
}
