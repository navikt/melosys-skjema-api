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
import no.nav.melosys.skjema.arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier
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
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.repr.ReprService
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
import no.nav.melosys.skjema.types.SkjemaData
import no.nav.melosys.skjema.types.SkjemaInnsendtKvittering
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.felles.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import no.nav.melosys.skjema.types.Skjemadel
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

data class UtsendtArbeidstakerControllerTestFixture<T>(
    // Eksisterende felter (brukes av steg-tester og validerings-tester)
    val stepKey: String = "",
    val uri: String = "",
    val requestBody: Any? = null,
    val expectedDataAfterPost: T? = null,
    val httpMethod: HttpMethod? = null,
    val expectedValidationError: Map<String, String>? = null,

    // Nye felter for applyFixture()
    val tokenFnr: String = korrektSyntetiskFnr,
    val existingSkjemaer: List<Skjema> = emptyList(), // Første element regnes som hovedskjema (brukes for id, orgnr, fnr i mocks)
    val altinnHarTilgang: Boolean? = null,
    val reprHarFullmakt: Boolean? = null,
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

    @MockkBean
    private lateinit var reprService: ReprService


    @BeforeEach
    fun setUp() {
        clearMocks(notificationService)
        clearMocks(altinnService)
        clearMocks(eregService)
        clearMocks(reprService)
        skjemaRepository.deleteAll()

        every { altinnService.harBrukerTilgang(any()) } returns true
        every { altinnService.hentBrukersTilganger() } returns emptyList()
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
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id} skal returnere spesifikt skjema som arbeidstaker")
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
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}")
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
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id} skal returnere spesifikt skjema som arbeidsgiver")
    fun `GET skjema som arbeidsgiver by id skal returnere spesifikt skjema`() {
        val skjemaData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()
        val savedSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.UTKAST,
                data = skjemaData,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                )
            )
        )

        val token = createTokenForUser(korrektSyntetiskFnr)

        val responseBody = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}")
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
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id} skal returnere 404 for ikke-eksisterende skjema")
    fun `GET skjema by id skal returnere 404 for ikke-eksisterende skjema`() {
        val token = createTokenForUser(korrektSyntetiskFnr)
        val nonExistentId = UUID.randomUUID()

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/$nonExistentId")
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
    @MethodSource("stegTestFixtures")
    @DisplayName("POST steg endpoints skal lagre data korrekt")
    fun `POST steg endpoints skal lagre data korrekt`(fixture: UtsendtArbeidstakerControllerTestFixture<SkjemaData>) {
        val savedSkjemaer = applyFixture(fixture)
        val existingSkjema = savedSkjemaer.first()

        val token = createTokenForUser(fixture.tokenFnr)

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/${existingSkjema.id}/${fixture.stepKey}")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fixture.requestBody!!)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<UtsendtArbeidstakerSkjemaDto>()

        val persistedData = skjemaRepository.getReferenceById(existingSkjema.id!!).data
        persistedData shouldBe fixture.expectedDataAfterPost
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tilgangNektetFixtures")
    @DisplayName("Endpoints skal returnere 403 når bruker ikke har tilgang")
    fun `Endpoints skal returnere 403 når bruker ikke har tilgang`(
        fixture: UtsendtArbeidstakerControllerTestFixture<*>
    ) {
        val savedSkjemaer = applyFixture(fixture)
        val existingSkjema = savedSkjemaer.first()

        val token = createTokenForUser(fixture.tokenFnr)

        val request = webTestClient.method(fixture.httpMethod!!)
            .uri(fixture.uri, existingSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)

        fixture.requestBody?.let { request.bodyValue(it) }

        request.exchange()
            .expectStatus().isForbidden
    }

    fun tilgangNektetFixtures(): List<Arguments> {
        fun arbeidsgiverSkjema() = skjemaMedDefaultVerdier(
            orgnr = korrektSyntetiskOrgnr,
            fnr = etAnnetKorrektSyntetiskFnr,
            status = SkjemaStatus.UTKAST,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
            ),
        )

        // ARBEIDSGIVER uten Altinn-tilgang — alle tilgangsstyrte endepunkter
        val arbeidsgiverUtenTilgang = listOf<UtsendtArbeidstakerControllerTestFixture<*>>(
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}",
                httpMethod = HttpMethod.GET,
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/innsendt-kvittering",
                httpMethod = HttpMethod.GET,
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/arbeidsgiverens-virksomhet-i-norge",
                httpMethod = HttpMethod.POST,
                requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/utenlandsoppdraget",
                httpMethod = HttpMethod.POST,
                requestBody = utenlandsoppdragetDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/arbeidstakerens-lonn",
                httpMethod = HttpMethod.POST,
                requestBody = arbeidstakerensLonnDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/arbeidssted-i-utlandet",
                httpMethod = HttpMethod.POST,
                requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/tilleggsopplysninger",
                httpMethod = HttpMethod.POST,
                requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/utsendingsperiode-og-land",
                httpMethod = HttpMethod.POST,
                requestBody = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/arbeidssituasjon",
                httpMethod = HttpMethod.POST,
                requestBody = arbeidssituasjonDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/skatteforhold-og-inntekt",
                httpMethod = HttpMethod.POST,
                requestBody = skatteforholdOgInntektDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/familiemedlemmer",
                httpMethod = HttpMethod.POST,
                requestBody = familiemedlemmerDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/send-inn",
                httpMethod = HttpMethod.POST,
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
                altinnHarTilgang = false,
            ),
        )

        // DEG_SELV med fnr mismatch — ett representativt endepunkt
        val degSelvFnrMismatch = listOf<UtsendtArbeidstakerControllerTestFixture<*>>(
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}",
                httpMethod = HttpMethod.GET,
                existingSkjemaer = listOf(
                    skjemaMedDefaultVerdier(
                        fnr = etAnnetKorrektSyntetiskFnr,
                        metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                            representasjonstype = Representasjonstype.DEG_SELV,
                        ),
                    )
                ),
            ),
        )

        // ARBEIDSGIVER_MED_FULLMAKT — fullmakt nektet av repr-api
        val fullmaktNektet = listOf<UtsendtArbeidstakerControllerTestFixture<*>>(
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}",
                httpMethod = HttpMethod.GET,
                existingSkjemaer = listOf(
                    skjemaMedDefaultVerdier(
                        fnr = etAnnetKorrektSyntetiskFnr,
                        orgnr = korrektSyntetiskOrgnr,
                        metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                            representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                            fullmektigFnr = korrektSyntetiskFnr,
                        ),
                    )
                ),
                reprHarFullmakt = false,
            ),
            // Kombinert skjemadel — fullmakt nektet
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/arbeidsgiverens-virksomhet-i-norge",
                httpMethod = HttpMethod.POST,
                requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(
                    skjemaMedDefaultVerdier(
                        fnr = etAnnetKorrektSyntetiskFnr,
                        orgnr = korrektSyntetiskOrgnr,
                        metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                            representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                            fullmektigFnr = korrektSyntetiskFnr,
                            skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                        ),
                    )
                ),
                reprHarFullmakt = false,
            ),
        )

        // ARBEIDSGIVER_MED_FULLMAKT — feil fullmektig
        val feilFullmektig = listOf<UtsendtArbeidstakerControllerTestFixture<*>>(
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}",
                httpMethod = HttpMethod.GET,
                existingSkjemaer = listOf(
                    skjemaMedDefaultVerdier(
                        fnr = etAnnetKorrektSyntetiskFnr,
                        orgnr = korrektSyntetiskOrgnr,
                        metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                            representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                            fullmektigFnr = etAnnetKorrektSyntetiskFnr, // Noen andre er fullmektig
                        ),
                    )
                ),
            ),
            // Kombinert skjemadel — feil fullmektig
            UtsendtArbeidstakerControllerTestFixture<Any>(
                uri = "/api/skjema/utsendt-arbeidstaker/{id}/arbeidssituasjon",
                httpMethod = HttpMethod.POST,
                requestBody = arbeidssituasjonDtoMedDefaultVerdier(),
                existingSkjemaer = listOf(
                    skjemaMedDefaultVerdier(
                        fnr = etAnnetKorrektSyntetiskFnr,
                        orgnr = korrektSyntetiskOrgnr,
                        metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                            representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                            fullmektigFnr = etAnnetKorrektSyntetiskFnr, // Noen andre er fullmektig
                            skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                        ),
                    )
                ),
            ),
        )

        return (arbeidsgiverUtenTilgang + degSelvFnrMismatch + fullmaktNektet + feilFullmektig)
            .map { Arguments.of(it) }
    }

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
            "/api/skjema/utsendt-arbeidstaker/{id}/arbeidsgiverens-virksomhet-i-norge"
        ),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/utenlandsoppdraget"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/arbeidstakerens-lonn"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/arbeidssted-i-utlandet"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/tilleggsopplysninger"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/utsendingsperiode-og-land"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/arbeidssituasjon"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/skatteforhold-og-inntekt"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/{id}/familiemedlemmer")
    )

    fun stegTestFixtures(): List<Arguments> {
        val agBaseData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()
        val atBaseData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        val agOgAtBaseData = arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier()

        fun arbeidsgiverSkjema(data: SkjemaData = agBaseData) = skjemaMedDefaultVerdier(
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            data = data,
        )

        fun fullmaktSkjema(data: SkjemaData = agBaseData) = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            data = data,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                fullmektigFnr = korrektSyntetiskFnr,
            ),
        )

        fun arbeidstakerSkjema(data: SkjemaData = atBaseData) = skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            status = SkjemaStatus.UTKAST,
            data = data,
        )

        fun arbeidsgiverOgArbeidstakerSkjema(data: SkjemaData = agOgAtBaseData) = skjemaMedDefaultVerdier(
            fnr = etAnnetKorrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            data = data,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                fullmektigFnr = korrektSyntetiskFnr,
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
            ),
        )

        return listOf(
            // Arbeidsgiver steg
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidsgiverens-virksomhet-i-norge",
                requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "utenlandsoppdraget",
                requestBody = utenlandsoppdragetDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidstakerens-lonn",
                requestBody = arbeidstakerensLonnDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidssted-i-utlandet",
                requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "tilleggsopplysninger",
                requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidsgiverSkjema()),
            ),
            // Arbeidsgiver med fullmakt steg
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidsgiverens-virksomhet-i-norge",
                requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(fullmaktSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "utenlandsoppdraget",
                requestBody = utenlandsoppdragetDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(fullmaktSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidstakerens-lonn",
                requestBody = arbeidstakerensLonnDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(fullmaktSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidssted-i-utlandet",
                requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(fullmaktSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "tilleggsopplysninger",
                requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                expectedDataAfterPost = agBaseData.copy(tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(fullmaktSkjema()),
                reprHarFullmakt = true,
            ),
            // Arbeidstaker steg
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "utsendingsperiode-og-land",
                requestBody = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
                expectedDataAfterPost = atBaseData.copy(utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidstakerSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidssituasjon",
                requestBody = arbeidssituasjonDtoMedDefaultVerdier(),
                expectedDataAfterPost = atBaseData.copy(arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidstakerSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "skatteforhold-og-inntekt",
                requestBody = skatteforholdOgInntektDtoMedDefaultVerdier(),
                expectedDataAfterPost = atBaseData.copy(skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidstakerSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "familiemedlemmer",
                requestBody = familiemedlemmerDtoMedDefaultVerdier(),
                expectedDataAfterPost = atBaseData.copy(familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidstakerSkjema()),
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "tilleggsopplysninger",
                requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                expectedDataAfterPost = atBaseData.copy(tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()),
                existingSkjemaer = listOf(arbeidstakerSkjema()),
            ),
            // Arbeidsgiver og arbeidstaker (kombinert) steg
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidsgiverens-virksomhet-i-norge",
                requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    arbeidsgiversData = agOgAtBaseData.arbeidsgiversData.copy(
                        arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier()
                    )
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "utenlandsoppdraget",
                requestBody = utenlandsoppdragetDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    arbeidsgiversData = agOgAtBaseData.arbeidsgiversData.copy(
                        utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier()
                    )
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidstakerens-lonn",
                requestBody = arbeidstakerensLonnDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    arbeidsgiversData = agOgAtBaseData.arbeidsgiversData.copy(
                        arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier()
                    )
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidssted-i-utlandet",
                requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    arbeidsgiversData = agOgAtBaseData.arbeidsgiversData.copy(
                        arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier()
                    )
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "utsendingsperiode-og-land",
                requestBody = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier()
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "arbeidssituasjon",
                requestBody = arbeidssituasjonDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    arbeidstakersData = agOgAtBaseData.arbeidstakersData.copy(
                        arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier()
                    )
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "skatteforhold-og-inntekt",
                requestBody = skatteforholdOgInntektDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    arbeidstakersData = agOgAtBaseData.arbeidstakersData.copy(
                        skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier()
                    )
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "familiemedlemmer",
                requestBody = familiemedlemmerDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    arbeidstakersData = agOgAtBaseData.arbeidstakersData.copy(
                        familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier()
                    )
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
            UtsendtArbeidstakerControllerTestFixture(
                stepKey = "tilleggsopplysninger",
                requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                expectedDataAfterPost = agOgAtBaseData.copy(
                    tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
                ),
                existingSkjemaer = listOf(arbeidsgiverOgArbeidstakerSkjema()),
                reprHarFullmakt = true,
            ),
        ).map { Arguments.of(it) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkterMedUgyldigData")
    fun `Påse at kjøres validering på alle request bodies`(fixture: UtsendtArbeidstakerControllerTestFixture<*>) {
        val token = createTokenForUser(korrektSyntetiskFnr)
        val testId = UUID.randomUUID()

        webTestClient.post()
            .uri(fixture.uri, testId)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fixture.requestBody!!)
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
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidsgiverens-virksomhet-i-norge",
            requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
                erArbeidsgiverenOffentligVirksomhet = false,
                erArbeidsgiverenBemanningsEllerVikarbyraa = null,
                opprettholderArbeidsgiverenVanligDrift = false
            ),
            expectedValidationError = mapOf("erArbeidsgiverenBemanningsEllerVikarbyraa" to "arbeidsgiverensVirksomhetINorgeTranslation.maaOppgiOmBemanningsbyraa")
        ),
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/utenlandsoppdraget",
            requestBody = utenlandsoppdragetDtoMedDefaultVerdier().copy(
                arbeidsgiverHarOppdragILandet = false,
                utenlandsoppholdetsBegrunnelse = ""
            ),
            expectedValidationError = mapOf("utenlandsoppholdetsBegrunnelse" to "utenlandsoppdragetTranslation.duMaOppgiBegrunnelse")
        ),
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidstakerens-lonn",
            requestBody = arbeidstakerensLonnDtoMedDefaultVerdier().copy(
                arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                virksomheterSomUtbetalerLonnOgNaturalytelser = norskeOgUtenlandskeVirksomheterMedDefaultVerdier()
            ),
            expectedValidationError = mapOf("virksomheterSomUtbetalerLonnOgNaturalytelser" to "arbeidstakerensLonnTranslation.virksomheterSkalIkkeOppgis")
        ),
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidssted-i-utlandet",
            requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.PA_LAND,
                paLand = null,
            ),
            expectedValidationError = mapOf("paLand" to "arbeidsstedIUtlandetTranslation.maaOppgiArbeidsstedPaLand")
        ),
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidsgiversSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/tilleggsopplysninger",
            requestBody = tilleggsopplysningerDtoMedDefaultVerdier().copy(
                harFlereOpplysningerTilSoknaden = true,
                tilleggsopplysningerTilSoknad = null
            ),
            expectedValidationError = mapOf("tilleggsopplysningerTilSoknad" to "tilleggsopplysningerTranslation.maaOppgiTilleggsopplysninger")
        ),
        // Arbeidstaker endpoints
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/utsendingsperiode-og-land",
            requestBody = UtsendingsperiodeOgLandDto(
                utsendelseLand = LandKode.SE,
                utsendelsePeriode = PeriodeDto(
                    fraDato = java.time.LocalDate.of(2024, 12, 31),
                    tilDato = java.time.LocalDate.of(2024, 1, 1)
                )
            ),
            expectedValidationError = mapOf("utsendelsePeriode" to "periodeTranslation.fraDatoMaaVaereFoerTilDato")
        ),
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/arbeidssituasjon",
            requestBody = arbeidssituasjonDtoMedDefaultVerdier().copy(
                harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
                aktivitetIMaanedenFoerUtsendingen = null
            ),
            expectedValidationError = mapOf("aktivitetIMaanedenFoerUtsendingen" to "arbeidssituasjonTranslation.maaOppgiAktivitetFoerUtsending")
        ),
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/skatteforhold-og-inntekt",
            requestBody = skatteforholdOgInntektDtoMedDefaultVerdier().copy(
                mottarPengestotteFraAnnetEosLandEllerSveits = true,
                landSomUtbetalerPengestotte = null
            ),
            expectedValidationError = mapOf("landSomUtbetalerPengestotte" to "skatteforholdOgInntektTranslation.maaOppgiLandSomUtbetalerPengestotte")
        ),
        UtsendtArbeidstakerControllerTestFixture<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>(
            uri = "/api/skjema/utsendt-arbeidstaker/f47ac10b-58cc-4372-a567-0e02b2c3d479/tilleggsopplysninger",
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

        val token = createTokenForUser(skjemaSomSkalSendesInn.fnr)

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

    private fun applyFixture(fixture: UtsendtArbeidstakerControllerTestFixture<*>): List<Skjema> {
        val savedSkjemaer = skjemaRepository.saveAll(fixture.existingSkjemaer)

        fixture.altinnHarTilgang?.let { harTilgang ->
            every { altinnService.harBrukerTilgang(savedSkjemaer.first().orgnr) } returns harTilgang
        }

        fixture.reprHarFullmakt?.let { harFullmakt ->
            every { reprService.harSkriverettigheterForMedlemskap(savedSkjemaer.first().fnr) } returns harFullmakt
        }

        return savedSkjemaer
    }

    private fun createTokenForUser(pid: String): String {
        return mockOAuth2Server.getToken(
            claims = mapOf("pid" to pid)
        )
    }
}