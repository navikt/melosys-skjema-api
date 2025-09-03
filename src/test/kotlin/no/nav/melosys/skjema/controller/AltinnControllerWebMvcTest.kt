package no.nav.melosys.skjema.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import no.nav.melosys.skjema.dto.OrganisasjonDto
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * WebMvc unit test for AltinnController.
 * Tester controller-laget isolert med mockede avhengigheter.
 */
@WebMvcTest(
    controllers = [AltinnController::class],
    properties = ["no.nav.security.jwt.enabled=false"]
)
@Import(AltinnControllerWebMvcTest.TestConfig::class)
class AltinnControllerWebMvcTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var altinnService: AltinnService
    
    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockAltinnService(): AltinnService = mockk()
        
        @Bean
        @Primary
        fun mockSubjectHandler(): SubjectHandler = mockk(relaxed = true)
    }
    
    @BeforeEach
    fun setup() {
        clearMocks(altinnService)
    }
    
    @AfterEach
    fun tearDown() {
        clearMocks(altinnService)
    }
    
    @Test
    fun `GET hentTilganger skal returnere liste over organisasjoner`() {
        val organisasjoner = listOf(
            OrganisasjonDto(
                orgnr = "123456789",
                navn = "Test Bedrift AS",
                organisasjonsform = "AS"
            ),
            OrganisasjonDto(
                orgnr = "987654321",
                navn = "Annen Bedrift AS",
                organisasjonsform = "AS"
            )
        )
        
        every { altinnService.hentBrukersTilganger() } returns organisasjoner
        

        mockMvc.get("/api/hentTilganger") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].orgnr") { value("123456789") }
            jsonPath("$[0].navn") { value("Test Bedrift AS") }
            jsonPath("$[0].organisasjonsform") { value("AS") }
            jsonPath("$[1].orgnr") { value("987654321") }
            jsonPath("$[1].navn") { value("Annen Bedrift AS") }
        }
        
        verify(exactly = 1) { altinnService.hentBrukersTilganger() }
    }
    
    @Test
    fun `GET hentTilganger skal returnere 204 når ingen tilganger`() {

        every { altinnService.hentBrukersTilganger() } returns emptyList()
        

        mockMvc.get("/api/hentTilganger") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNoContent() }
        }


        verify(exactly = 1) { altinnService.hentBrukersTilganger() }
    }
    
    @Test
    fun `GET harTilgang skal returnere true når bruker har tilgang`() {

        val orgnr = "123456789"
        every { altinnService.harBrukerTilgang(orgnr) } returns true
        

        mockMvc.get("/api/harTilgang/$orgnr") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { string("true") }
        }


        verify(exactly = 1) { altinnService.harBrukerTilgang(orgnr) }
    }
    
    @Test
    fun `GET harTilgang skal returnere false når bruker ikke har tilgang`() {

        val orgnr = "987654321"
        every { altinnService.harBrukerTilgang(orgnr) } returns false
        

        mockMvc.get("/api/harTilgang/$orgnr") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { string("false") }
        }


        verify(exactly = 1) { altinnService.harBrukerTilgang(orgnr) }
    }
    
    @Test
    fun `GET hentTilganger skal håndtere spesielle tegn i organisasjonsnavn`() {

        val organisasjoner = listOf(
            OrganisasjonDto(
                orgnr = "123456789",
                navn = "Ærlig & Ærlig AS",
                organisasjonsform = "AS"
            ),
            OrganisasjonDto(
                orgnr = "987654321",
                navn = "Østfold Øl-Bryggeri AS",
                organisasjonsform = "AS"
            )
        )

        every { altinnService.hentBrukersTilganger() } returns organisasjoner
        

        mockMvc.get("/api/hentTilganger") {
            accept = MediaType.APPLICATION_JSON
            characterEncoding = "UTF-8"
        }.andExpect {
            status { isOk() }
            content { 
                contentType("application/json")
                encoding("UTF-8")
            }
            jsonPath("$[0].navn") { value("Ærlig & Ærlig AS") }
            jsonPath("$[1].navn") { value("Østfold Øl-Bryggeri AS") }
        }
    }
    
    @Test
    fun `GET hentTilganger skal returnere mange organisasjoner`() {
        val mangeTilganger = (1..50).map { i ->
            OrganisasjonDto(
                orgnr = (100000000 + i).toString(),
                navn = "Organisasjon $i AS",
                organisasjonsform = "AS"
            )
        }
        
        every { altinnService.hentBrukersTilganger() } returns mangeTilganger
        

        mockMvc.get("/api/hentTilganger") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.length()") { value(50) }
            jsonPath("$[0].orgnr") { value("100000001") }
            jsonPath("$[49].orgnr") { value("100000050") }
        }
    }
}