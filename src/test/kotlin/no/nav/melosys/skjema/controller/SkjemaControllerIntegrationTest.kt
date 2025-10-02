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
import no.nav.melosys.skjema.entity.Skjema
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

class SkjemaControllerIntegrationTest : ApiTestBase() {
    
    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @Autowired 
    private lateinit var mockOAuth2Server: MockOAuth2Server
    
    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository
    
    @MockkBean
    private lateinit var notificationService: NotificationService
    
    private val testPid = "12345678901"
    private val testOrgnr = "123456789"
    
    @BeforeEach
    fun setUp() {
        clearMocks(notificationService)
        skjemaRepository.deleteAll()
        every { notificationService.sendNotificationToArbeidstaker(any(), any()) } returns Unit
        every { notificationService.sendNotificationToArbeidsgiver(any(), any(), any(), any()) } returns "test-beskjed-id"
    }
    
    @Test
    @DisplayName("GET /api/skjema skal returnere liste over brukerens skjemaer")
    fun `GET skjema skal returnere liste over brukerens skjemaer`() {
        val skjema1 = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
        val skjema2 = createTestSkjema(testPid, "987654321", SkjemaStatus.SENDT)
        val skjema3 = createTestSkjema("99999999999", testOrgnr, SkjemaStatus.UTKAST)
        
        skjemaRepository.saveAll(listOf(skjema1, skjema2, skjema3))
        
        val token = createTokenForUser(testPid)
        webTestClient.get()
            .uri("/api/skjema")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<List<Map<String, Any>>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                response.responseBody!!.shouldHaveSize(2)
                val orgnrs = response.responseBody!!.map { it["orgnr"] }
                orgnrs shouldBe listOf(testOrgnr, "987654321")
            }
    }
    
    @Test
    @DisplayName("POST /api/skjema skal opprette nytt skjema")
    fun `POST skjema skal opprette nytt skjema`() {
        val token = createTokenForUser(testPid)
        val createRequest = mapOf(
            "fnr" to testPid,
            "orgnr" to testOrgnr
        )
        
        webTestClient.post()
            .uri("/api/skjema")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                val responseBody = response.responseBody!!
                responseBody["fnr"] shouldBe testPid
                responseBody["orgnr"] shouldBe testOrgnr
                responseBody["status"] shouldBe "UTKAST"
                responseBody["id"].toString().shouldNotBeBlank()
            }
    }
    
    @Test
    @DisplayName("GET /api/skjema/{id} skal returnere spesifikt skjema")
    fun `GET skjema by id skal returnere spesifikt skjema`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        
        webTestClient.get()
            .uri("/api/skjema/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                val responseBody = response.responseBody!!
                responseBody["id"] shouldBe savedSkjema.id.toString()
                responseBody["fnr"] shouldBe testPid
                responseBody["orgnr"] shouldBe testOrgnr
                responseBody["status"] shouldBe "UTKAST"
            }
    }
    
    @Test
    @DisplayName("GET /api/skjema/{id} skal returnere 404 for ikke-eksisterende skjema")
    fun `GET skjema by id skal returnere 404 for ikke-eksisterende skjema`() {
        val token = createTokenForUser(testPid)
        val nonExistentId = UUID.randomUUID()
        
        webTestClient.get()
            .uri("/api/skjema/$nonExistentId")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }
    
    
    @Test
    @DisplayName("POST /api/skjema/{id}/submit skal sende skjema og trigge notifikasjoner")
    fun `POST submit skjema skal sende skjema og trigge notifikasjoner`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        
        webTestClient.post()
            .uri("/api/skjema/${savedSkjema.id}/submit")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
        verify { notificationService.sendNotificationToArbeidstaker(savedSkjema.id.toString(), any()) }
        verify { notificationService.sendNotificationToArbeidsgiver(any(), any(), any(), testOrgnr) }
    }
    
    @Test
    @DisplayName("GET /api/skjema/{id}/pdf skal returnere PDF response")
    fun `GET pdf skal returnere PDF response`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.SENDT)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        
        webTestClient.get()
            .uri("/api/skjema/${savedSkjema.id}/pdf")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }
    
    @Test
    @DisplayName("POST /api/skjema/{id}/arbeidsgiver/arbeidsgiveren skal lagre arbeidsgiver info")
    fun `POST arbeidsgiver info skal lagre arbeidsgiver info`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        val arbeidsgiverRequest = mapOf(
            "organisasjonsnummer" to testOrgnr,
            "organisasjonNavn" to "Test Bedrift AS"
        )
        
        webTestClient.post()
            .uri("/api/skjema/${savedSkjema.id}/arbeidsgiver/arbeidsgiveren")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(arbeidsgiverRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                val responseBody = response.responseBody!!
                responseBody["id"] shouldBe savedSkjema.id.toString()
                responseBody["data"].toString().shouldNotBeBlank()
            }
    }
    
    @Test
    @DisplayName("POST /api/skjema/{id}/arbeidsgiver/virksomhet-i-norge skal lagre virksomhet info")
    fun `POST virksomhet info skal lagre virksomhet info`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        val virksomhetRequest = mapOf(
            "erArbeidsgiverenOffentligVirksomhet" to true,
            "erArbeidsgiverenBemanningsEllerVikarbyraa" to false,
            "opprettholderArbeidsgivereVanligDrift" to true
        )
        
        webTestClient.post()
            .uri("/api/skjema/${savedSkjema.id}/arbeidsgiver/virksomhet-i-norge")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(virksomhetRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
    }
    
    @Test
    @DisplayName("POST /api/skjema/{id}/arbeidsgiver/oppsummering skal sende inn skjema")
    fun `POST oppsummering skal sende inn skjema`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        
        val token = createTokenForUser(testPid)
        val oppsummeringRequest = mapOf(
            "bekreftetRiktighet" to true,
            "submittedAt" to Instant.now().toString()
        )
        
        webTestClient.post()
            .uri("/api/skjema/${savedSkjema.id}/arbeidsgiver/oppsummering")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(oppsummeringRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<Map<String, Any>>()
            .consumeWith { response ->
                response.responseBody.shouldNotBeNull()
                val responseBody = response.responseBody!!
                responseBody["status"] shouldBe "SENDT"
            }
    }
    
    @Test
    @DisplayName("POST /api/skjema/{id}/arbeidstaker/arbeidstakeren skal lagre arbeidstaker info")
    fun `POST arbeidstaker info skal lagre arbeidstaker info`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
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
            .uri("/api/skjema/${savedSkjema.id}/arbeidstaker/arbeidstakeren")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(arbeidstakerRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
    }
    
    @Test
    @DisplayName("Skal ikke kunne aksessere andres skjemaer")
    fun `skal ikke kunne aksessere andres skjemaer`() {
        val andresSkjema = createTestSkjema("99999999999", testOrgnr, SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(andresSkjema)
        
        val token = createTokenForUser(testPid)
        webTestClient.get()
            .uri("/api/skjema/${savedSkjema.id}")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }
    
    @Test
    @DisplayName("Skal kreve autentisering for alle endepunkter")
    fun `skal kreve autentisering for alle endepunkter`() {
        val skjema = createTestSkjema(testPid, testOrgnr, SkjemaStatus.UTKAST)
        val savedSkjema = skjemaRepository.save(skjema)
        webTestClient.get()
            .uri("/api/skjema")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized
        webTestClient.post()
            .uri("/api/skjema")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("fnr" to testPid, "orgnr" to testOrgnr))
            .exchange()
            .expectStatus().isUnauthorized
        webTestClient.get()
            .uri("/api/skjema/${savedSkjema.id}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized
    }
    
    private fun createTestSkjema(fnr: String, orgnr: String, status: SkjemaStatus): Skjema {
        return Skjema(
            status = status,
            fnr = fnr,
            orgnr = orgnr,
            opprettetAv = fnr,
            endretAv = fnr,
            opprettetDato = Instant.now(),
            endretDato = Instant.now()
        )
    }
    
    private fun createTokenForUser(pid: String): String {
        return mockOAuth2Server.getToken(
            claims = mapOf("pid" to pid)
        )
    }
}