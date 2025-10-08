package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.skjema.*
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.NotificationService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.util.*
import no.nav.melosys.skjema.service.AltinnService
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpMethod

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkjemaControllerIntegrationTest : ApiTestBase() {
    
    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @Autowired 
    private lateinit var mockOAuth2Server: MockOAuth2Server
    
    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository
    
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
            .expectBody<List<Map<String, Any>>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody.shouldHaveSize(2)
                val orgnrs = responseBody.map { it["orgnr"] }
                orgnrs shouldBe listOf(testOrgnr, "987654321")
            }
    }
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver skal opprette nytt skjema")
    fun `POST skjema skal opprette nytt skjema`() {
        val token = createTokenForUser(testPid)
        val createRequest = mapOf(
            "orgnr" to testOrgnr
        )
        
        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody["orgnr"] shouldBe testOrgnr
                responseBody["status"] shouldBe "UTKAST"
                responseBody["id"].toString().shouldNotBeBlank()
            }
    }

    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver skal ikke tillate å opprette skjema for orgnr bruker ikke har tilgang til")
    fun `POST skjema bruker kan ikke opprette hvis ikke tilgang`() {
        val token = createTokenForUser(testPid)
        val createRequest = mapOf(
            "orgnr" to testOrgnr
        )

        every { altinnService.harBrukerTilgang(testOrgnr) } returns false

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isNotFound
    }
    
    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/arbeidstaker/{id} skal returnere spesifikt skjema")
    fun `GET skjema som arbeidstaker by id skal returnere spesifikt skjema`() {
        val skjema = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody["id"] shouldBe savedSkjema.id.toString()
                responseBody["fnr"] shouldBe testPid
                responseBody["orgnr"] shouldBe testOrgnr
                responseBody["status"] shouldBe "UTKAST"
            }
    }

    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id} skal returnere spesifikt skjema")
    fun `GET skjema som arbeidsgiver by id skal returnere spesifikt skjema`() {
        val skjema = skjemaMedDefaultVerdier(orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)

        val token = createTokenForUser(testPid)

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody["id"] shouldBe savedSkjema.id.toString()
                responseBody["orgnr"] shouldBe savedSkjema.orgnr
                responseBody["status"] shouldBe savedSkjema.status.toString()
            }
    }
    
    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id} skal returnere 404 for ikke-eksisterende skjema")
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
        val skjema = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        
        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/submit")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
        verify { notificationService.sendNotificationToArbeidstaker(savedSkjema.id.toString(), any()) }
        verify { notificationService.sendNotificationToArbeidsgiver(any(), any(), any(), testOrgnr) }
    }
    
    @Test
    @DisplayName("GET /api/skjema/utsendt-arbeidstaker/{id}/pdf skal returnere PDF response")
    fun `GET pdf skal returnere PDF response`() {
        val skjema = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.SENDT)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${savedSkjema.id}/pdf")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiveren skal lagre arbeidsgiver info")
    fun `POST arbeidsgiver info skal lagre arbeidsgiver info`() {
        val skjema = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        val arbeidsgiverRequest = mapOf(
            "organisasjonsnummer" to testOrgnr,
            "organisasjonNavn" to "Test Bedrift AS"
        )
        
        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${savedSkjema.id}/arbeidsgiveren")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(arbeidsgiverRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody["id"] shouldBe savedSkjema.id.toString()
                responseBody["data"].toString().shouldNotBeBlank()
            }
    }
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/virksomhet-i-norge skal lagre virksomhet info")
    fun `POST virksomhet info skal lagre virksomhet info`() {
        val skjema = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        val virksomhetRequest = mapOf(
            "erArbeidsgiverenOffentligVirksomhet" to true,
            "erArbeidsgiverenBemanningsEllerVikarbyraa" to false,
            "opprettholderArbeidsgivereVanligDrift" to true
        )

        every { altinnService.harBrukerTilgang(savedSkjema.orgnr!!) } returns true

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${savedSkjema.id}/virksomhet-i-norge")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(virksomhetRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
    }
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/submit skal sende inn skjema")
    fun `POST oppsummering skal sende inn skjema`() {
        val skjema = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        val oppsummeringRequest = mapOf(
            "bekreftetRiktighet" to true,
            "submittedAt" to Instant.now().toString()
        )
        
        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidsgiver/${savedSkjema.id}/submit")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(oppsummeringRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                val responseBody = response.responseBody
                responseBody.shouldNotBeNull()
                responseBody["status"] shouldBe "SENDT"
            }
    }
    
    @Test
    @DisplayName("POST /api/skjema/utsendt-arbeidstaker/arbeidstaker/{id}/arbeidstakeren skal lagre arbeidstaker info")
    fun `POST arbeidstaker info skal lagre arbeidstaker info`() {
        val skjema = skjemaMedDefaultVerdier(fnr = testPid, orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        val arbeidstakerRequest = mapOf(
            "harNorskFodselsnummer" to true,
            "fodselsnummer" to testPid,
            "fornavn" to "Test",
            "etternavn" to "Testesen",
            "harVaertEllerSkalVaereILonnetArbeidFoerUtsending" to true,
            "aktivitetIMaanedenFoerUtsendingen" to "LONNET_ARBEID",
            "skalJobbeForFlereVirksomheter" to false
        )
        
        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker/${savedSkjema.id}/arbeidstakeren")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(arbeidstakerRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
    }
    
    @Test
    @DisplayName("Get /api/skjema/utsendt-arbeidstaker/arbeidstaker/{id} skal ikke kunne aksessere andres skjemaer")
    fun `Get skjema som arbeidstaker skal ikke kunne aksessere andres skjemaer`() {
        val andresSkjema = skjemaMedDefaultVerdier(fnr = "99999999999", orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(andresSkjema)
        
        val token = createTokenForUser(testPid)
        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/arbeidstaker/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }

    @MethodSource("orgnummerHarVerdiOgOrgnnummerErNull")
    @ParameterizedTest(name = "har ikke tilgang når orgnr = {0}")
    @DisplayName("Get /api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id} skal ikke kunne aksessere andres skjemaer")
    fun `Get skjema som arbeidsgiver skal ikke kunne aksessere andres skjemaer`(orgnummer: String?) {
        val andresSkjema = skjemaMedDefaultVerdier(orgnr = orgnummer)
        val savedSkjema = skjemaRepository.save(andresSkjema)

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
        val skjema = skjemaMedDefaultVerdier(orgnr = testOrgnr, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        every { altinnService.harBrukerTilgang(testOrgnr) } returns false

        val actualPath = path.replace("{id}", savedSkjema.id.toString())
        
        val request = webTestClient.method(httpMethod)
            .uri(actualPath)
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
        val skjema = skjemaMedDefaultVerdier(orgnr = null, status = SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)

        val actualPath = path.replace("{id}", savedSkjema.id.toString())
        
        val request = webTestClient.method(httpMethod)
            .uri(actualPath)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            
        requestBody?.let { request.bodyValue(it) }
        
        request.exchange()
            .expectStatus().isNotFound
    }

    fun arbeidsgiverEndpointsSomKreverTilgang(): List<Arguments> = listOf(
        Arguments.of(HttpMethod.GET, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}", null),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidsgiveren", arbeidsgiverRequestMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/virksomhet-i-norge", virksomhetRequestMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/utenlandsoppdraget", utenlandsoppdragRequestMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/arbeidstakerens-lonn", arbeidstakerLonnRequestMedDefaultVerdier()),
        Arguments.of(HttpMethod.POST, "/api/skjema/utsendt-arbeidstaker/arbeidsgiver/{id}/submit", submitSkjemaRequestMedDefaultVerdier())
    )

    
    private fun createTokenForUser(pid: String): String {
        return mockOAuth2Server.getToken(
            claims = mapOf("pid" to pid)
        )
    }
}