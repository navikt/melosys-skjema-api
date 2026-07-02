package no.nav.melosys.skjema.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.utsendtArbeidstakerMetadataOrThrow
import no.nav.melosys.skjema.getToken
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.pdl.PdlClient
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlFoedselsdato
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlNavn
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.m2mTokenWithReadSkjemaDataAccess
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.service.NotificationService
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.tilleggsopplysningerDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.SkjemaData
import no.nav.melosys.skjema.types.SkjemaInnsendtKvittering
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Reproduserer at et nytt ARBEIDSTAKERS_DEL-skjema som erstatter et eldre
 * skjema arver `kobletSkjemaId` til en allerede innsendt arbeidsgivers del, slik at M2M-PDF-en
 * (journalføringen) feilaktig inneholder BEGGE deler — selv om innbyggeren bare sendte inn sin
 * egen arbeidstakers del.
 *
 * Testen er en ekte black-box e2e mot skjema-api:
 *  - henter ekte tokens fra MockOAuth2Server (TokenX for innbygger, Azure for M2M)
 *  - sender inn skjemaene via det virkelige `POST /{id}/send-inn`-endepunktet (kjører validering
 *    + [no.nav.melosys.skjema.service.UtsendtArbeidstakerSkjemaKoblingService.finnOgKobl])
 *  - henter ut PDF-en via det virkelige `GET /m2m/api/skjema/{id}/pdf`-endepunktet
 *
 * Kun integrasjoner uten verdi for selve bugen er mocket (Altinn/Ereg/Repr/Notification/PDL),
 * på samme måte som de øvrige controller-integrasjonstestene.
 */
