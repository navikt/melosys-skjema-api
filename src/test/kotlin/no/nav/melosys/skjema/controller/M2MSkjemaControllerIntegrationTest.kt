package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.extensions.toOsloLocalDateTime
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.pdl.PdlClient
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlFoedselsdato
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlNavn
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.m2mTokenWithReadSkjemaDataAccess
import no.nav.melosys.skjema.m2mTokenWithoutAccess
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.m2m.BulkOppdaterSaksstatusResultat
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class M2MSkjemaControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @MockkBean
    private lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setUp() {
        skjemaRepository.deleteAll()
        innsendingRepository.deleteAll()

        every { pdlClient.hentPerson(any()) } returns PdlPerson(
            navn = listOf(PdlNavn("Hans", null, "Hansen")),
            foedselsdato = listOf(PdlFoedselsdato("1980-01-01"))
        )
    }

    @Nested
    @DisplayName("GET /m2m/api/skjema/utsendt-arbeidstaker/{id}/data")
    inner class GetSkjema {

        @Test
        fun `skal returnere skjema når gyldig M2M-token med tillatt klient`() {
            val skjemaData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
            val skjema = skjemaRepository
                .save(
                    skjemaMedDefaultVerdier(
                        status = SkjemaStatus.SENDT,
                        data = skjemaData
                    )
                )

            val opprettetDato = Instant.parse("2025-01-15T10:30:00Z")
            val innsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    opprettetDato = opprettetDato,
                    referanseId = "TEST01"
                )
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            val responseBody = webTestClient.get()
                .uri("/m2m/api/skjema/utsendt-arbeidstaker/${skjema.id}/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<UtsendtArbeidstakerSkjemaM2MDto>()
                .returnResult().responseBody.shouldNotBeNull()

            responseBody.skjema.id shouldBe skjema.id
            responseBody.skjema.fnr shouldBe skjema.fnr
            responseBody.kobletSkjema.shouldBeNull()
            responseBody.tidligereInnsendteSkjema shouldBe emptyList()
            responseBody.referanseId shouldBe "TEST01"
            responseBody.innsenderFnr shouldBe innsending.innsenderFnr
            responseBody.dokumentTittel shouldBe "Søknad om A1 for utsendte arbeidstakere i EØS eller Sveits"
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


        @Test
        fun `skal returnere 404 når skjema har status UTKAST`() {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.UTKAST)
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            webTestClient.get()
                .uri("/m2m/api/skjema/utsendt-arbeidstaker/${skjema.id}/data")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("GET /m2m/api/skjema/{id}/pdf")
    inner class GetPdf {

        @Test
        fun `skal returnere PDF når gyldig M2M-token med tillatt klient`() {
            val skjemaData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    status = SkjemaStatus.SENDT,
                    data = skjemaData
                )
            )

            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema)
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            val responseBody = webTestClient.get()
                .uri("/m2m/api/skjema/${skjema.id}/pdf")
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_PDF)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectBody<ByteArray>()
                .returnResult().responseBody.shouldNotBeNull()

            // Verifiser at det er en gyldig PDF (starter med PDF magic bytes)
            val pdfHeader = String(responseBody.take(5).toByteArray())
            pdfHeader shouldStartWith "%PDF-"
        }

        @Test
        fun `skal returnere 403 når azp ikke matcher tillatt klient`() {
            val token = mockOAuth2Server.m2mTokenWithoutAccess()

            webTestClient.get()
                .uri("/m2m/api/skjema/92fb319c-53f6-45e6-958a-9cbe1856973a/pdf")
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `skal returnere 401 når token mangler`() {
            webTestClient.get()
                .uri("/m2m/api/skjema/92fb319c-53f6-45e6-958a-9cbe1856973a/pdf")
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `skal returnere 404 når skjema ikke finnes`() {
            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()
            val ukjentId = UUID.randomUUID()

            webTestClient.get()
                .uri("/m2m/api/skjema/$ukjentId/pdf")
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        fun `skal returnere 404 når skjema ikke er innsendt`() {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.UTKAST)
            )


            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            webTestClient.get()
                .uri("/m2m/api/skjema/${skjema.id}/pdf")
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("POST /m2m/api/skjema/{id}/saksnummer")
    inner class RegistrerSaksnummer {

        @Test
        fun `skal registrere saksnummer og returnere 204`() {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, status = InnsendingStatus.FERDIG)
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            webTestClient.post()
                .uri("/m2m/api/skjema/${skjema.id}/saksnummer")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "SAK123"}""")
                .exchange()
                .expectStatus().isNoContent

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.saksnummer shouldBe "SAK123"
        }

        @Test
        fun `skal returnere 400 naar saksnummer er tomt`() {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, status = InnsendingStatus.FERDIG)
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            webTestClient.post()
                .uri("/m2m/api/skjema/${skjema.id}/saksnummer")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": ""}""")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `skal returnere 400 naar saksnummer er blank med mellomrom`() {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, status = InnsendingStatus.FERDIG)
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            webTestClient.post()
                .uri("/m2m/api/skjema/${skjema.id}/saksnummer")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "   "}""")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `skal returnere 400 naar saksnummer er for langt`() {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, status = InnsendingStatus.FERDIG)
            )

            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

            webTestClient.post()
                .uri("/m2m/api/skjema/${skjema.id}/saksnummer")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "${"x".repeat(100)}"}""")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `skal returnere 404 naar skjema ikke finnes`() {
            val token = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()
            val ukjentId = UUID.randomUUID()

            webTestClient.post()
                .uri("/m2m/api/skjema/$ukjentId/saksnummer")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "SAK123"}""")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            val token = mockOAuth2Server.m2mTokenWithoutAccess()

            webTestClient.post()
                .uri("/m2m/api/skjema/${UUID.randomUUID()}/saksnummer")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "SAK123"}""")
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `skal returnere 401 naar token mangler`() {
            webTestClient.post()
                .uri("/m2m/api/skjema/${UUID.randomUUID()}/saksnummer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "SAK123"}""")
                .exchange()
                .expectStatus().isUnauthorized
        }
    }

    @Nested
    @DisplayName("PUT /m2m/api/skjema/{id}/saksstatus")
    inner class OppdaterSaksstatus {

        private fun lagInnsendtSkjemaMedInnsending(saksnummer: String? = null): Skjema {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, status = InnsendingStatus.FERDIG, saksnummer = saksnummer)
            )
            return skjema
        }

        private fun oppdaterSaksstatus(skjemaId: UUID, body: String) = webTestClient.put()
            .uri("/m2m/api/skjema/$skjemaId/saksstatus")
            .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()

        @Test
        fun `skal oppdatere saksstatus og backfille saksnummer naar det mangler`() {
            val skjema = lagInnsendtSkjemaMedInnsending(saksnummer = null)

            oppdaterSaksstatus(skjema.id!!, """{"saksnummer": "MEL-100", "saksstatus": "AVSLUTTET"}""")
                .expectStatus().isNoContent

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.saksstatus shouldBe Saksstatus.AVSLUTTET
            oppdatert.saksnummer shouldBe "MEL-100"
            oppdatert.saksstatusOppdatert.shouldNotBeNull()
        }

        @Test
        fun `skal oppdatere alle innsendinger med samme saksnummer`() {
            val agDel = lagInnsendtSkjemaMedInnsending(saksnummer = "MEL-200")
            val atDel = lagInnsendtSkjemaMedInnsending(saksnummer = "MEL-200")
            val annenSak = lagInnsendtSkjemaMedInnsending(saksnummer = "MEL-999")

            oppdaterSaksstatus(agDel.id!!, """{"saksnummer": "MEL-200", "saksstatus": "AVSLUTTET"}""")
                .expectStatus().isNoContent

            innsendingRepository.findBySkjemaId(agDel.id!!)!!.saksstatus shouldBe Saksstatus.AVSLUTTET
            innsendingRepository.findBySkjemaId(atDel.id!!)!!.saksstatus shouldBe Saksstatus.AVSLUTTET
            innsendingRepository.findBySkjemaId(annenSak.id!!)!!.saksstatus.shouldBeNull()
        }

        @Test
        fun `skal beholde eksisterende saksnummer ved avvik men fortsatt oppdatere status`() {
            val skjema = lagInnsendtSkjemaMedInnsending(saksnummer = "MEL-300")

            oppdaterSaksstatus(skjema.id!!, """{"saksnummer": "MEL-301", "saksstatus": "MOTTATT"}""")
                .expectStatus().isNoContent

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.saksnummer shouldBe "MEL-300"
            oppdatert.saksstatus shouldBe Saksstatus.MOTTATT
        }

        @Test
        fun `skal returnere 400 naar saksnummer er tomt`() {
            val skjema = lagInnsendtSkjemaMedInnsending()

            oppdaterSaksstatus(skjema.id!!, """{"saksnummer": "", "saksstatus": "AVSLUTTET"}""")
                .expectStatus().isBadRequest
        }

        @Test
        fun `skal returnere 404 naar skjema ikke finnes`() {
            oppdaterSaksstatus(UUID.randomUUID(), """{"saksnummer": "MEL-100", "saksstatus": "AVSLUTTET"}""")
                .expectStatus().isNotFound
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            webTestClient.put()
                .uri("/m2m/api/skjema/${UUID.randomUUID()}/saksstatus")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "MEL-100", "saksstatus": "AVSLUTTET"}""")
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `skal returnere 401 naar token mangler`() {
            webTestClient.put()
                .uri("/m2m/api/skjema/${UUID.randomUUID()}/saksstatus")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"saksnummer": "MEL-100", "saksstatus": "AVSLUTTET"}""")
                .exchange()
                .expectStatus().isUnauthorized
        }
    }

    @Nested
    @DisplayName("PUT /m2m/api/skjema/saksstatus/bulk")
    inner class BulkOppdaterSaksstatus {

        private fun lagInnsendtSkjemaMedInnsending(saksnummer: String? = null): Skjema {
            val skjema = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, status = InnsendingStatus.FERDIG, saksnummer = saksnummer)
            )
            return skjema
        }

        @Test
        fun `skal oppdatere flere innsendinger og rapportere ukjente skjema-id-er`() {
            val medSaksnummer = lagInnsendtSkjemaMedInnsending(saksnummer = "MEL-400")
            val utenSaksnummer = lagInnsendtSkjemaMedInnsending(saksnummer = null)
            val ukjentSkjemaId = UUID.randomUUID()

            val resultat = webTestClient.put()
                .uri("/m2m/api/skjema/saksstatus/bulk")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    """
                    {"oppdateringer": [
                        {"skjemaId": "${medSaksnummer.id}", "saksnummer": "MEL-400", "saksstatus": "AVSLUTTET"},
                        {"skjemaId": "${utenSaksnummer.id}", "saksnummer": "MEL-401", "saksstatus": "MOTTATT"},
                        {"skjemaId": "$ukjentSkjemaId", "saksnummer": "MEL-402", "saksstatus": "AVSLUTTET"}
                    ]}
                    """.trimIndent()
                )
                .exchange()
                .expectStatus().isOk
                .expectBody<BulkOppdaterSaksstatusResultat>()
                .returnResult().responseBody.shouldNotBeNull()

            resultat.antallOppdatert shouldBe 2
            resultat.ukjenteSkjemaIder shouldBe listOf(ukjentSkjemaId)
            innsendingRepository.findBySkjemaId(medSaksnummer.id!!)!!.saksstatus shouldBe Saksstatus.AVSLUTTET
            val backfillet = innsendingRepository.findBySkjemaId(utenSaksnummer.id!!)!!
            backfillet.saksstatus shouldBe Saksstatus.MOTTATT
            backfillet.saksnummer shouldBe "MEL-401"
        }

        @Test
        fun `skal returnere 400 naar oppdateringer er tom`() {
            webTestClient.put()
                .uri("/m2m/api/skjema/saksstatus/bulk")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"oppdateringer": []}""")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            webTestClient.put()
                .uri("/m2m/api/skjema/saksstatus/bulk")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"oppdateringer": [{"skjemaId": "${UUID.randomUUID()}", "saksnummer": "MEL-1", "saksstatus": "AVSLUTTET"}]}""")
                .exchange()
                .expectStatus().isForbidden
        }
    }
}
