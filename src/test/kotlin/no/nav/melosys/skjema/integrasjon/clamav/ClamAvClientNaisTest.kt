package no.nav.melosys.skjema.integrasjon.clamav

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.exception.VedleggVirusFunnetException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class ClamAvClientNaisTest : ApiTestBase() {

    @Autowired
    private lateinit var clamAvClientNais: ClamAvClientNais

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @AfterEach
    fun teardown() {
        wireMockServer.resetAll()
    }

    private fun testFil() = MockMultipartFile(
        "fil",
        "test.pdf",
        "application/pdf",
        "%PDF-1.4 test content".toByteArray()
    )

    @Test
    fun `scan sender fil som multipart-request og godtar OK-respons`() {
        wireMockServer.stubFor(
            put(urlPathEqualTo("/api/v2/scan"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""[{"filename":"test.pdf","result":"OK"}]""")
                )
        )

        shouldNotThrowAny { clamAvClientNais.scan(testFil()) }

        // Verifiserer at requesten faktisk bygges som multipart/form-data med fil-delen.
        // Dette er stien som tidligere krevde reactive-streams via MultipartBodyBuilder.
        wireMockServer.verify(
            putRequestedFor(urlPathEqualTo("/api/v2/scan"))
                .withHeader("Content-Type", containing("multipart/form-data"))
                .withRequestBody(containing("file1"))
                .withRequestBody(containing("test.pdf"))
        )
    }

    @Test
    fun `scan kaster VedleggVirusFunnetException naar ClamAV melder FOUND`() {
        wireMockServer.stubFor(
            put(urlPathEqualTo("/api/v2/scan"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""[{"filename":"test.pdf","result":"FOUND"}]""")
                )
        )

        shouldThrow<VedleggVirusFunnetException> { clamAvClientNais.scan(testFil()) }
    }

    @Test
    fun `scan kaster VedleggVirusFunnetException ved tomt svar`() {
        wireMockServer.stubFor(
            put(urlPathEqualTo("/api/v2/scan"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )

        shouldThrow<VedleggVirusFunnetException> { clamAvClientNais.scan(testFil()) }
    }
}

