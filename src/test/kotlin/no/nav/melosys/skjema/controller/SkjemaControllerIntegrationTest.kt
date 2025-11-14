package no.nav.melosys.skjema.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiverenDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakerenArbeidsgiversDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakerenDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakerensLonnDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDto
import no.nav.melosys.skjema.dto.arbeidsgiver.CreateArbeidsgiverSkjemaRequest
import no.nav.melosys.skjema.dto.arbeidstaker.CreateArbeidstakerSkjemaRequest
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.familiemedlemmerDtoMedDefaultVerdier
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.service.NotificationService
import no.nav.melosys.skjema.skatteforholdOgInntektDtoMedDefaultVerdier
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.submitSkjemaRequestMedDefaultVerdier
import no.nav.melosys.skjema.tilleggsopplysningerDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidssituasjonDtoMedDefaultVerdier
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

data class SkjemaStegTestFixture<T>(
    val stepKey: String,
    val requestBody: Any,
    val dataBeforePost: T,
    val expectedDataAfterPost: T
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkjemaControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @Autowired 
    private lateinit var mockOAuth2Server: MockOAuth2Server
    
    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockkBean
    private lateinit var notificationService: NotificationService

    @MockkBean
    private lateinit var altinnService: AltinnService
    
    private val testPid = "12345678901"
    private val testOrgnr = "123456789"
    
    @BeforeEach
    fun setUp() {
        clearMocks(notificationService)
        clearMocks(altinnService)
        skjemaRepository.deleteAll()

        every { altinnService.harBrukerTilgang(any()) } returns true
        every { notificationService.sendNotificationToArbeidstaker(any(), any()) } returns Unit
        every { notificationService.sendNotificationToArbeidsgiver(any(), any(), any(), any()) } returns "test-beskjed-id"
    }
    
    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker skal returnere liste over brukerens skjemaer")
    fun `GET skjema skal returnere liste over brukerens skjemaer`() {
        val skjema1 = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val skjema2 = skjemaMedDefaultVerdier(fnr = testPid, orgnr = "987654321", status = SkjemaStatus.SENDT)
        val skjema3 = skjemaMedDefaultVerdier(fnr = "99999999999", orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        
        skjemaRepository.saveAll(listOf(skjema1, skjema2, skjema3))
        
        val token = createTokenForUser(testPid)
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<ArbeidstakersSkjemaDto>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody.shouldHaveSize(2)
            }
    }
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver skal opprette nytt skjema")
    fun `POST skjema skal opprette nytt skjema`() {
        val token = createTokenForUser(testPid)
        val createRequest = CreateArbeidsgiverSkjemaRequest(testOrgnr)

        val responseBody = webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<ArbeidsgiversSkjemaDto>()
            .returnResult().responseBody

        responseBody.run {
            this.shouldNotBeNull()
            this shouldBe ArbeidsgiversSkjemaDto(
                id = this.id,
                orgnr = createRequest.orgnr,
                status = SkjemaStatus.UTKAST,
                data = ArbeidsgiversSkjemaDataDto()
            )
            skjemaRepository.findByIdOrNull(this.id).shouldNotBeNull()
        }
    }

    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver skal ikke tillate å opprette skjema for orgnr bruker ikke har tilgang til")
    fun `POST skjema bruker kan ikke opprette hvis ikke tilgang`() {
        val token = createTokenForUser(testPid)
        val createRequest = CreateArbeidsgiverSkjemaRequest(testOrgnr)

        every { altinnService.harBrukerTilgang(createRequest.orgnr) } returns false

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isNotFound

        skjemaRepository.findByOrgnr(createRequest.orgnr).shouldBeEmpty()
    }
    
    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/arbeidstaker/{id} skal returnere spesifikt skjema")
    fun `GET skjema som arbeidstaker by id skal returnere spesifikt skjema`() {
        val skjemaData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = testPid,
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST,
            data = objectMapper.valueToTree(skjemaData)
        ))
        
        val token = createTokenForUser(savedSkjema.fnr!!)
        
        val responseBody = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<ArbeidstakersSkjemaDto>()
            .returnResult().responseBody

        responseBody.run {
            this.shouldNotBeNull()
            this shouldBe ArbeidstakersSkjemaDto(
                id = savedSkjema.id!!,
                fnr = savedSkjema.fnr,
                status = SkjemaStatus.UTKAST,
                data = skjemaData
            )
        }
    }

    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id} skal returnere spesifikt skjema")
    fun `GET skjema som arbeidsgiver by id skal returnere spesifikt skjema`() {
        val skjemaData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST,
            data = objectMapper.valueToTree(skjemaData)
        ))

        val token = createTokenForUser(testPid)

        val responseBody = webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<ArbeidsgiversSkjemaDto>()
            .returnResult().responseBody

        responseBody.run {
            this.shouldNotBeNull()
            this shouldBe ArbeidsgiversSkjemaDto(
                id = savedSkjema.id!!,
                orgnr = savedSkjema.orgnr!!,
                status = savedSkjema.status,
                data = skjemaData
            )
        }
    }
    
    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/arbeidstaker/{id} skal returnere 404 for ikke-eksisterende skjema")
    fun `GET skjema by id skal returnere 404 for ikke-eksisterende skjema`() {
        val token = createTokenForUser(testPid)
        val nonExistentId = UUID.randomUUID()
        
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker/$nonExistentId")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }
    
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/{id}/submit skal sende skjema og trigge notifikasjoner")
    fun `POST submit skjema skal sende skjema og trigge notifikasjoner`() {
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = testPid,
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST
        ))
        
        val token = createTokenForUser(testPid)
        
        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/submit")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
        verify { notificationService.sendNotificationToArbeidstaker(testPid, any()) }
        verify { notificationService.sendNotificationToArbeidsgiver(any(), any(), any(), testOrgnr) }
    }
    
    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id}/pdf skal returnere PDF response")
    fun `GET pdf skal returnere PDF response`() {
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = testPid,
            orgnr = testOrgnr,
            status = SkjemaStatus.SENDT
        ))
        
        val token = createTokenForUser(testPid)
        
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/pdf")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/submit skal sende inn skjema")
    fun `POST submit skal sende inn skjema`() {
        val existingSkjemaDataBeforePOST = arbeidstakersSkjemaDataDtoMedDefaultVerdier()

        val existingSkjemaBeforePOST = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = testPid,
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST,
            data = objectMapper.valueToTree(existingSkjemaDataBeforePOST)
        ))
        
        val token = createTokenForUser(testPid)
        val submitRequest = submitSkjemaRequestMedDefaultVerdier()
        
        val responseBody = webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${existingSkjemaBeforePOST.id}/submit")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(submitRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<ArbeidstakersSkjemaDto>()
            .returnResult().responseBody

        responseBody.run {
            this.shouldNotBeNull()
            this shouldBe ArbeidstakersSkjemaDto(
                id = existingSkjemaBeforePOST.id!!,
                fnr = existingSkjemaBeforePOST.fnr!!,
                status = SkjemaStatus.SENDT,
                data = existingSkjemaDataBeforePOST
            )

            val persistedSkjemaDataAfterPOST = convertJsonToDto<ArbeidstakersSkjemaDataDto>(
                skjemaRepository.getReferenceById(this.id).data
            )
            this.data shouldBe persistedSkjemaDataAfterPOST
        }
    }
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidstaker skal opprette nytt skjema")
    fun `POST arbeidstaker skjema skal opprette nytt skjema`() {
        val createRequest = CreateArbeidstakerSkjemaRequest(testPid)

        val token = createTokenForUser(createRequest.fnr)
        
        val responseBody = webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<ArbeidstakersSkjemaDto>()
            .returnResult().responseBody

        responseBody.run {
            this.shouldNotBeNull()
            this shouldBe ArbeidstakersSkjemaDto(
                id = this.id,
                fnr = createRequest.fnr,
                status = SkjemaStatus.UTKAST,
                data = ArbeidstakersSkjemaDataDto()
            )
            skjemaRepository.findByIdOrNull(this.id).shouldNotBeNull()
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arbeidsgiverStegTestFixtures")
    @DisplayName("POST arbeidsgiver steg endpoints skal lagre data korrekt")
    fun `POST arbeidsgiver steg endpoints skal lagre data korrekt`(fixture: SkjemaStegTestFixture<ArbeidsgiversSkjemaDataDto>) {
        val existingSkjemaBeforePOST = skjemaRepository.save(skjemaMedDefaultVerdier(
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST,
            data = objectMapper.valueToTree(fixture.dataBeforePost)
        ))

        val token = createTokenForUser(testPid)

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${existingSkjemaBeforePOST.id}/${fixture.stepKey}")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fixture.requestBody)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)

        val persistedSkjemaDataAfterPOST = convertJsonToDto<ArbeidsgiversSkjemaDataDto>(
            skjemaRepository.getReferenceById(existingSkjemaBeforePOST.id!!).data
        )
        persistedSkjemaDataAfterPOST shouldBe fixture.expectedDataAfterPost
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arbeidstakerStegTestFixtures")
    @DisplayName("POST arbeidstaker steg endpoints skal lagre data korrekt")
    fun `POST arbeidstaker steg endpoints skal lagre data korrekt`(fixture: SkjemaStegTestFixture<ArbeidstakersSkjemaDataDto>) {
        val existingSkjemaBeforePOST = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = testPid,
            status = SkjemaStatus.UTKAST,
            data = objectMapper.valueToTree(fixture.dataBeforePost)
        ))

        val token = createTokenForUser(testPid)

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker/${existingSkjemaBeforePOST.id}/${fixture.stepKey}")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fixture.requestBody)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)

        val persistedSkjemaDataAfterPOST = convertJsonToDto<ArbeidstakersSkjemaDataDto>(
            skjemaRepository.getReferenceById(existingSkjemaBeforePOST.id!!).data
        )
        persistedSkjemaDataAfterPOST shouldBe fixture.expectedDataAfterPost
    }

    @MethodSource("orgnummerHarVerdiOgOrgnnummerErNull")
    @ParameterizedTest(name = "har ikke tilgang når orgnr = {0}")
    @DisplayName("Get /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id} skal ikke kunne aksessere andres skjemaer")
    fun `Get skjema som arbeidsgiver skal ikke kunne aksessere andres skjemaer`(orgnummer: String?) {
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            orgnr = orgnummer
        ))

        val token = createTokenForUser(testPid)

        orgnummer?.let {
            every { altinnService.harBrukerTilgang(it) } returns false
        }

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }

    fun orgnummerHarVerdiOgOrgnnummerErNull(): List<Arguments> = listOf(
        Arguments.of("123456789"),
        Arguments.of(null)
    )

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("arbeidsgiverEndpointsSomKreverTilgang")
    @DisplayName("Arbeidsgiver endpoints skal returnere 404 når bruker ikke har Altinn-tilgang")
    fun `Arbeidsgiver endpoints skal returnere 404 når bruker ikke har Altinn-tilgang`(httpMethod: HttpMethod, path: String, requestBody: Any?) {
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST
        ))
        
        val token = createTokenForUser(testPid)
        every { altinnService.harBrukerTilgang(testOrgnr) } returns false
        
        val request = webTestClient.method(httpMethod)
            .uri(path, savedSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            
        requestBody?.let { request.bodyValue(it) }
        
        request.exchange()
            .expectStatus().isNotFound
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("arbeidsgiverEndpointsSomKreverTilgang")
    @DisplayName("Arbeidsgiver endpoints skal returnere 404 for skjemaer med orgnr=null")
    fun `Arbeidsgiver endpoints skal returnere 404 for skjemaer med orgnr null`(httpMethod: HttpMethod, path: String, requestBody: Any?) {
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            orgnr = null,
            status = SkjemaStatus.UTKAST
        ))
        
        val token = createTokenForUser(testPid)
        
        val request = webTestClient.method(httpMethod)
            .uri(path, savedSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            
        requestBody?.let { request.bodyValue(it) }
        
        request.exchange()
            .expectStatus().isNotFound
    }

    fun arbeidsgiverEndpointsSomKreverTilgang(): List<Arguments> = listOf(
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}", null),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiveren", arbeidsgiverenDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidstakeren", arbeidstakerenArbeidsgiversDelDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiverens-virksomhet-i-norge", arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/utenlandsoppdraget", utenlandsoppdragetDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidstakerens-lonn", arbeidstakerensLonnDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidssted-i-utlandet", arbeidsstedIUtlandetDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/tilleggsopplysninger", tilleggsopplysningerDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/submit", submitSkjemaRequestMedDefaultVerdier())
    )

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("arbeidstakerEndpointsSomKreverTilgang")
    @DisplayName("Arbeidstaker endpoints skal returnere 404 når bruker ikke har tilgang til skjemaet")
    fun `Arbeidstaker endpoints skal returnere 404 når bruker ikke har tilgang til skjemaet`(httpMethod: HttpMethod, path: String, requestBody: Any?) {
        val annenBrukerFnr = "99999999999"
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = annenBrukerFnr,
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST
        ))
        
        val token = createTokenForUser(testPid) // testPid har ikke tilgang til skjemaet som tilhører annenBrukerFnr
        
        val request = webTestClient.method(httpMethod)
            .uri(path, savedSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            
        requestBody?.let { request.bodyValue(it) }
        
        request.exchange()
            .expectStatus().isNotFound
    }

    fun arbeidstakerEndpointsSomKreverTilgang(): List<Arguments> = listOf(
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}", null),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/arbeidstakeren", arbeidstakerenDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/utenlandsoppdraget", utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/arbeidssituasjon", arbeidssituasjonDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/skatteforhold-og-inntekt", skatteforholdOgInntektDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/familiemedlemmer", familiemedlemmerDtoMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/tilleggsopplysninger", tilleggsopplysningerDtoMedDefaultVerdier())
    )

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("postEndpoints")
    @DisplayName("POST-endepunkter skal returnere 400 tom JSON body")
    fun `POST endpoints should return 400 for empty JSON body`(httpMethod: HttpMethod, path: String) {
        val savedSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = testPid,
            orgnr = testOrgnr,
            status = SkjemaStatus.UTKAST
        ))

        val token = createTokenForUser(testPid)

        webTestClient.method(httpMethod)
            .uri(path, savedSkjema.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest
    }

    fun postEndpoints(): List<Arguments> = listOf(
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiveren"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidstakeren"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiverens-virksomhet-i-norge"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/utenlandsoppdraget"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidstakerens-lonn"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidssted-i-utlandet"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/tilleggsopplysninger"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/arbeidstakeren"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/utenlandsoppdraget"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/arbeidssituasjon"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/skatteforhold-og-inntekt"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/familiemedlemmer"),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/tilleggsopplysninger")
    )

    fun arbeidsgiverStegTestFixtures(): List<Arguments> {
        val baseData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()

        return listOf(
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "arbeidsgiveren",
                    requestBody = arbeidsgiverenDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(arbeidsgiveren = arbeidsgiverenDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "arbeidstakeren",
                    requestBody = arbeidstakerenArbeidsgiversDelDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(arbeidstakeren = arbeidstakerenArbeidsgiversDelDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "arbeidsgiverens-virksomhet-i-norge",
                    requestBody = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "utenlandsoppdraget",
                    requestBody = utenlandsoppdragetDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "arbeidstakerens-lonn",
                    requestBody = arbeidstakerensLonnDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "arbeidssted-i-utlandet",
                    requestBody = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "tilleggsopplysninger",
                    requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier())
                )
            )
        )
    }

    fun arbeidstakerStegTestFixtures(): List<Arguments> {
        val baseData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()

        return listOf(
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "arbeidstakeren",
                    requestBody = arbeidstakerenDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(arbeidstakeren = arbeidstakerenDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "utenlandsoppdraget",
                    requestBody = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "arbeidssituasjon",
                    requestBody = arbeidssituasjonDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "skatteforhold-og-inntekt",
                    requestBody = skatteforholdOgInntektDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "familiemedlemmer",
                    requestBody = familiemedlemmerDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier())
                )
            ),
            Arguments.of(
                SkjemaStegTestFixture(
                    stepKey = "tilleggsopplysninger",
                    requestBody = tilleggsopplysningerDtoMedDefaultVerdier(),
                    dataBeforePost = baseData,
                    expectedDataAfterPost = baseData.copy(tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier())
                )
            )
        )
    }


    private fun createTokenForUser(pid: String): String {
        return mockOAuth2Server.getToken(
            claims = mapOf("pid" to pid)
        )
    }

    private inline fun <reified T> convertJsonToDto(jsonNode: JsonNode?): T? {
        return jsonNode?.let { objectMapper.treeToValue(it, T::class.java) }
    }
}