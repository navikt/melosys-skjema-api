package no.nav.melosys.skjema.controller

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.extensions.parseArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.m2mTokenWithReadSkjemaDataAccess
import no.nav.melosys.skjema.m2mTokenWithoutAccess
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import tools.jackson.databind.json.JsonMapper

class M2MSkjemaControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @BeforeEach
    fun setUp() {
        skjemaRepository.deleteAll()
        innsendingRepository.deleteAll()
    }

    @Nested
    @DisplayName("GET /m2m/api/skjema/utsendt-arbeidstaker/{id}/data")
    inner class GetSkjema {

        @Test
        fun `skal returnere skjema når gyldig M2M-token med tillatt klient`() {

            val skjema = skjemaRepository
                .save(skjemaMedDefaultVerdier(
                    status = SkjemaStatus.SENDT,
                    data = jsonMapper.valueToTree(arbeidstakersSkjemaDataDtoMedDefaultVerdier())
                ))

            val innsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema)
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            val responseBody = webTestClient.get()
                .uri("/m2m/api/skjema/utsendt-arbeidstaker/${skjema.id}/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<UtsendtArbeidstakerM2MSkjemaData>()
                .returnResult().responseBody.shouldNotBeNull()

            responseBody.referanseId shouldBe innsending.referanseId
            responseBody.arbeidsgiversDeler shouldBe emptyList()
            responseBody.arbeidstakersDeler.size shouldBe 1
            responseBody.arbeidstakersDeler[0].id shouldBe skjema.id
            responseBody.arbeidstakersDeler[0].data.utenlandsoppdraget shouldBe
                jsonMapper.parseArbeidstakersSkjemaDataDto(skjema.data!!).utenlandsoppdraget
        }

        @Test
        fun `skal returnere 403 når azp ikke matcher tillatt klient`() {
            val token = mockOAuth2Server.m2mTokenWithoutAccess()

            webTestClient.get()
                .uri("/m2m/api/skjema/utsendt-arbeidstaker/92fb319c-53f6-45e6-958a-9cbe1856973a/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `skal returnere 401 når token mangler`() {
            webTestClient.get()
                .uri("/m2m/api/skjema/utsendt-arbeidstaker/92fb319c-53f6-45e6-958a-9cbe1856973a/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `skal returnere 401 når TokenX-token brukes i stedet for Azure`() {
            val tokenXToken = mockOAuth2Server.getToken(
                claims = mapOf("pid" to "12345678901")
            )

            webTestClient.get()
                .uri("/m2m/api/skjema/utsendt-arbeidstaker/92fb319c-53f6-45e6-958a-9cbe1856973a/data")
                .header("Authorization", "Bearer $tokenXToken")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `skal returnere 404 når skjema ikke finnes`() {
            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()
            val ukjentId = UUID.randomUUID()

            webTestClient.get()
                .uri("/m2m/api/skjema/utsendt-arbeidstaker/$ukjentId/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
        }
    }
}
