package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
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
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
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
            .jsonPath("$.soknader[0].utsendelseLand").isEqualTo("SE")
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
    @DisplayName("prefyllFraSkjemaId kopierer land og periode fra egen innsendt arbeidsgiver-del")
    fun `prefyller land og periode fra arbeidsgiver-delen`() {
        mockOpprettAvhengigheter()
        val agDel = skjemaRepository.save(
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

        val response = opprettSoknad(token, """"prefyllFraSkjemaId": "${agDel.id}", "opprettetVia": "MOTPART_CTA"""")

        val lagret = skjemaRepository.findById(response.id).orElseThrow()
        val data = lagret.data as UtsendtArbeidstakerArbeidstakersSkjemaDataDto
        data.utsendingsperiodeOgLand shouldBe utsendingsperiodeOgLandDtoMedDefaultVerdier()

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${response.id}")
            .headers { it.setBearerAuth(token) }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.opprettetVia").isEqualTo("MOTPART_CTA")
            .jsonPath("$.data.utsendingsperiodeOgLand.utsendelseLand").isEqualTo("SE")
            .jsonPath("$.data.utsendingsperiodeOgLand.utsendelsePeriode.fraDato").isEqualTo("2024-01-01")
            .jsonPath("$.motpartensUtsendingsperiodeOgLand.utsendelseLand").isEqualTo("SE")
            .jsonPath("$.motpartensUtsendingsperiodeOgLand.utsendelsePeriode.fraDato").isEqualTo("2024-01-01")
    }

    @Test
    @DisplayName("Motpartens oppgitte verdier består etter at bruker har overskrevet sine egne")
    fun `motpartens verdier bestaar etter overskriving`() {
        mockOpprettAvhengigheter()
        val agDel = skjemaRepository.save(
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
        val response = opprettSoknad(token, """"prefyllFraSkjemaId": "${agDel.id}"""")

        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/${response.id}/utsendingsperiode-og-land")
            .headers { it.setBearerAuth(token) }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"utsendelseLand": "DE", "utsendelsePeriode": {"fraDato": "2025-03-01", "tilDato": "2025-09-30"}}""")
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/skjema/utsendt-arbeidstaker/${response.id}")
            .headers { it.setBearerAuth(token) }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.utsendingsperiodeOgLand.utsendelseLand").isEqualTo("DE")
            .jsonPath("$.motpartensUtsendingsperiodeOgLand.utsendelseLand").isEqualTo("SE")
            .jsonPath("$.motpartensUtsendingsperiodeOgLand.utsendelsePeriode.fraDato").isEqualTo("2024-01-01")
    }

    @Test
    @DisplayName("prefyllFraSkjemaId som ikke er egen innsendt arbeidsgiver-del ignoreres")
    fun `prefyll ignoreres for ugyldige kilder`() {
        mockOpprettAvhengigheter()
        val annenPersonsAgDel = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
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

        val response = opprettSoknad(token, """"prefyllFraSkjemaId": "${annenPersonsAgDel.id}"""")

        skjemaRepository.findById(response.id).orElseThrow().data shouldBe null
    }

    private fun opprettSoknad(token: String, ekstraFelter: String): OpprettUtsendtArbeidstakerSoknadResponse {
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
                  $ekstraFelter
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody(OpprettUtsendtArbeidstakerSoknadResponse::class.java)
            .returnResult()
            .responseBody

        response.shouldNotBeNull()
        return response
    }

    private fun mockOpprettAvhengigheter() {
        every { eregService.organisasjonsnummerEksisterer(korrektSyntetiskOrgnr) } returns true
        every { eregService.hentOrganisasjonMedJuridiskEnhet(korrektSyntetiskOrgnr) } returns OrganisasjonMedJuridiskEnhetDto(
            organisasjon = SimpleOrganisasjonDto(orgnr = korrektSyntetiskOrgnr, navn = "Test Arbeidsgiver AS"),
            juridiskEnhet = SimpleOrganisasjonDto(orgnr = korrektSyntetiskOrgnr, navn = "Test Arbeidsgiver AS")
        )
        every { pdlService.hentNavn(korrektSyntetiskFnr) } returns "Test Testesen"
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
