package no.nav.melosys.skjema.integrasjon.ereg

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import java.nio.file.Files

class EregConsumerTest : ApiTestBase() {

    @Autowired
    private lateinit var eregConsumer: EregConsumer

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @AfterEach
    fun teardown() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentOrganisasjon skal utføre forventet http-request og deserialisere json respons`() {
        val orgnummer = "990983666"
        val responseJson = Files.readString(ClassPathResource("ereg/organisasjonResponse.json").file.toPath())

        wireMockServer.stubFor(get(urlPathMatching(".*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseJson)
            )
        )

        eregConsumer.hentOrganisasjon(orgnummer, inkluderHierarki = false)

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/v2/organisasjon/$orgnummer"))
            .withQueryParam("inkluderHierarki", equalTo("false"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Nav-Consumer-Id", equalTo("melosys-skjema-api"))
        )
    }

    @Test
    fun `hentOrganisasjon kaster OrganisasjonEksistererIkkeException når EREG returnerer 404`() {
        wireMockServer.stubFor(get(urlPathMatching(".*"))
            .willReturn(
                aResponse()
                    .withStatus(404)
            )
        )

        assertThrows<OrganisasjonEksistererIkkeException> {
            eregConsumer.hentOrganisasjon("999999999")
        }
    }
}