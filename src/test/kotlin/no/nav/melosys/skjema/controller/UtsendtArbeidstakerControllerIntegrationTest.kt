package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.clearMocks
import io.mockk.every
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidssituasjonDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakerensLonnDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.controller.dto.ErrorResponse
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
import no.nav.melosys.skjema.familiemedlemmerDtoMedDefaultVerdier
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedDefaultVerdier
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.service.NotificationService
import no.nav.melosys.skjema.skatteforholdOgInntektDtoMedDefaultVerdier
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.tilleggsopplysningerDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.SkjemaInnsendtKvittering
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

data class SkjemaStegTestFixture<T>(
    val stepKey: String = "",
    val uri: String = "",
    val requestBody: Any,
    val dataBeforePost: T? = null,
    val expectedDataAfterPost: T? = null,
    val httpMethod: HttpMethod? = null,
    val expectedValidationError: Map<String, String>? = null
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtsendtArbeidstakerControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @MockkBean
    private lateinit var notificationService: NotificationService

    @MockkBean
    private lateinit var altinnService: AltinnService

    @MockkBean
    private lateinit var eregService: EregService


    @BeforeEach
    fun setUp() {
        clearMocks(notificationService)
        clearMocks(altinnService)
        clearMocks(eregService)
        skjemaRepository.deleteAll()

        every { altinnService.harBrukerTilgang(any()) } returns true
        every { altinnService.hentBrukersTilganger() } returns emptyList()
        every { notificationService.sendNotificationToArbeidstaker(any(), any()) } returns Unit
        every {
            notificationService.sendNotificationToArbeidsgiver(
                any(),
                any(),
                any(),
                any()
            )
        } returns "test-beskjed-id"
        every { eregService.organisasjonsnummerEksisterer(any()) } returns true
    }

    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker skal returnere liste over brukerens skjemaer")
    fun `GET skjema skal returnere liste over brukerens skjemaer`() {
        val brukersFnr = korrektSyntetiskFnr
        val enAnnenBrukersFnr = etAnnetKorrektSyntetiskFnr
        val skjema1 = skjemaMedDefaultVerdier(fnr = brukersFnr, status = SkjemaStatus.UTKAST)
        val skjema2 = skjemaMedDefaultVerdier(fnr = enAnnenBrukersFnr, status = SkjemaStatus.SENDT)
        val skjema3 = skjemaMedDefaultVerdier(fnr = brukersFnr, status = SkjemaStatus.UTKAST)

        skjemaRepository.saveAll(listOf(skjema1, skjema2, skjema3))

        val token = createTokenForUser(brukersFnr)
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<UtsendtArbeidstakerSkjemaDto>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody.shouldHaveSize(2)
            }
    }


    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id}/arbeidstaker-view skal returnere spesifikt skjema")
    fun `GET skjema som arbeidstaker by id skal returnere spesifikt skjema`() {
        val skjemaData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                status = SkjemaStatus.UTKAST,
                data = skjemaData
            )
        )

        val token = createTokenForUser(savedSkjema.fnr)

        val responseBody = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/arbeidstaker-view")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<UtsendtArbeidstakerSkjemaDto>()
            .returnResult().responseBody

        responseBody.run {
            this.shouldNotBeNull()
            this.id shouldBe savedSkjema.id!!
            this.fnr shouldBe savedSkjema.fnr
            this.status shouldBe SkjemaStatus.UTKAST
            (this.data as UtsendtArbeidstakerArbeidstakersSkjemaDataDto) shouldBe skjemaData
        }
    }

    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id}/arbeidsgiver-view skal returnere spesifikt skjema")
    fun `GET skjema som arbeidsgiver by id skal returnere spesifikt skjema`() {
        val skjemaData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.UTKAST,
                data = skjemaData
            )
        )

        val token = createTokenForUser(korrektSyntetiskFnr)

        val responseBody = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/arbeidsgiver-view")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<UtsendtArbeidstakerSkjemaDto>()
            .returnResult().responseBody

        responseBody.run {
            this.shouldNotBeNull()
            this.id shouldBe savedSkjema.id!!
            this.orgnr shouldBe savedSkjema.orgnr
            this.status shouldBe savedSkjema.status
            (this.data as UtsendtArbeidstakerArbeidsgiversSkjemaDataDto) shouldBe skjemaData
        }
    }

    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id}/arbeidstaker-view skal returnere 404 for ikke-eksisterende skjema")
    fun `GET skjema by id skal returnere 404 for ikke-eksisterende skjema`() {
        val token = createTokenForUser(korrektSyntetiskFnr)
        val nonExistentId = UUID.randomUUID()

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/$nonExistentId/arbeidstaker-view")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id}/pdf skal returnere PDF response")
    fun `GET pdf skal returnere PDF response`() {
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = savedSkjema,
                referanseId = UUID.randomUUID().toString().take(6).uppercase()
            )
        )

        val token = createTokenForUser(savedSkjema.fnr)

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/pdf")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arbeidsgiverStegTestFixtures")
    @DisplayName("POST arbeidsgiver steg endpoints skal lagre data korrekt")
    fun `POST arbeidsgiver steg endpoints skal lagre data korrekt`(fixture: SkjemaStegTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>) {
        val existingSkjemaBeforePOST = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.UTKAST,
                data = fixture.dataBeforePost
            )
        )

        val token = createTokenForUser(korrektSyntetiskFnr)

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${existingSkjemaBeforePOST.id}/${fixture.stepKey}")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fixture.requestBody)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)

        val persistedSkjemaDataAfterPOST = skjemaRepository.getReferenceById(existingSkjemaBeforePOST.id!!).data as UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
        persistedSkjemaDataAfterPOST shouldBe fixture.expectedDataAfterPost
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arbeidstakerStegTestFixtures")
    @DisplayName("POST arbeidstaker steg endpoints skal lagre data korrekt")
    fun `POST arbeidstaker steg endpoints skal lagre data korrekt`(fixture: SkjemaStegTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>) {
        val existingSkjemaBeforePOST = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                status = SkjemaStatus.UTKAST,
                data = fixture.dataBeforePost
            )
        )

        val token = createTokenForUser(existingSkjemaBeforePOST.fnr!!)

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker/${existingSkjemaBeforePOST.id}/${fixture.stepKey}")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fixture.requestBody)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)

        val persistedSkjemaDataAfterPOST = skjemaRepository.getReferenceById(existingSkjemaBeforePOST.id!!).data as UtsendtArbeidstakerArbeidstakersSkjemaDataDto
        persistedSkjemaDataAfterPOST shouldBe fixture.expectedDataAfterPost
    }

    @Test
    @DisplayName("Get /api/skjema/utsendt-arbeidstaker/{id}/arbeidsgiver-view skal ikke kunne aksessere andres skjemaer")
    fun `Get skjema som arbeidsgiver skal ikke kunne aksessere andres skjemaer`() {
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                orgnr = korrektSyntetiskOrgnr,
                fnr = etAnnetKorrektSyntetiskFnr,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER
                )
            )
        )

        val token = createTokenForUser(korrektSyntetiskFnr)

        every { altinnService.harBrukerTilgang(savedSkjema.orgnr) } returns false


        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/arbeidsgiver-view")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isForbidden
    }

    fun orgnummerHarVerdiOgOrgnnummerErNull(): List<Arguments> = listOf(
        Arguments.of("123456789"),
        Arguments.of(null)
    )

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("arbeidsgiverEndpointsSomKreverTilgang")
    @DisplayName("Arbeidsgiver endpoints skal returnere 403 når bruker ikke har Altinn-tilgang")
    fun `Arbeidsgiver endpoints skal returnere 403 når bruker ikke har Altinn-tilgang`(
        httpMethod: HttpMethod,
        path: String,
        requestBody: Any?
    ) {
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                orgnr = korrektSyntetiskOrgnr,
                fnr = etAnnetKorrektSyntetiskFnr,
                status = SkjemaStatus.UTKAST,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                ),
            )
        )

        val token = createTokenForUser(korrektSyntetiskFnr)
        every { altinnService.harBrukerTilgang(savedSkjema.orgnr!!) } returns false

        val request = webTestClient.method(httpMethod)
            .uri(path, savedSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)

        requestBody?.let { request.bodyValue(it) }

        request.exchange()
            .expectStatus().isForbidden
    }

    fun arbeidsgiverEndpointsSomKreverTilgang(): List<Arguments> = listOf(
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/{id}/arbeidsgiver-view", null),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiverens-virksomhet-i-norge",
            arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/utenlandsoppdraget",
            utenlandsoppdragetDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidstakerens-lonn",
            arbeidstakerensLonnDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidssted-i-utlandet",
            arbeidsstedIUtlandetDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/tilleggsopplysninger",
            tilleggsopplysningerDtoMedDefaultVerdier()
        )
    )

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("arbeidstakerEndpointsSomKreverTilgang")
    @DisplayName("Arbeidstaker endpoints skal returnere 403 når bruker ikke har tilgang til skjemaet")
    fun `Arbeidstaker endpoints skal returnere 403 når bruker ikke har tilgang til skjemaet`(
        httpMethod: HttpMethod,
        path: String,
        requestBody: Any?
    ) {
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.UTKAST
            )
        )

        val token =
            createTokenForUser(korrektSyntetiskFnr) //  korrektSyntetiskFnr har ikke tilgang til skjemaet som tilhører etAnnetKorrektSyntetiskFnr

        val request = webTestClient.method(httpMethod)
            .uri(path, savedSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)

        requestBody?.let { request.bodyValue(it) }

        request.exchange()
            .expectStatus().isForbidden
    }

    fun arbeidstakerEndpointsSomKreverTilgang(): List<Arguments> = listOf(
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/{id}/arbeidstaker-view", null),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/utenlandsoppdraget",
            utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/arbeidssituasjon",
            arbeidssituasjonDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/skatteforhold-og-inntekt",
            skatteforholdOgInntektDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/familiemedlemmer",
            familiemedlemmerDtoMedDefaultVerdier()
        ),
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/tilleggsopplysninger",
            tilleggsopplysningerDtoMedDefaultVerdier()
        ),
        // Teknisk sett endepunkt felles for arbeidstaker og arbeidsgiver
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/{id}/send-inn",
            null
        ),
        Arguments.of(
            HttpMethod.GET,
            "/api/skjema/utsendt-arbeidstaker/{id}/innsendt-kvittering",
            null
        ),
    )

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("postEndpoints")
    @DisplayName("POST-endepunkter skal returnere 400 tom JSON body")
    fun `POST endpoints should return 400 for empty JSON body`(httpMethod: HttpMethod, path: String) {
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.UTKAST
            )
        )

        val token = createTokenForUser(korrektSyntetiskFnr)

        webTestClient.method(httpMethod)
            .uri(path, savedSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest
    }

    fun postEndpoints(): List<Arguments> = listOf(
        Arguments.of(
            HttpMethod.POST,
            "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiverens-virksomhet-i-norge"
        ),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/utenlandsoppdraget"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidstakerens-lonn"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidssted-i-utlandet"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/tilleggsopplysninger"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/utenlandsoppdraget"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/arbeidssituasjon"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/skatteforhold-og-inntekt"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/familiemedlemmer"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/tilleggsopplysninger")
    )

    fun arbeidsgiverStegTestFixtures(): List<Arguments> {
        val baseData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()

        return listOf(
            SkjemaStegTestFixture(
                stepKey = "arbeidsgiverens-virksomhet-i-norge",
                requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "utenlandsoppdraget",
                requestBody = utenlandsoppdragetDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "arbeidstakerens-lonn",
                requestBody = arbeidstakerensLonnDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "arbeidssted-i-utlandet",
                requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "tilleggsopplysninger",
                requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier())

            )
        ).map { Arguments.of(it) }
    }

    fun arbeidstakerStegTestFixtures(): List<Arguments> {
        val baseData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()

        return listOf(
            SkjemaStegTestFixture(
                stepKey = "utenlandsoppdraget",
                requestBody = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "arbeidssituasjon",
                requestBody = arbeidssituasjonDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "skatteforhold-og-inntekt",
                requestBody = skatteforholdOgInntektDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "familiemedlemmer",
                requestBody = familiemedlemmerDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier())

            ),
            SkjemaStegTestFixture(
                stepKey = "tilleggsopplysninger",
                requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                dataBeforePost = baseData,
                expectedDataAfterPost = baseData.copy(tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier())

            )
        ).map { Arguments.of(it) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkterMedUgyldigData")
    fun `Påse at kjøres validering på alle request bodies`(fixture: SkjemaStegTestFixture<*>) {
        val token = createTokenForUser(korrektSyntetiskFnr)
        val testId = UUID.randomUUID()

        webTestClient.post()
            .uri(fixture.uri, testId)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fixture.requestBody)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorResponse::class.java)
            .returnResult().responseBody.run {
                this.shouldNotBeNull()
                this.errors.shouldNotBeNull()
                this.errors shouldBe fixture.expectedValidationError
            }

    }

    fun endepunkterMedUgyldigData(): List<Arguments> = listOf(
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidsgiverens-virksomhet-i-norge",
            requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
                erArbeidsgiverenOffentligVirksomhet = false,
                erArbeidsgiverenBemanningsEllerVikarbyraa = null,
                opprettholderArbeidsgiverenVanligDrift = false
            ),
            expectedValidationError = mapOf("erArbeidsgiverenBemanningsEllerVikarbyraa" to "arbeidsgiverensVirksomhetINorgeTranslation.maaOppgiOmBemanningsbyraa")
        ),
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/f47ac10b-58cc-4372-a567-0e02b2c3d479/utenlandsoppdraget",
            requestBody = utenlandsoppdragetDtoMedDefaultVerdier().copy(
                arbeidstakerUtsendelsePeriode = PeriodeDto(
                    fraDato = java.time.LocalDate.of(2024, 12, 31),
                    tilDato = java.time.LocalDate.of(2024, 1, 1)
                )
            ),
            expectedValidationError = mapOf("arbeidstakerUtsendelsePeriode" to "periodeTranslation.fraDatoMaaVaereFoerTilDato")
        ),
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidstakerens-lonn",
            requestBody = arbeidstakerensLonnDtoMedDefaultVerdier().copy(
                arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                virksomheterSomUtbetalerLonnOgNaturalytelser = norskeOgUtenlandskeVirksomheterMedDefaultVerdier()
            ),
            expectedValidationError = mapOf("virksomheterSomUtbetalerLonnOgNaturalytelser" to "arbeidstakerensLonnTranslation.virksomheterSkalIkkeOppgis")
        ),
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidssted-i-utlandet",
            requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.PA_LAND,
                paLand = null,
            ),
            expectedValidationError = mapOf("paLand" to "arbeidsstedIUtlandetTranslation.maaOppgiArbeidsstedPaLand")
        ),
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/f47ac10b-58cc-4372-a567-0e02b2c3d479/tilleggsopplysninger",
            requestBody = tilleggsopplysningerDtoMedDefaultVerdier().copy(
                harFlereOpplysningerTilSoknaden = true,
                tilleggsopplysningerTilSoknad = null
            ),
            expectedValidationError = mapOf("tilleggsopplysningerTilSoknad" to "tilleggsopplysningerTranslation.maaOppgiTilleggsopplysninger")
        ),
        // Arbeidstaker endpoints
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/utenlandsoppdraget",
            requestBody = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier().copy(
                utsendelsePeriode = PeriodeDto(
                    fraDato = java.time.LocalDate.of(2024, 12, 31),
                    tilDato = java.time.LocalDate.of(2024, 1, 1)
                )
            ),
            expectedValidationError = mapOf("utsendelsePeriode" to "periodeTranslation.fraDatoMaaVaereFoerTilDato")
        ),
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidssituasjon",
            requestBody = arbeidssituasjonDtoMedDefaultVerdier().copy(
                harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
                aktivitetIMaanedenFoerUtsendingen = null
            ),
            expectedValidationError = mapOf("aktivitetIMaanedenFoerUtsendingen" to "arbeidssituasjonTranslation.maaOppgiAktivitetFoerUtsending")
        ),
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/skatteforhold-og-inntekt",
            requestBody = skatteforholdOgInntektDtoMedDefaultVerdier().copy(
                mottarPengestotteFraAnnetEosLandEllerSveits = true,
                landSomUtbetalerPengestotte = null
            ),
            expectedValidationError = mapOf("landSomUtbetalerPengestotte" to "skatteforholdOgInntektTranslation.maaOppgiLandSomUtbetalerPengestotte")
        ),
        SkjemaStegTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/tilleggsopplysninger",
            requestBody = tilleggsopplysningerDtoMedDefaultVerdier().copy(
                harFlereOpplysningerTilSoknaden = true,
                tilleggsopplysningerTilSoknad = null
            ),
            expectedValidationError = mapOf("tilleggsopplysningerTilSoknad" to "tilleggsopplysningerTranslation.maaOppgiTilleggsopplysninger")
        )
    ).map { Arguments.of(it) }

    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/{id}/send-inn skal sende inn skjema")
    fun `POST send-inn skal sende inn skjema`() {
        val skjemaSomSkalSendesInn = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.UTKAST,
                type = SkjemaType.UTSENDT_ARBEIDSTAKER,
                data = arbeidstakersSkjemaDataDtoMedDefaultVerdier(),
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV
                )
            )
        )

        val token = createTokenForUser(skjemaSomSkalSendesInn.fnr!!)

        val skjemaInnsendtKvittering = webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjemaSomSkalSendesInn.id!!}/send-inn")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(SkjemaInnsendtKvittering::class.java)
            .returnResult().responseBody

        skjemaInnsendtKvittering.shouldNotBeNull()

        skjemaInnsendtKvittering.run {
            skjemaInnsendtKvittering.skjemaId shouldBe skjemaSomSkalSendesInn.id
            skjemaInnsendtKvittering.status shouldBe SkjemaStatus.SENDT
            skjemaInnsendtKvittering.referanseId shouldMatch "^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}$"
        }
    }

    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id}/innsendt-kvittering skal hente kvittering")
    fun `GET innsendt-kvittering skal returnere kvittering`() {
        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                status = SkjemaStatus.SENDT
            )
        )

        innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = skjema,
                status = InnsendingStatus.MOTTATT,
                referanseId = "ABC123"
            )
        )

        val token = createTokenForUser(korrektSyntetiskFnr)

        val kvittering = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${skjema.id!!}/innsendt-kvittering")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(SkjemaInnsendtKvittering::class.java)
            .returnResult().responseBody

        kvittering.shouldNotBeNull()
        kvittering.skjemaId shouldBe skjema.id
        kvittering.referanseId shouldBe "ABC123"
        kvittering.status shouldBe SkjemaStatus.SENDT
    }

    private fun createTokenForUser(pid: String): String {
        return mockOAuth2Server.getToken(
            claims = mapOf("pid" to pid)
        )
    }
}