class UtsendtArbeidstakerKoblingPdfReproIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @MockkBean
    private lateinit var notificationService: NotificationService

    @MockkBean
    private lateinit var altinnService: AltinnService

    @MockkBean
    private lateinit var eregService: EregService

    @MockkBean
    private lateinit var reprService: ReprService

    @MockkBean
    private lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()

        every { altinnService.harBrukerTilgang(any()) } returns true
        every { reprService.harSkriverettigheterForMedlemskap(any()) } returns true
        every { eregService.organisasjonsnummerEksisterer(any()) } returns true
        every { notificationService.sendNotificationToArbeidsgiver(any(), any(), any(), any()) } returns "beskjed-id"
        every { pdlClient.hentPerson(any()) } returns PdlPerson(
            navn = listOf(PdlNavn("Våken", null, "Elefant")),
            foedselsdato = listOf(PdlFoedselsdato("1980-01-01"))
        )
    }

    @Test
    @DisplayName("Arvet kobling påvirker ikke journalføring: PDF for ren arbeidstakers-del inneholder kun arbeidstakers del")
    fun `journalfoering styres av skjemadel ikke av kobletSkjema`() {
        // Innbygger-token (TokenX) for arbeidstakeren — samme person eier alle utkastene (opprettetAv).
        val innbyggerToken = mockOAuth2Server.getToken(claims = mapOf("pid" to korrektSyntetiskFnr))

        // --- 1) Innbygger (DEG_SELV) sender inn ARBEIDSTAKERS_DEL (v1) ---
        val arbeidstakerV1 = seedUtkast(
            representasjonstype = Representasjonstype.DEG_SELV,
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            data = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        )
        sendInn(arbeidstakerV1.id!!, innbyggerToken)

        // --- 2) Arbeidsgiver (ARBEIDSGIVER) sender inn ARBEIDSGIVERS_DEL ---
        // Overlappende periode + samme juridiske enhet => motpart-kobling mot v1.
        val arbeidsgiverDel = seedUtkast(
            representasjonstype = Representasjonstype.ARBEIDSGIVER,
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
            data = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
                utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
            )
        )
        sendInn(arbeidsgiverDel.id!!, innbyggerToken)

        // Nå er v1 og arbeidsgiverDel koblet sammen (komplett søknad).
        skjemaRepository.findById(arbeidsgiverDel.id!!).get()
            .utsendtArbeidstakerMetadataOrThrow().kobletSkjemaId shouldBe arbeidstakerV1.id

        // --- 3) Innbygger (DEG_SELV) sender inn NY ARBEIDSTAKERS_DEL (v2) som erstatter v1 ---
        // Innbyggeren fyller KUN ut sin egen del på nytt; ingen ny arbeidsgivers del sendes.
        val arbeidstakerV2 = seedUtkast(
            representasjonstype = Representasjonstype.DEG_SELV,
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            data = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        )
        sendInn(arbeidstakerV2.id!!, innbyggerToken)

        // --- 4) M2M: hent skjemadata for v2 (det melosys-api konsumerer) ---
        val m2mToken = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()
        val v2Data = hentM2MData(arbeidstakerV2.id!!, m2mToken)

        // v2 er en ren ARBEIDSTAKERS_DEL og har arvet kobling til arbeidsgivers del.
        // kobletSkjema eksponeres fortsatt på /data — melosys-api bruker det til samme-sak-vurdering.
        v2Data.skjema.metadata.skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
        val koblet = v2Data.kobletSkjema.shouldNotBeNull()
        koblet.id shouldBe arbeidsgiverDel.id
        koblet.metadata.skjemadel shouldBe Skjemadel.ARBEIDSGIVERS_DEL

        // --- 5) M2M: hent PDF for v2. Journalføringen skal gjenspeile skjemadel (ARBEIDSTAKERS_DEL),
        //         IKKE den arvede koblingen. Derfor: kun arbeidstakers del i PDF-en. ---
        val pdfTekst = hentM2MPdfTekst(arbeidstakerV2.id!!, m2mToken)

        pdfTekst shouldContain "Arbeidstakers del"
        pdfTekst shouldNotContain "Arbeidsgivers del"
    }

    @Test
    @DisplayName("Arbeidsgivers del sendt først, så arbeidstakers del: arbeidstakers PDF inneholder kun arbeidstakers del")
    fun `motpart-kobling - arbeidsgiver foerst - paavirker ikke journalfoering`() {
        val innbyggerToken = mockOAuth2Server.getToken(claims = mapOf("pid" to korrektSyntetiskFnr))

        // 1) Arbeidsgiver sender ARBEIDSGIVERS_DEL først — ingen motpart ennå, ingen kobling.
        val arbeidsgiverDel = seedUtkast(
            representasjonstype = Representasjonstype.ARBEIDSGIVER,
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
            data = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
                utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
                tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
            )
        )
        sendInn(arbeidsgiverDel.id!!, innbyggerToken)

        // 2) Arbeidstaker sender ARBEIDSTAKERS_DEL etterpå → motpart-kobles til arbeidsgivers del.
        val arbeidstakerDel = seedUtkast(
            representasjonstype = Representasjonstype.DEG_SELV,
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            data = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        )
        sendInn(arbeidstakerDel.id!!, innbyggerToken)

        val m2mToken = mockOAuth2Server.m2mTokenWithReadSkjemaDataAccess()

        // Arbeidstakers del er nå koblet til arbeidsgivers del (vanlig motpart-kobling, ingen ny versjon).
        val atData = hentM2MData(arbeidstakerDel.id!!, m2mToken)
        atData.skjema.metadata.skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
        atData.kobletSkjema.shouldNotBeNull().id shouldBe arbeidsgiverDel.id

        // Journalføringen skal kun gjenspeile arbeidstakers del — ikke den koblede arbeidsgiver-delen.
        // (Før fiks: PDF-en inneholder også "Arbeidsgivers del" → testen feiler.)
        val pdfTekst = hentM2MPdfTekst(arbeidstakerDel.id!!, m2mToken)
        pdfTekst shouldContain "Arbeidstakers del"
        pdfTekst shouldNotContain "Arbeidsgivers del"
    }

    private fun seedUtkast(
        representasjonstype: Representasjonstype,
        skjemadel: Skjemadel,
        data: SkjemaData
    ): Skjema = skjemaRepository.save(
        skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            type = SkjemaType.UTSENDT_ARBEIDSTAKER,
            data = data,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = representasjonstype,
                skjemadel = skjemadel
            )
        )
    )

    private fun sendInn(skjemaId: UUID, token: String): SkjemaInnsendtKvittering =
        webTestClient.post()
            .uri("/api/skjema/utsendt-arbeidstaker/$skjemaId/send-inn")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(SkjemaInnsendtKvittering::class.java)
            .returnResult().responseBody!!
            .also { it.status shouldBe SkjemaStatus.SENDT }

    private fun hentM2MData(skjemaId: UUID, token: String): UtsendtArbeidstakerSkjemaM2MDto =
        webTestClient.get()
            .uri("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UtsendtArbeidstakerSkjemaM2MDto::class.java)
            .returnResult().responseBody!!

    private fun hentM2MPdfTekst(skjemaId: UUID, token: String): String {
        val pdfBytes = webTestClient.get()
            .uri("/m2m/api/skjema/$skjemaId/pdf")
            .header("Authorization", "Bearer $token")
            .accept(MediaType.APPLICATION_PDF)
            .exchange()
            .expectStatus().isOk
            .expectBody(ByteArray::class.java)
            .returnResult().responseBody!!

        return Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }
    }
}
