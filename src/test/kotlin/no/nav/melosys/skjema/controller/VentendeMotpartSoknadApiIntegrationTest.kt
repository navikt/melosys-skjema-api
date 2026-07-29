package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.OrganisasjonMedJuridiskEnhetDto
import no.nav.melosys.skjema.types.felles.SimpleOrganisasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettUtsendtArbeidstakerSoknadResponse
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettetVia
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * API-tester for motpart-CTA-flyten: JSON-kontrakten for ventende motpart-søknader
 * og at `opprettetVia` fra opprett-requesten persisteres på skjemaet.
 */
class VentendeMotpartSoknadApiIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @MockkBean
    private lateinit var eregService: EregService

    @MockkBean
    private lateinit var pdlService: PdlService

    @Autowired
    private lateinit var unleash: Unleash

    @BeforeEach
    fun setUp() {
        skjemaRepository.deleteAll()
        (unleash as FakeUnleash).enableAll()
    }

    @Test
    @DisplayName("GET ventende-motpart-soknader returnerer JSON med arbeidsgiver, periode og innsendt dato")
    fun `henter ventende motpart-soknader som json`() {
        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                data = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()
                    .copy(utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier()),
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                )
            )
        )
        val token = mockOAuth2Server.getToken(claims = mapOf("pid" to korrektSyntetiskFnr))

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/ventende-motpart-soknader")
            .headers { it.setBearerAuth(token) }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.soknader.length()").isEqualTo(1)
            .jsonPath("$.soknader[0].skjemaId").isEqualTo(skjema.id.toString())
            .jsonPath("$.soknader[0].arbeidsgiverNavn").isEqualTo("Test Arbeidsgiver AS")
            .jsonPath("$.soknader[0].arbeidsgiverOrgnr").isEqualTo(korrektSyntetiskOrgnr)
            .jsonPath("$.soknader[0].utsendingsperiode.fraDato").isEqualTo("2024-01-01")
            .jsonPath("$.soknader[0].utsendingsperiode.tilDato").isEqualTo("2024-12-31")
            .jsonPath("$.soknader[0].innsendtDato").isNotEmpty
    }

    @Test
    @DisplayName("opprettetVia fra opprett-requesten persisteres på skjemaet")
    fun `opprettetVia persisteres ved opprettelse`() {
        every { eregService.organisasjonsnummerEksisterer(korrektSyntetiskOrgnr) } returns true
        every { eregService.hentOrganisasjonMedJuridiskEnhet(korrektSyntetiskOrgnr) } returns OrganisasjonMedJuridiskEnhetDto(
            organisasjon = SimpleOrganisasjonDto(orgnr = korrektSyntetiskOrgnr, navn = "Test Arbeidsgiver AS"),
            juridiskEnhet = SimpleOrganisasjonDto(orgnr = korrektSyntetiskOrgnr, navn = "Test Arbeidsgiver AS")
        )
        every { pdlService.hentNavn(korrektSyntetiskFnr) } returns "Test Testesen"
        val token = mockOAuth2Server.getToken(claims = mapOf("pid" to korrektSyntetiskFnr))

        val response = webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/opprett-med-kontekst")
            .headers { it.setBearerAuth(token) }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "representasjonstype": "DEG_SELV",
                  "radgiverfirma": null,
                  "arbeidsgiver": {"orgnr": "$korrektSyntetiskOrgnr", "navn": "Test Arbeidsgiver AS"},
                  "arbeidstaker": {"fnr": "$korrektSyntetiskFnr", "etternavn": "Testesen"},
                  "opprettetVia": "MOTPART_CTA"
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody(OpprettUtsendtArbeidstakerSoknadResponse::class.java)
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        val lagret = skjemaRepository.findById(response.id).orElseThrow()
        lagret.opprettetVia shouldBe OpprettetVia.MOTPART_CTA
    }

    @Test
    @DisplayName("Uten opprettetVia i requesten forblir feltet null")
    fun `opprettetVia er null ved ordinaer opprettelse`() {
        every { eregService.organisasjonsnummerEksisterer(korrektSyntetiskOrgnr) } returns true
        every { eregService.hentOrganisasjonMedJuridiskEnhet(korrektSyntetiskOrgnr) } returns OrganisasjonMedJuridiskEnhetDto(
            organisasjon = SimpleOrganisasjonDto(orgnr = korrektSyntetiskOrgnr, navn = "Test Arbeidsgiver AS"),
            juridiskEnhet = SimpleOrganisasjonDto(orgnr = korrektSyntetiskOrgnr, navn = "Test Arbeidsgiver AS")
        )
        every { pdlService.hentNavn(korrektSyntetiskFnr) } returns "Test Testesen"
        val token = mockOAuth2Server.getToken(claims = mapOf("pid" to korrektSyntetiskFnr))

        val response = webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/opprett-med-kontekst")
            .headers { it.setBearerAuth(token) }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "representasjonstype": "DEG_SELV",
                  "radgiverfirma": null,
                  "arbeidsgiver": {"orgnr": "$korrektSyntetiskOrgnr", "navn": "Test Arbeidsgiver AS"},
                  "arbeidstaker": {"fnr": "$korrektSyntetiskFnr", "etternavn": "Testesen"}
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody(OpprettUtsendtArbeidstakerSoknadResponse::class.java)
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        skjemaRepository.findById(response.id).orElseThrow().opprettetVia shouldBe null
    }
}
