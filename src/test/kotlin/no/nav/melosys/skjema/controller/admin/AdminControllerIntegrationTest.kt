package no.nav.melosys.skjema.controller.admin

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.adminTokenMedTilgang
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.kafka.BrukervarselMelding
import no.nav.melosys.skjema.kafka.BrukervarselProducer
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.m2mTokenWithoutAccess
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.InnsendingService
import no.nav.melosys.skjema.sikkerhet.AdminApiKeyInterceptor.Companion.API_KEY_HEADER
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettetVia
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import no.nav.security.mock.oauth2.MockOAuth2Server

private const val TEST_ADMIN_APIKEY = "test-admin-apikey"

class AdminControllerIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockkBean(relaxed = true)
    private lateinit var innsendingService: InnsendingService

    @MockkBean(relaxed = true)
    private lateinit var brukervarselProducer: BrukervarselProducer

    /** WebTestClient som sender gyldig admin-API-nøkkel på alle kall (jf. application-test.yml). */
    private val adminClient by lazy {
        webTestClient.mutate().defaultHeader(API_KEY_HEADER, TEST_ADMIN_APIKEY).build()
    }

    @BeforeEach
    fun setUp() {
        // Native delete (cascade) – rydder skjema-tabellen før hver test.
        jdbcTemplate.update("DELETE FROM skjema")
    }

    private fun lagFeiletInnsending(referanseId: String = "FEIL01") =
        skjemaRepository.save(
            skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
        ).let { skjema ->
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    status = InnsendingStatus.KAFKA_FEILET,
                    antallForsok = 3,
                    feilmelding = "Kafka utilgjengelig",
                    referanseId = referanseId
                )
            )
        }

    @Nested
    @DisplayName("Sikkerhet")
    inner class Sikkerhet {

        @Test
        fun `skal returnere 401 naar token mangler`() {
            adminClient.get().uri("/admin/statistikk")
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            adminClient.get().uri("/admin/statistikk")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `skal returnere 403 naar API-noekkel mangler selv med gyldig token`() {
            webTestClient.get().uri("/admin/statistikk")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `skal returnere 403 naar API-noekkel er feil`() {
            webTestClient.get().uri("/admin/statistikk")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .header(API_KEY_HEADER, "feil-noekkel")
                .exchange()
                .expectStatus().isForbidden
        }
    }

    @Nested
    @DisplayName("GET /admin/statistikk")
    inner class Statistikk {

        @Test
        fun `skal returnere antall per status`() {
            lagFeiletInnsending()

            val body = adminClient.get().uri("/admin/statistikk")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<AdminStatistikkDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.skjemaPerStatus[SkjemaStatus.SENDT] shouldBe 1
            body.innsendingPerStatus[InnsendingStatus.KAFKA_FEILET] shouldBe 1
            body.antallFeiledeInnsendinger shouldBe 1
        }
    }

    @Nested
    @DisplayName("GET /admin/innsendinger/feilede")
    inner class FeiledeInnsendinger {

        @Test
        fun `skal returnere feilede innsendinger uten personopplysninger`() {
            val innsending = lagFeiletInnsending()

            val body = adminClient.get().uri("/admin/innsendinger/feilede")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<List<InnsendingAdminDto>>()
                .returnResult().responseBody.shouldNotBeNull()

            body shouldHaveSize 1
            body[0].innsendingId shouldBe innsending.id
            body[0].status shouldBe InnsendingStatus.KAFKA_FEILET
            body[0].feilmelding shouldBe "Kafka utilgjengelig"
            body[0].antallForsok shouldBe 3
        }

        @Test
        fun `skal returnere antall feilede`() {
            lagFeiletInnsending("FEIL01")
            lagFeiletInnsending("FEIL02")

            val body = adminClient.get().uri("/admin/innsendinger/feilede/antall")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<AntallDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.antall shouldBe 2
        }
    }

    @Nested
    @DisplayName("GET /admin/innsendinger/{id}")
    inner class HentInnsending {

        @Test
        fun `skal returnere innsending`() {
            val innsending = lagFeiletInnsending()

            adminClient.get().uri("/admin/innsendinger/${innsending.id}")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<InnsendingAdminDto>()
                .returnResult().responseBody.shouldNotBeNull()
                .innsendingId shouldBe innsending.id
        }

        @Test
        fun `skal returnere 404 naar innsending ikke finnes`() {
            adminClient.get().uri("/admin/innsendinger/${UUID.randomUUID()}")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("POST /admin/innsendinger/{id}/retry")
    inner class RetryInnsending {

        @Test
        fun `skal reprosessere innsending og returnere 200`() {
            val innsending = lagFeiletInnsending()
            val skjemaId = innsending.skjema.id!!

            adminClient.post().uri("/admin/innsendinger/${innsending.id}/retry")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk

            verify(exactly = 1) { innsendingService.prosesserInnsending(skjemaId) }
        }

        @Test
        fun `skal returnere 404 naar innsending ikke finnes`() {
            adminClient.post().uri("/admin/innsendinger/${UUID.randomUUID()}/retry")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("POST /admin/innsendinger/retry-feilede")
    inner class RetryAlleFeilede {

        @Test
        fun `skal reprosessere alle feilede og returnere antall`() {
            val innsending1 = lagFeiletInnsending("FEIL01")
            val innsending2 = lagFeiletInnsending("FEIL02")
            every { innsendingService.prosesserInnsending(any()) } returns Unit

            val body = adminClient.post().uri("/admin/innsendinger/retry-feilede")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<RetryResultatDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.antallForsoekt shouldBe 2
            body.antallFeilet shouldBe 0
            verify(exactly = 1) { innsendingService.prosesserInnsending(innsending1.skjema.id!!) }
            verify(exactly = 1) { innsendingService.prosesserInnsending(innsending2.skjema.id!!) }
        }
    }

    @Nested
    @DisplayName("GET /admin/statistikk/bruk")
    inner class Bruksstatistikk {

        private val periodeA = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30"))
        private val periodeOverlapp = PeriodeDto(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-08-31"))
        private val periodeSenere = PeriodeDto(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-31"))

        /** Lager et innsendt (SENDT) skjema med full kontroll på fnr, virksomhet, del, periode og innsendingsdato. */
        private fun lagInnsendt(
            skjemadel: Skjemadel,
            fnr: String = "10000000001",
            orgnr: String = korrektSyntetiskOrgnr,
            juridiskEnhet: String = orgnr,
            periode: PeriodeDto? = periodeA,
            representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
            sprak: Språk = Språk.NORSK_BOKMAL,
            erstatterSkjemaId: UUID? = null,
            innsendtDato: Instant = Instant.now(),
            utkastStartet: Instant = innsendtDato,
            innsenderFnr: String = "12345678901",
            saksstatus: Saksstatus? = null,
            saksnummer: String? = null,
            opprettetVia: OpprettetVia? = null
        ): Skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = fnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                    utsendingsperiodeOgLand = periode?.let { utsendingsperiodeOgLandDtoMedDefaultVerdier().copy(utsendelsePeriode = it) }
                ),
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = representasjonstype,
                    skjemadel = skjemadel,
                    juridiskEnhetOrgnr = juridiskEnhet,
                    erstatterSkjemaId = erstatterSkjemaId
                ),
                opprettetVia = opprettetVia,
                opprettetDato = utkastStartet
            )
        ).also { skjema ->
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    innsendtSprak = sprak,
                    opprettetDato = innsendtDato,
                    innsenderFnr = innsenderFnr,
                    saksstatus = saksstatus,
                    saksnummer = saksnummer
                )
            )
        }

        /** Lager et påbegynt utkast (status UTKAST) for å teste venter-trakten. */
        private fun lagUtkast(skjemadel: Skjemadel, fnr: String, juridiskEnhet: String = korrektSyntetiskOrgnr) =
            skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = fnr,
                    status = SkjemaStatus.UTKAST,
                    metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(skjemadel = skjemadel, juridiskEnhetOrgnr = juridiskEnhet)
                )
            )

        private fun hentBruk(fraOgMed: String? = null, tilOgMed: String? = null): BrukStatistikkDto =
            adminClient.get().uri { b ->
                b.path("/admin/statistikk/bruk")
                fraOgMed?.let { b.queryParam("fraOgMed", it) }
                tilOgMed?.let { b.queryParam("tilOgMed", it) }
                b.build()
            }
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<BrukStatistikkDto>()
                .returnResult().responseBody.shouldNotBeNull()

        @Test
        fun `skal aggregere utkast, innsendte per skjemadel-flyt-spraak, trend og unike`() {
            // Utkast: ett ferskt og ett gammelt (>30 dager)
            skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.UTKAST, opprettetDato = Instant.now()))
            skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.UTKAST, opprettetDato = Instant.now().minus(40, ChronoUnit.DAYS)))

            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "10000000001", representasjonstype = Representasjonstype.DEG_SELV)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "10000000002", representasjonstype = Representasjonstype.ARBEIDSGIVER, sprak = Språk.ENGELSK)
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "10000000003", representasjonstype = Representasjonstype.RADGIVER)

            val body = hentBruk()

            body.utkast.antall shouldBe 2
            body.utkast.under1Dag shouldBe 1
            body.utkast.over30Dager shouldBe 1
            body.utkast.mellom1Og7Dager shouldBe 0
            body.utkast.eldsteOpprettetDato.shouldNotBeNull()
            body.utkast.perSkjemadel[Skjemadel.ARBEIDSTAKERS_DEL] shouldBe 2

            body.totaltInnsendt shouldBe 3
            body.innsendtPerSkjemadel[Skjemadel.ARBEIDSTAKERS_DEL] shouldBe 1
            body.innsendtPerSkjemadel[Skjemadel.ARBEIDSGIVERS_DEL] shouldBe 1
            body.innsendtPerSkjemadel[Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL] shouldBe 1

            body.innsendtPerFlyt[Representasjonstype.DEG_SELV] shouldBe 1
            body.innsendtPerFlyt[Representasjonstype.ARBEIDSGIVER] shouldBe 1
            body.innsendtPerFlyt[Representasjonstype.RADGIVER] shouldBe 1
            body.innsendtPerFlyt[Representasjonstype.ANNEN_PERSON] shouldBe 0

            body.innsendtPerSprak[Språk.NORSK_BOKMAL] shouldBe 2
            body.innsendtPerSprak[Språk.ENGELSK] shouldBe 1

            body.innsendtSisteDoegn shouldBe 3
            body.antallUnikePersoner shouldBe 3
            body.antallUnikeVirksomheter shouldBe 1
        }

        @Test
        fun `saksdekning - komplett, matchende separate deler og uten motpart`() {
            // Komplett (begge deler i ett)
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "20000000001")
            // Sak med begge deler hver for seg (samme person + enhet + overlappende periode)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "20000000002", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "20000000002", periode = periodeOverlapp)
            // Kun arbeidstaker-del (ingen motpart)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "20000000003")
            // Kun arbeidsgiver-del (ingen motpart)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "20000000004")

            val s = hentBruk().saksdekning

            s.antallKomplette shouldBe 1
            s.antallSakerMedBeggeDeler shouldBe 2 // 1 komplett + 1 matchende separat sak
            s.arbeidstakerDeler.medMotpart shouldBe 1
            s.arbeidsgiverDeler.medMotpart shouldBe 1
            s.arbeidstakerDeler.venterIngenMotpart shouldBe 1 // P3, ingen motpart
            s.arbeidsgiverDeler.venterIngenMotpart shouldBe 1 // P4, ingen motpart
            s.antallMuligeDobbeltinnsendinger shouldBe 0
        }

        @Test
        fun `saksdekning - ventende del der motparten har paabegynt utkast`() {
            // Arbeidsgiver har sendt sin del, men arbeidstaker har bare PÅBEGYNT et utkast
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "55000000001")
            lagUtkast(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "55000000001")
            // Arbeidsgiver har sendt sin del, og ingen motpart finnes (verken sendt eller utkast)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "55000000002")

            val s = hentBruk().saksdekning
            s.arbeidsgiverDeler.totalt shouldBe 2
            s.arbeidsgiverDeler.medMotpart shouldBe 0
            s.arbeidsgiverDeler.venterMotpartHarUtkast shouldBe 1 // har påbegynt utkast
            s.arbeidsgiverDeler.venterIngenMotpart shouldBe 1
        }

        @Test
        fun `saksdekning - flere versjoner av samme del telles som sak med flere versjoner`() {
            val gammel = lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "58000000001", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "58000000001", periode = periodeOverlapp, erstatterSkjemaId = gammel.id)

            hentBruk().saksdekning.antallSakerMedFlereVersjoner shouldBe 1
        }

        @Test
        fun `saksdekning - samme sak med komplett og separate deler telles kun en gang`() {
            val fnr = "50000000001"
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = fnr)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = periodeOverlapp)

            val s = hentBruk().saksdekning
            s.antallKomplette shouldBe 1
            s.antallSakerMedBeggeDeler shouldBe 1 // samme (fnr, juridisk enhet) – ikke dobbelttalt
        }

        @Test
        fun `saksdekning - mulige dobbeltinnsendinger telles per tilfelle, ikke per rad`() {
            // Ekte dobbeltinnsending: samme person sender arbeidsgivers del to ganger, overlappende periode
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "30000000001", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "30000000001", periode = periodeOverlapp)

            // Versjon-erstatning: ny arbeidstaker-del erstatter en eldre (skal IKKE telles som duplikat)
            val gammel = lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "30000000002", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "30000000002", periode = periodeOverlapp, erstatterSkjemaId = gammel.id)

            val s = hentBruk().saksdekning
            s.antallMuligeDobbeltinnsendinger shouldBe 1 // ett tilfelle (to rader)
            s.muligeDobbeltinnsendinger.single().antallInnsendinger shouldBe 2
        }

        @Test
        fun `duplikat-tilfeller lister saksnumre, og skjema-id naar saksnummer mangler`() {
            // Tre overlappende arbeidsgiver-deler for samme sak = ETT tilfelle med tre innsendinger
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "31000000001", periode = periodeA, saksnummer = "SAK-D1")
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "31000000001", periode = periodeOverlapp, saksnummer = "SAK-D2")
            val utenSaksnummer = lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "31000000001", periode = periodeA)
            // Et eget tilfelle for en annen sak – to arbeidstaker-deler
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "31000000002", periode = periodeA, saksnummer = "SAK-E1")
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "31000000002", periode = periodeOverlapp, saksnummer = "SAK-E2")

            val s = hentBruk().saksdekning
            s.antallMuligeDobbeltinnsendinger shouldBe 2
            s.muligeDobbeltinnsendinger shouldHaveSize 2
            val treer = s.muligeDobbeltinnsendinger.single { it.antallInnsendinger == 3 }
            treer.saksnumre shouldContainExactlyInAnyOrder listOf("SAK-D1", "SAK-D2", utenSaksnummer.id.toString())
            s.muligeDobbeltinnsendinger.single { it.antallInnsendinger == 2 }
                .saksnumre shouldContainExactlyInAnyOrder listOf("SAK-E1", "SAK-E2")
        }

        @Test
        fun `kohort - del i vinduet matcher motpart utenfor vinduet`() {
            val iVinduet = Instant.parse("2026-03-15T10:00:00Z")
            val utenforVinduet = Instant.parse("2026-06-15T10:00:00Z")
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "33000000001", periode = periodeA, innsendtDato = iVinduet)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "33000000001", periode = periodeOverlapp, innsendtDato = utenforVinduet)

            val s = hentBruk(fraOgMed = "2026-03-01", tilOgMed = "2026-03-31").saksdekning
            with(s.arbeidstakerDeler) {
                totalt shouldBe 1
                medMotpart shouldBe 1
                venterIngenMotpart shouldBe 0
                venterMotpartHarUtkast shouldBe 0
            }
            s.arbeidsgiverDeler.totalt shouldBe 0
            s.antallSakerMedBeggeDeler shouldBe 1
            s.antallSakerMedMatchendeSeparateDeler shouldBe 1
        }

        @Test
        fun `erstattede versjoner holdes utenfor totalt og avstemmes mot innsendtPerSkjemadel`() {
            val gammel = lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "34000000001", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "34000000001", periode = periodeOverlapp, erstatterSkjemaId = gammel.id)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "34000000002", periode = periodeA)

            val body = hentBruk()
            body.totaltInnsendt shouldBe 3 // fordelingene beholder alle innsendinger
            with(body.saksdekning.arbeidsgiverDeler) {
                totalt shouldBe 1
                antallErstattedeVersjoner shouldBe 1
                totalt + antallErstattedeVersjoner shouldBe body.innsendtPerSkjemadel[Skjemadel.ARBEIDSGIVERS_DEL]
            }
            with(body.saksdekning.arbeidstakerDeler) {
                antallErstattedeVersjoner shouldBe 0
                totalt + antallErstattedeVersjoner shouldBe body.innsendtPerSkjemadel[Skjemadel.ARBEIDSTAKERS_DEL]
            }
        }

        @Test
        fun `separat del dekkes av komplett skjema og venter dermed ikke`() {
            // Arbeidsgiver sendte sin del, mens arbeidstakerens del kom via et komplett skjema
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "35000000001", periode = periodeA, saksstatus = Saksstatus.MOTTATT)
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "35000000001", periode = periodeOverlapp)
            // Samme, men saken er avsluttet
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "35000000002", periode = periodeA, saksstatus = Saksstatus.AVSLUTTET)
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "35000000002", periode = periodeOverlapp)
            // Komplett skjema uten overlappende periode dekker ikke delen
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "35000000003", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "35000000003", periode = periodeSenere)

            val s = hentBruk().saksdekning
            with(s.arbeidsgiverDeler) {
                totalt shouldBe 3
                medMotpart shouldBe 0
                dekketAvKomplettSkjema shouldBe 2
                dekketAvKomplettSkjemaAktivSak shouldBe 1
                dekketAvKomplettSkjemaAvsluttetSak shouldBe 1
                venterIngenMotpart shouldBe 1
                totalt shouldBe medMotpart + dekketAvKomplettSkjema + venterMotpartHarUtkast + venterIngenMotpart
            }
        }

        @Test
        fun `dekomponering av saker med begge deler gaar opp`() {
            // Kun komplett
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "36000000001")
            // Kun matchende separate deler
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "36000000002", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "36000000002", periode = periodeOverlapp)
            // Både komplett og matchende separate deler
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "36000000003")
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "36000000003", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "36000000003", periode = periodeOverlapp)
            // Udekket sak
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "36000000004", periode = periodeA)

            val s = hentBruk().saksdekning
            s.antallSakerMedKomplett shouldBe 2
            s.antallSakerMedMatchendeSeparateDeler shouldBe 2
            s.antallSakerMedBaadeKomplettOgSeparate shouldBe 1
            s.antallSakerMedBeggeDeler shouldBe 3
            s.antallSakerMedBeggeDeler shouldBe
                s.antallSakerMedKomplett + s.antallSakerMedMatchendeSeparateDeler - s.antallSakerMedBaadeKomplettOgSeparate
        }

        @Test
        fun `initiativ - arbeidsgiver foerst, arbeidstaker foerst og uavhengig`() {
            val t0 = Instant.parse("2026-02-01T08:00:00Z")
            val t1 = Instant.parse("2026-02-02T08:00:00Z")
            val t2 = Instant.parse("2026-02-03T08:00:00Z")
            val t3 = Instant.parse("2026-02-04T08:00:00Z")

            // Arbeidsgiver sendte inn først; arbeidstakerens utkast ble startet etterpå
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "37000000001", periode = periodeA, utkastStartet = t0, innsendtDato = t1)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "37000000001", periode = periodeOverlapp, utkastStartet = t2, innsendtDato = t3)

            // Arbeidstaker sendte inn først; arbeidsgiverens utkast ble startet etterpå
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "37000000002", periode = periodeA, utkastStartet = t0, innsendtDato = t1)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "37000000002", periode = periodeOverlapp, utkastStartet = t2, innsendtDato = t3)

            // Begge startet utkastet før noen av delene ble sendt inn
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "37000000003", periode = periodeA, utkastStartet = t0, innsendtDato = t3)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "37000000003", periode = periodeOverlapp, utkastStartet = t1, innsendtDato = t3)

            val s = hentBruk().saksdekning
            s.parInitiertAvArbeidsgiver shouldBe 1
            s.parInitiertAvArbeidstaker shouldBe 1
            s.parUavhengigStartet shouldBe 1
            s.parInitiertAvArbeidsgiver + s.parInitiertAvArbeidstaker + s.parUavhengigStartet shouldBe
                s.antallSakerMedMatchendeSeparateDeler
        }

        @Test
        fun `initiativ - maales paa det matchende paret, ikke paa et tidligere par som aldri matchet`() {
            val fnr = "43000000001"
            // Første utsendelse: AT og AG har perioder som verken overlapper hverandre eller den senere utsendelsen
            val tidligAtPeriode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-28"))
            val tidligAgPeriode = PeriodeDto(LocalDate.parse("2026-04-01"), LocalDate.parse("2026-05-31"))
            // Andre utsendelse: AT og AG overlapper hverandre – det eneste faktiske paret
            val senAgPeriode = PeriodeDto(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-12-31"))
            val senAtPeriode = PeriodeDto(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-31"))
            val t0 = Instant.parse("2026-02-01T08:00:00Z")
            val t1 = Instant.parse("2026-02-02T08:00:00Z")
            val t2 = Instant.parse("2026-02-03T08:00:00Z")
            val t3 = Instant.parse("2026-02-04T08:00:00Z")
            val t4 = Instant.parse("2026-02-05T08:00:00Z")
            val t5 = Instant.parse("2026-02-06T08:00:00Z")

            // Ikke-matchende par, tidligst i tid: begge startet før noen innsending (ville gitt "uavhengig")
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = tidligAtPeriode, utkastStartet = t0, innsendtDato = t1)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = tidligAgPeriode, utkastStartet = t0, innsendtDato = t1)
            // Matchende par, senere: arbeidsgiver sendte inn før arbeidstakers utkast ble startet
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = senAgPeriode, utkastStartet = t2, innsendtDato = t3)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = senAtPeriode, utkastStartet = t4, innsendtDato = t5)

            val s = hentBruk().saksdekning
            s.antallSakerMedMatchendeSeparateDeler shouldBe 1
            s.parInitiertAvArbeidsgiver shouldBe 1
            s.parInitiertAvArbeidstaker shouldBe 0
            s.parUavhengigStartet shouldBe 0
        }

        @Test
        fun `initiativ - flere utsendelser paa samme sak klassifiseres etter den tidligste klyngen`() {
            val fnr = "45000000001"
            // Utsendelse 1 (tidligst INNSENDT, men senest i periode): arbeidsgiver sendte inn før
            // arbeidstaker startet utkastet
            val ag1Periode = PeriodeDto(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-12-31"))
            val at1Periode = PeriodeDto(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-31"))
            // Utsendelse 2 (tidligst i periode, men sist innsendt): eget par, med et arbeidstaker-utkast
            // som ble startet lenge før alt annet
            val at2Periode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-28"))
            val ag2Periode = PeriodeDto(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-03-31"))
            val t0 = Instant.parse("2026-01-01T08:00:00Z")
            val t1 = Instant.parse("2026-01-02T08:00:00Z")
            val t2 = Instant.parse("2026-01-03T08:00:00Z")
            val t3 = Instant.parse("2026-01-04T08:00:00Z")
            val t4 = Instant.parse("2026-01-05T08:00:00Z")
            val t5 = Instant.parse("2026-01-06T08:00:00Z")
            val t6 = Instant.parse("2026-01-07T08:00:00Z")
            val t7 = Instant.parse("2026-01-08T08:00:00Z")

            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = ag1Periode, utkastStartet = t2, innsendtDato = t3)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at1Periode, utkastStartet = t4, innsendtDato = t5)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at2Periode, utkastStartet = t0, innsendtDato = t6)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = ag2Periode, utkastStartet = t1, innsendtDato = t7)

            val s = hentBruk().saksdekning
            s.antallSakerMedMatchendeSeparateDeler shouldBe 1
            // Utsendelse 1 vinner fordi den ble innsendt først – ikke fordi perioden starter først
            s.parInitiertAvArbeidsgiver shouldBe 1
            s.parInitiertAvArbeidstaker shouldBe 0
            s.parUavhengigStartet shouldBe 0
        }

        @Test
        fun `initiativ - del uten motpart som henger paa klyngen paavirker ikke klassifiseringen`() {
            val fnr = "47000000001"
            // AT0 overlapper AT1, men ikke AG1 – den kobles transitivt inn i klyngen uten å ha en motpart
            val at0Periode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-31"))
            val at1Periode = PeriodeDto(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-04-30"))
            val ag1Periode = PeriodeDto(LocalDate.parse("2026-04-01"), LocalDate.parse("2026-05-31"))
            val t0 = Instant.parse("2026-01-01T08:00:00Z")
            val t1 = Instant.parse("2026-01-02T08:00:00Z")
            val t2 = Instant.parse("2026-01-03T08:00:00Z")
            val t3 = Instant.parse("2026-01-04T08:00:00Z")
            val t4 = Instant.parse("2026-01-05T08:00:00Z")
            val t5 = Instant.parse("2026-01-06T08:00:00Z")

            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at0Periode, utkastStartet = t0, innsendtDato = t1)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = ag1Periode, utkastStartet = t2, innsendtDato = t3)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at1Periode, utkastStartet = t4, innsendtDato = t5)

            val s = hentBruk().saksdekning
            s.antallSakerMedMatchendeSeparateDeler shouldBe 1
            // Kun AT1/AG1 utgjør paret: AT1 ble startet etter at AG1 var innsendt
            s.parInitiertAvArbeidsgiver shouldBe 1
            s.parInitiertAvArbeidstaker shouldBe 0
            s.parUavhengigStartet shouldBe 0
        }

        @Test
        fun `initiativ - erstattet versjon med bred periode limer ikke sammen to utsendelser`() {
            val fnr = "48000000001"
            val at1Periode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-28"))
            val ag1Periode = PeriodeDto(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-03-31"))
            val ag2Periode = PeriodeDto(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-12-31"))
            val at2Periode = PeriodeDto(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-31"))
            // Erstattet versjon av utsendelse 2 sin AG-del, med tastefeil-periode som dekker hele året
            val feilPeriode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"))

            // Utsendelse 1 (innsendt først): arbeidstaker sendte inn før arbeidsgiver startet utkastet
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at1Periode,
                utkastStartet = Instant.parse("2026-01-01T08:00:00Z"), innsendtDato = Instant.parse("2026-01-03T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = ag1Periode,
                utkastStartet = Instant.parse("2026-01-04T08:00:00Z"), innsendtDato = Instant.parse("2026-01-05T08:00:00Z")
            )
            // Utsendelse 2: arbeidsgiverens utkast ble startet tidlig, men sendt inn sist. Erstatterens
            // utkast-start kan godt ligge før originalens – koblingen settes ved innsending, ikke ved
            // utkast-opprettelse.
            val gammelAg = lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = feilPeriode,
                utkastStartet = Instant.parse("2026-01-06T08:00:00Z"), innsendtDato = Instant.parse("2026-01-07T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = ag2Periode, erstatterSkjemaId = gammelAg.id,
                utkastStartet = Instant.parse("2026-01-02T08:00:00Z"), innsendtDato = Instant.parse("2026-01-08T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at2Periode,
                utkastStartet = Instant.parse("2026-01-09T08:00:00Z"), innsendtDato = Instant.parse("2026-01-10T08:00:00Z")
            )

            val s = hentBruk().saksdekning
            s.antallSakerMedMatchendeSeparateDeler shouldBe 1
            // Utsendelse 1 er den tidligste gjeldende utsendelsen, og der startet arbeidstakeren
            s.parInitiertAvArbeidstaker shouldBe 1
            s.parInitiertAvArbeidsgiver shouldBe 0
            s.parUavhengigStartet shouldBe 0
        }

        @Test
        fun `initiativ - tidligere versjon av en matchende del teller med, men bare i sin egen utsendelse`() {
            // Sak A: arbeidsgiveren sendte inn tidlig, korrigerte senere. Uten versjonskjeden ville den
            // korrigerte versjonens tidspunkter gjort arbeidstakeren til initiativtaker.
            val aFnr = "50000000001"
            val aV1Periode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-31"))
            val aParPeriode = PeriodeDto(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-04-30"))
            val aV1 = lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = aFnr, periode = aV1Periode,
                utkastStartet = Instant.parse("2026-01-01T08:00:00Z"), innsendtDato = Instant.parse("2026-01-02T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL, fnr = aFnr, periode = aParPeriode,
                utkastStartet = Instant.parse("2026-01-03T08:00:00Z"), innsendtDato = Instant.parse("2026-01-04T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = aFnr, periode = aParPeriode, erstatterSkjemaId = aV1.id,
                utkastStartet = Instant.parse("2026-01-05T08:00:00Z"), innsendtDato = Instant.parse("2026-01-06T08:00:00Z")
            )

            // Sak B: samme tidsbilde, men den erstattede AG-versjonen ble korrigert inn i en ANNEN
            // utsendelse – da skal den ikke telle i den valgte klyngen
            val bFnr = "50000000002"
            val bGammelPeriode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-31"))
            val bParPeriode = PeriodeDto(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-04-30"))
            val bSenAgPeriode = PeriodeDto(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-12-31"))
            val bSenAtPeriode = PeriodeDto(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-31"))
            val bGammel = lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = bFnr, periode = bGammelPeriode,
                utkastStartet = Instant.parse("2026-01-01T08:00:00Z"), innsendtDato = Instant.parse("2026-01-02T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL, fnr = bFnr, periode = bParPeriode,
                utkastStartet = Instant.parse("2026-01-03T08:00:00Z"), innsendtDato = Instant.parse("2026-01-04T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = bFnr, periode = bParPeriode,
                utkastStartet = Instant.parse("2026-01-05T08:00:00Z"), innsendtDato = Instant.parse("2026-01-06T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = bFnr, periode = bSenAgPeriode, erstatterSkjemaId = bGammel.id,
                utkastStartet = Instant.parse("2026-01-07T08:00:00Z"), innsendtDato = Instant.parse("2026-01-08T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL, fnr = bFnr, periode = bSenAtPeriode,
                utkastStartet = Instant.parse("2026-01-09T08:00:00Z"), innsendtDato = Instant.parse("2026-01-10T08:00:00Z")
            )

            val s = hentBruk().saksdekning
            s.antallSakerMedMatchendeSeparateDeler shouldBe 2
            s.parInitiertAvArbeidsgiver shouldBe 1 // sak A: den erstattede versjonen hører til paret
            s.parInitiertAvArbeidstaker shouldBe 1 // sak B: den erstattede versjonen hører til en annen utsendelse
            s.parUavhengigStartet shouldBe 0
        }

        @Test
        fun `initiativ - erstattet versjon fra en annen utsendelse forgifter ikke tidsstemplene`() {
            val fnr = "49000000001"
            val at1Periode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-28"))
            val ag1Periode = PeriodeDto(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-03-31"))
            val ag2Periode = PeriodeDto(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-12-31"))
            val at2Periode = PeriodeDto(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-31"))
            val feilPeriode = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"))

            // Utsendelse 2 sin ERSTATTEDE AG-versjon bærer de tidligste tidsstemplene, og perioden dekker
            // hele året – den overlapper dermed utsendelse 1 sin AT-del uten å høre til den utsendelsen
            val gammelAg = lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = feilPeriode,
                utkastStartet = Instant.parse("2026-01-01T08:00:00Z"), innsendtDato = Instant.parse("2026-01-02T08:00:00Z")
            )
            // Utsendelse 1 (innsendt først av de gjeldende): arbeidstaker sendte inn før arbeidsgiver startet
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at1Periode,
                utkastStartet = Instant.parse("2026-01-05T08:00:00Z"), innsendtDato = Instant.parse("2026-01-06T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = ag1Periode,
                utkastStartet = Instant.parse("2026-01-07T08:00:00Z"), innsendtDato = Instant.parse("2026-01-08T08:00:00Z")
            )
            // Utsendelse 2, gjeldende versjoner
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, periode = ag2Periode, erstatterSkjemaId = gammelAg.id,
                utkastStartet = Instant.parse("2026-01-03T08:00:00Z"), innsendtDato = Instant.parse("2026-01-09T08:00:00Z")
            )
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL, fnr = fnr, periode = at2Periode,
                utkastStartet = Instant.parse("2026-01-10T08:00:00Z"), innsendtDato = Instant.parse("2026-01-11T08:00:00Z")
            )

            val s = hentBruk().saksdekning
            s.antallSakerMedMatchendeSeparateDeler shouldBe 1
            // Den erstattede versjonen følger sin etterfølger til utsendelse 2, og trekker ikke
            // arbeidsgiversiden i utsendelse 1 bakover i tid
            s.parInitiertAvArbeidstaker shouldBe 1
            s.parInitiertAvArbeidsgiver shouldBe 0
            s.parUavhengigStartet shouldBe 0
        }

        @Test
        fun `flere versjoner telles selv om bare den erstattede raden er i vinduet`() {
            val iVinduet = Instant.parse("2026-03-15T10:00:00Z")
            val utenforVinduet = Instant.parse("2026-06-15T10:00:00Z")
            val gammel = lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "46000000001", periode = periodeA, innsendtDato = iVinduet)
            lagInnsendt(
                Skjemadel.ARBEIDSGIVERS_DEL,
                fnr = "46000000001",
                periode = periodeOverlapp,
                innsendtDato = utenforVinduet,
                erstatterSkjemaId = gammel.id
            )

            val body = hentBruk(fraOgMed = "2026-03-01", tilOgMed = "2026-03-31")
            body.saksdekning.antallSakerMedFlereVersjoner shouldBe 1
            with(body.saksdekning.arbeidsgiverDeler) {
                totalt shouldBe 0
                antallErstattedeVersjoner shouldBe 1
                totalt + antallErstattedeVersjoner shouldBe body.innsendtPerSkjemadel[Skjemadel.ARBEIDSGIVERS_DEL]
            }
        }

        @Test
        fun `erstattet komplett holdes utenfor antallKomplette og kan avstemmes`() {
            val gammel = lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "44000000001", periode = periodeA)
            lagInnsendt(
                Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                fnr = "44000000001",
                periode = periodeOverlapp,
                erstatterSkjemaId = gammel.id
            )
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "44000000002", periode = periodeA)

            val body = hentBruk()
            body.innsendtPerSkjemadel[Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL] shouldBe 3
            with(body.saksdekning) {
                antallKomplette shouldBe 2
                antallErstattedeKomplette shouldBe 1
                antallKomplette + antallErstattedeKomplette shouldBe
                    body.innsendtPerSkjemadel[Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL]
            }
        }

        @Test
        fun `komplette fordeles paa fullmaktstype`() {
            lagInnsendt(
                Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                fnr = "38000000001",
                representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                fnr = "38000000002",
                representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT
            )
            lagInnsendt(
                Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                fnr = "38000000003",
                representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT
            )

            val s = hentBruk().saksdekning
            s.antallKomplette shouldBe 3
            s.komplettPerFlyt[Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT] shouldBe 1
            s.komplettPerFlyt[Representasjonstype.RADGIVER_MED_FULLMAKT] shouldBe 2
            s.komplettPerFlyt[Representasjonstype.DEG_SELV] shouldBe 0
            s.komplettPerFlyt.values.sum() shouldBe s.antallKomplette
        }

        @Test
        fun `deler uten utsendingsperiode telles som kvalitetsmaal`() {
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "39000000001", periode = null)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "39000000002", periode = null)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "39000000003", periode = periodeA)

            val s = hentBruk().saksdekning
            s.antallDelerUtenPeriode shouldBe 2
            s.arbeidstakerDeler.venterIngenMotpart shouldBe 2
        }

        @Test
        fun `toppliste grupperer paa juridisk enhet paa tvers av underenheter og viser saksstatus`() {
            val juridiskEnhet = "910000010"
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "41000000001", orgnr = "910000011", juridiskEnhet = juridiskEnhet, saksstatus = Saksstatus.MOTTATT)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "41000000001", orgnr = "910000012", juridiskEnhet = juridiskEnhet, periode = periodeOverlapp, saksstatus = Saksstatus.AVSLUTTET)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "41000000002", orgnr = "910000012", juridiskEnhet = juridiskEnhet)
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "41000000003", orgnr = "910000020", juridiskEnhet = "910000020")

            val body = hentBruk()
            // Underenheter telles hver for seg på topp-nivå, mens topplisten grupperer på juridisk enhet
            body.antallUnikeVirksomheter shouldBe 3
            body.antallUnikeJuridiskeEnheter shouldBe 2

            val topp = body.topplisteVirksomheter
            topp shouldHaveSize 2
            with(topp[0]) {
                antallInnsendinger shouldBe 3 // begge underenhetene slått sammen
                antallArbeidstakerDel shouldBe 2
                antallArbeidsgiverDel shouldBe 1
                antallSakerMedBeggeDeler shouldBe 1
                antallMottatt shouldBe 1
                antallAvsluttet shouldBe 1
                antallUkjent shouldBe 1
            }
            topp[1].antallInnsendinger shouldBe 1
            topp[1].antallKomplett shouldBe 1
        }

        @Test
        fun `alle kontrollsummer gaar opp for en sammensatt populasjon`() {
            val iVinduet = Instant.parse("2026-03-15T10:00:00Z")
            val utenforVinduet = Instant.parse("2026-06-15T10:00:00Z")
            // Komplett skjema
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "42000000001", innsendtDato = iVinduet, representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT)
            // Matchende separat par, arbeidsgiver-delen sendt utenfor vinduet
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "42000000002", periode = periodeA, innsendtDato = iVinduet)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "42000000002", periode = periodeOverlapp, innsendtDato = utenforVinduet)
            // Versjonert arbeidsgiver-del
            val gammel = lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "42000000003", periode = periodeA, innsendtDato = iVinduet)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "42000000003", periode = periodeOverlapp, innsendtDato = iVinduet, erstatterSkjemaId = gammel.id)
            // Duplikat-tilfelle
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "42000000004", periode = periodeA, innsendtDato = iVinduet, saksnummer = "SAK-X1")
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "42000000004", periode = periodeOverlapp, innsendtDato = iVinduet, saksnummer = "SAK-X2")
            // Del uten periode
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "42000000005", periode = null, innsendtDato = iVinduet, saksstatus = Saksstatus.AVSLUTTET)
            // Separat del dekket av komplett skjema
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "42000000006", periode = periodeA, innsendtDato = iVinduet)
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "42000000006", periode = periodeOverlapp, innsendtDato = utenforVinduet)
            // Ventende del der motparten har påbegynt utkast
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "42000000007", periode = periodeA, innsendtDato = iVinduet)
            lagUtkast(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "42000000007")

            val body = hentBruk(fraOgMed = "2026-03-01", tilOgMed = "2026-03-31")
            val s = body.saksdekning

            Skjemadel.entries.forEach { del ->
                val status = when (del) {
                    Skjemadel.ARBEIDSTAKERS_DEL -> s.arbeidstakerDeler
                    Skjemadel.ARBEIDSGIVERS_DEL -> s.arbeidsgiverDeler
                    Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> null
                }
                status?.let {
                    it.totalt + it.antallErstattedeVersjoner shouldBe body.innsendtPerSkjemadel[del]
                    it.totalt shouldBe it.medMotpart + it.dekketAvKomplettSkjema + it.venterMotpartHarUtkast + it.venterIngenMotpart
                    it.medMotpart shouldBe it.medMotpartAktivSak + it.medMotpartAvsluttetSak
                    it.dekketAvKomplettSkjema shouldBe it.dekketAvKomplettSkjemaAktivSak + it.dekketAvKomplettSkjemaAvsluttetSak
                }
            }

            s.antallSakerMedBeggeDeler shouldBe
                s.antallSakerMedKomplett + s.antallSakerMedMatchendeSeparateDeler - s.antallSakerMedBaadeKomplettOgSeparate
            s.parInitiertAvArbeidsgiver + s.parInitiertAvArbeidstaker + s.parUavhengigStartet shouldBe
                s.antallSakerMedMatchendeSeparateDeler
            s.komplettPerFlyt.values.sum() shouldBe s.antallKomplette
            // Kun AG-delen uten periode (42000000005) venter med avsluttet sak
            s.antallVentendeMedAvsluttetSak shouldBe 1
            body.topplisteVirksomheter.sumOf { it.antallInnsendinger } shouldBe
                s.antallKomplette + s.arbeidstakerDeler.totalt + s.arbeidsgiverDeler.totalt

            // Konkrete tall for den sammensatte populasjonen
            s.antallKomplette shouldBe 1
            s.antallDelerUtenPeriode shouldBe 1
            s.antallMuligeDobbeltinnsendinger shouldBe 1
            s.muligeDobbeltinnsendinger shouldHaveSize 1
            s.arbeidsgiverDeler.antallErstattedeVersjoner shouldBe 1
            s.arbeidsgiverDeler.dekketAvKomplettSkjema shouldBe 1
            s.arbeidsgiverDeler.venterMotpartHarUtkast shouldBe 1
            s.arbeidstakerDeler.medMotpart shouldBe 1
        }

        @Test
        fun `saksdekning - ikke-overlappende periode gir ikke match`() {
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "40000000001", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "40000000001", periode = periodeSenere)

            val s = hentBruk().saksdekning
            s.antallSakerMedBeggeDeler shouldBe 0
            s.arbeidstakerDeler.venterIngenMotpart shouldBe 1
            s.arbeidsgiverDeler.venterIngenMotpart shouldBe 1
        }

        @Test
        fun `saksstatus-uttrekk returnerer synk-feltene uten personopplysninger`() {
            val fnr = "63000000001"
            val skjema = lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = fnr, saksstatus = Saksstatus.MOTTATT)
            val innsending = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            innsending.saksnummer = "MEL-123456"
            innsendingRepository.save(innsending)

            val json = adminClient.get().uri("/admin/saksstatus/uttrekk")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!
            json shouldNotContain fnr
            json shouldNotContain innsending.innsenderFnr
            json shouldNotContain "Test Testesen"

            val uttrekk = adminClient.get().uri("/admin/saksstatus/uttrekk")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .exchange()
                .expectStatus().isOk
                .expectBody<SaksstatusUttrekkDto>()
                .returnResult()
                .responseBody!!
            uttrekk.antall shouldBe 1
            with(uttrekk.rader.single()) {
                skjemaId shouldBe skjema.id
                saksnummer shouldBe "MEL-123456"
                saksstatus shouldBe Saksstatus.MOTTATT
                referanseId shouldBe innsending.referanseId
            }
        }

        @Test
        fun `saksstatusfordeling teller mottatt, avsluttet og ukjent`() {
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "60000000001", saksstatus = Saksstatus.MOTTATT)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "60000000002", saksstatus = Saksstatus.AVSLUTTET)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "60000000003")

            val fordeling = hentBruk().saksstatusFordeling
            fordeling.mottatt shouldBe 1
            fordeling.avsluttet shouldBe 1
            fordeling.ukjent shouldBe 1
        }

        @Test
        fun `venter-tall splittes paa aktiv og avsluttet sak, og ventende med avsluttet sak telles`() {
            // Venter uten motpart, aktiv sak (MOTTATT)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "61000000001", saksstatus = Saksstatus.MOTTATT)
            // Venter uten motpart, avsluttet sak (motpart kom via annen kanal)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "61000000002", saksstatus = Saksstatus.AVSLUTTET)
            // Venter uten motpart, ikke synket (regnes som aktiv)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "61000000003")
            // Venter med motpart-utkast, avsluttet sak
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "61000000004", saksstatus = Saksstatus.AVSLUTTET)
            lagUtkast(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "61000000004")

            val s = hentBruk().saksdekning
            with(s.arbeidsgiverDeler) {
                venterIngenMotpart shouldBe 3
                venterIngenMotpartAktivSak shouldBe 2
                venterIngenMotpartAvsluttetSak shouldBe 1
                venterMotpartHarUtkast shouldBe 1
                venterMotpartHarUtkastAktivSak shouldBe 0
                venterMotpartHarUtkastAvsluttetSak shouldBe 1
            }
            s.antallVentendeMedAvsluttetSak shouldBe 2
        }

        @Test
        fun `motpart-cta telles for innsendte i perioden`() {
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "62000000001", opprettetVia = OpprettetVia.MOTPART_CTA)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "62000000002")
            lagInnsendt(
                Skjemadel.ARBEIDSTAKERS_DEL,
                fnr = "62000000004",
                opprettetVia = OpprettetVia.MOTPART_CTA,
                innsendtDato = Instant.parse("2020-01-15T12:00:00Z")
            )
            skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = "62000000003",
                    status = SkjemaStatus.UTKAST,
                    metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(skjemadel = Skjemadel.ARBEIDSTAKERS_DEL),
                    opprettetVia = OpprettetVia.MOTPART_CTA
                )
            )

            val alt = hentBruk().motpartCta
            alt.antallInnsendtViaCta shouldBe 2
            alt.antallUtkastViaCta shouldBe 1

            // Innsendt følger periodefilteret; utkast er nåtilstand og påvirkes ikke
            val iPerioden = hentBruk(fraOgMed = "2026-01-01").motpartCta
            iPerioden.antallInnsendtViaCta shouldBe 1
            iPerioden.antallUtkastViaCta shouldBe 1
        }

        @Test
        fun `skal returnere nuller naar ingen data`() {
            val body = hentBruk()

            body.utkast.antall shouldBe 0
            body.totaltInnsendt shouldBe 0
            body.innsendtPerSkjemadel[Skjemadel.ARBEIDSTAKERS_DEL] shouldBe 0
            body.saksdekning.antallSakerMedBeggeDeler shouldBe 0
            body.saksdekning.antallMuligeDobbeltinnsendinger shouldBe 0
            body.utkast.eldsteOpprettetDato shouldBe null
        }

        @Test
        fun `skal filtrere innsendt-statistikk paa innsendingsperiode`() {
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "60000000001", innsendtDato = Instant.parse("2026-01-15T10:00:00Z"))
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "60000000002", innsendtDato = Instant.parse("2026-03-15T10:00:00Z"))
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "60000000003", innsendtDato = Instant.parse("2026-06-15T10:00:00Z"))

            hentBruk().totaltInnsendt shouldBe 3 // ingen grense = alt
            hentBruk(fraOgMed = "2026-02-01", tilOgMed = "2026-04-30").totaltInnsendt shouldBe 1 // kun mars
            hentBruk(fraOgMed = "2026-02-01").totaltInnsendt shouldBe 2 // mars + juni
            hentBruk(tilOgMed = "2026-02-01").totaltInnsendt shouldBe 1 // kun januar
        }

        @Test
        fun `toppliste viser anonyme detaljer per virksomhet sortert synkende`() {
            // Virksomhet 1: 3 innsendinger, 2 ulike innsendere, alle arbeidstaker-deler
            repeat(3) { i ->
                lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "7000000000$i", orgnr = "910000001", innsenderFnr = if (i == 0) "11111111111" else "22222222222")
            }
            repeat(2) { i -> lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "7100000000$i", orgnr = "910000002") }
            lagInnsendt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, fnr = "72000000001", orgnr = "910000003")

            val topp = hentBruk().topplisteVirksomheter
            topp.map { it.antallInnsendinger } shouldBe listOf(3L, 2L, 1L)
            topp[0].antallUnikeInnsendere shouldBe 2
            topp[0].antallArbeidstakerDel shouldBe 3
            topp[1].antallArbeidsgiverDel shouldBe 2
            topp[2].antallKomplett shouldBe 1
            topp[2].antallSakerMedBeggeDeler shouldBe 1
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            adminClient.get().uri("/admin/statistikk/bruk")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .exchange()
                .expectStatus().isForbidden
        }
    }

    @Nested
    @DisplayName("GET /admin/statistikk/bruk/virksomheter/{rang}/saksnumre")
    inner class VirksomhetSaksnumre {

        private val periodeA = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30"))

        private fun lagInnsendt(
            fnr: String,
            juridiskEnhet: String,
            orgnr: String = juridiskEnhet,
            saksnummer: String? = null,
            innsendtDato: Instant = Instant.now()
        ): Skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = fnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                    utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier().copy(utsendelsePeriode = periodeA)
                ),
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    juridiskEnhetOrgnr = juridiskEnhet
                )
            )
        ).also { skjema ->
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, opprettetDato = innsendtDato, saksnummer = saksnummer)
            )
        }

        private fun hentSaksnumre(rang: Int, fraOgMed: String? = null, tilOgMed: String? = null) =
            adminClient.get().uri { b ->
                b.path("/admin/statistikk/bruk/virksomheter/$rang/saksnumre")
                fraOgMed?.let { b.queryParam("fraOgMed", it) }
                tilOgMed?.let { b.queryParam("tilOgMed", it) }
                b.build()
            }
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        private fun hentSaksnumreDto(rang: Int, fraOgMed: String? = null, tilOgMed: String? = null): VirksomhetSaksnumreDto =
            hentSaksnumre(rang, fraOgMed, tilOgMed)
                .expectStatus().isOk
                .expectBody<VirksomhetSaksnumreDto>()
                .returnResult().responseBody.shouldNotBeNull()

        private fun hentToppliste(fraOgMed: String? = null, tilOgMed: String? = null): List<VirksomhetStatistikkDto> =
            adminClient.get().uri { b ->
                b.path("/admin/statistikk/bruk")
                fraOgMed?.let { b.queryParam("fraOgMed", it) }
                tilOgMed?.let { b.queryParam("tilOgMed", it) }
                b.build()
            }
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<BrukStatistikkDto>()
                .returnResult().responseBody.shouldNotBeNull()
                .topplisteVirksomheter

        @Test
        fun `skal returnere saksnumrene for virksomheten paa oppgitt rang, uten personopplysninger`() {
            val utenSaksnummer = lagInnsendt(fnr = "80000000001", juridiskEnhet = "920000010", orgnr = "920000011", saksnummer = null)
            lagInnsendt(fnr = "80000000002", juridiskEnhet = "920000010", orgnr = "920000012", saksnummer = "SAK-A2")
            lagInnsendt(fnr = "80000000003", juridiskEnhet = "920000010", saksnummer = "SAK-A3")
            lagInnsendt(fnr = "80000000004", juridiskEnhet = "920000020", saksnummer = "SAK-B1")

            val topp1 = hentSaksnumre(1)
                .expectStatus().isOk
                .expectBody<VirksomhetSaksnumreDto>()
                .returnResult().responseBody.shouldNotBeNull()
            topp1.rang shouldBe 1
            topp1.antallInnsendinger shouldBe 3
            topp1.saksnumre shouldContainExactlyInAnyOrder listOf("SAK-A2", "SAK-A3", utenSaksnummer.id.toString())

            val topp2 = hentSaksnumre(2)
                .expectStatus().isOk
                .expectBody<VirksomhetSaksnumreDto>()
                .returnResult().responseBody.shouldNotBeNull()
            topp2.antallInnsendinger shouldBe 1
            topp2.saksnumre shouldBe listOf("SAK-B1")

            val json = hentSaksnumre(1).expectStatus().isOk
                .expectBody(String::class.java).returnResult().responseBody!!
            json shouldNotContain "80000000001"
            json shouldNotContain "920000010"
            json shouldNotContain "Test Testesen"
        }

        @Test
        fun `antallInnsendinger skal stemme med samme rad i topplisten`() {
            lagInnsendt(fnr = "81000000001", juridiskEnhet = "921000010")
            lagInnsendt(fnr = "81000000002", juridiskEnhet = "921000010")
            lagInnsendt(fnr = "81000000003", juridiskEnhet = "921000020")

            hentToppliste().forEachIndexed { indeks, virksomhet ->
                val saksnumre = hentSaksnumreDto(indeks + 1)
                saksnumre.antallInnsendinger shouldBe virksomhet.antallInnsendinger
                saksnumre.saksnumre shouldHaveSize virksomhet.antallInnsendinger.toInt()
            }
        }

        @Test
        fun `skal bruke samme periodevindu som topplisten`() {
            val iVinduet = Instant.parse("2026-03-15T10:00:00Z")
            val utenforVinduet = Instant.parse("2026-06-15T10:00:00Z")
            lagInnsendt(fnr = "83000000001", juridiskEnhet = "923000010", saksnummer = "SAK-V1", innsendtDato = iVinduet)
            lagInnsendt(fnr = "83000000002", juridiskEnhet = "923000010", saksnummer = "SAK-V2", innsendtDato = iVinduet)
            lagInnsendt(fnr = "83000000003", juridiskEnhet = "923000010", saksnummer = "SAK-UTENFOR", innsendtDato = utenforVinduet)
            lagInnsendt(fnr = "83000000004", juridiskEnhet = "923000020", saksnummer = "SAK-W1", innsendtDato = iVinduet)

            val toppRad1 = hentToppliste(fraOgMed = "2026-03-01", tilOgMed = "2026-03-31").first()
            val saksnumre = hentSaksnumreDto(1, fraOgMed = "2026-03-01", tilOgMed = "2026-03-31")

            saksnumre.antallInnsendinger shouldBe toppRad1.antallInnsendinger
            saksnumre.antallInnsendinger shouldBe 2
            saksnumre.saksnumre shouldContainExactlyInAnyOrder listOf("SAK-V1", "SAK-V2")

            // Uten periodefilter er den tredje innsendingen med igjen
            hentSaksnumreDto(1).saksnumre shouldContainExactlyInAnyOrder listOf("SAK-V1", "SAK-V2", "SAK-UTENFOR")
        }

        @Test
        fun `skal skille virksomheter med likt antall innsendinger paa juridisk enhet`() {
            lagInnsendt(fnr = "84000000001", juridiskEnhet = "924000020", saksnummer = "SAK-B1")
            lagInnsendt(fnr = "84000000002", juridiskEnhet = "924000020", saksnummer = "SAK-B2")
            lagInnsendt(fnr = "84000000003", juridiskEnhet = "924000010", saksnummer = "SAK-A1")
            lagInnsendt(fnr = "84000000004", juridiskEnhet = "924000010", saksnummer = "SAK-A2")

            hentToppliste().map { it.antallInnsendinger } shouldBe listOf(2L, 2L)
            // Ved likt antall sorteres laveste juridiske enhet først
            hentSaksnumreDto(1).saksnumre shouldContainExactlyInAnyOrder listOf("SAK-A1", "SAK-A2")
            hentSaksnumreDto(2).saksnumre shouldContainExactlyInAnyOrder listOf("SAK-B1", "SAK-B2")
        }

        @Test
        fun `skal returnere 404 naar rang er utenfor topplisten`() {
            lagInnsendt(fnr = "82000000001", juridiskEnhet = "922000010")

            hentSaksnumre(2).expectStatus().isNotFound
            hentSaksnumre(0).expectStatus().isNotFound
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            adminClient.get().uri("/admin/statistikk/bruk/virksomheter/1/saksnumre")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .exchange()
                .expectStatus().isForbidden
        }
    }

    @Nested
    @DisplayName("POST /admin/varsler/resend")
    inner class ResendVarsler {

        private val periodeA = PeriodeDto(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30"))
        private val periodeOverlapp = PeriodeDto(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-08-31"))
        private val periodeIngenOverlapp = PeriodeDto(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-31"))
        private val foerCutoff = Instant.parse("2026-06-01T00:00:00Z")
        private val etterCutoff = Instant.parse("2026-07-03T14:00:00Z")

        /** Lager en innsendt (SENDT) utsendt-arbeidstaker-del med kontroll på del, flyt, periode og innsendingsdato. */
        private fun lagInnsendtDel(
            skjemadel: Skjemadel,
            representasjonstype: Representasjonstype,
            fnr: String = "10000000001",
            periode: PeriodeDto = periodeA,
            innsendtDato: Instant = foerCutoff,
            juridiskEnhet: String = korrektSyntetiskOrgnr,
            saksnummer: String? = null,
            saksstatus: Saksstatus? = null,
            erstatterSkjemaId: UUID? = null
        ): Skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = fnr,
                status = SkjemaStatus.SENDT,
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                    utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier().copy(utsendelsePeriode = periode)
                ),
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = representasjonstype,
                    skjemadel = skjemadel,
                    juridiskEnhetOrgnr = juridiskEnhet,
                    erstatterSkjemaId = erstatterSkjemaId
                )
            )
        ).also { skjema ->
            innsendingRepository.save(
                innsendingMedDefaultVerdier(skjema = skjema, opprettetDato = innsendtDato, saksnummer = saksnummer, saksstatus = saksstatus)
            )
        }

        /** Handlingspliktig AG-del (arbeidsgiver uten fullmakt) – standard resend-kandidat. */
        private fun lagAgDel(
            fnr: String = "10000000001",
            representasjonstype: Representasjonstype = Representasjonstype.ARBEIDSGIVER,
            periode: PeriodeDto = periodeA,
            innsendtDato: Instant = foerCutoff,
            juridiskEnhet: String = korrektSyntetiskOrgnr,
            saksnummer: String? = null,
            saksstatus: Saksstatus? = null,
            erstatterSkjemaId: UUID? = null
        ): Skjema = lagInnsendtDel(Skjemadel.ARBEIDSGIVERS_DEL, representasjonstype, fnr, periode, innsendtDato, juridiskEnhet, saksnummer, saksstatus, erstatterSkjemaId)

        /** Innsendt arbeidstaker-del for samme person/enhet (markerer at saken ikke lenger venter på AT-del). */
        private fun lagAtDel(fnr: String, periode: PeriodeDto = periodeA, juridiskEnhet: String = korrektSyntetiskOrgnr): Skjema =
            lagInnsendtDel(Skjemadel.ARBEIDSTAKERS_DEL, Representasjonstype.DEG_SELV, fnr, periode, foerCutoff, juridiskEnhet)

        private fun resend(dryRun: Boolean = false): ResendVarslerResultatDto =
            adminClient.post().uri("/admin/varsler/resend?dryRun=$dryRun")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<ResendVarslerResultatDto>()
                .returnResult().responseBody.shouldNotBeNull()

        @Test
        fun `dry-run teller kandidatene uten aa sende noe, og er default`() {
            lagAgDel(fnr = "10000000030", saksnummer = "SAK-DRY")
            // Utkast-guarden skal telle med i dry-run også: denne ville ikke fått varsel
            lagAgDel(fnr = "10000000031", saksnummer = "SAK-UTKAST")
            skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = "10000000031",
                    status = SkjemaStatus.UTKAST,
                    metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                        representasjonstype = Representasjonstype.DEG_SELV,
                        skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                    )
                )
            )

            val eksplisitt = resend(dryRun = true)
            eksplisitt.dryRun shouldBe true
            eksplisitt.antallSendt shouldBe 1
            eksplisitt.saksnumre shouldBe listOf("SAK-DRY")

            val defaultKall = adminClient.post().uri("/admin/varsler/resend")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<ResendVarslerResultatDto>()
                .returnResult().responseBody.shouldNotBeNull()
            defaultKall.dryRun shouldBe true

            verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal sende varsel med korrekt lenke og ignorer-tekst for handlingspliktig AG-del foer cutoff som venter paa AT-del`() {
            val skjema = lagAgDel(saksnummer = "SAK-001")

            val body = resend()

            body.antallSendt shouldBe 1
            body.saksnumre shouldBe listOf("SAK-001")
            verify(exactly = 1) {
                brukervarselProducer.sendBrukervarsel(
                    match<BrukervarselMelding> { melding ->
                        melding.ident == skjema.fnr &&
                            melding.sms &&
                            melding.tekster.first { it.språk == Språk.NORSK_BOKMAL }.tekst.contains("kan du se bort fra denne meldingen")
                    }
                )
            }
        }

        @Test
        fun `skal returnere saksnumrene som faktisk fikk varsel, og skjema-id naar saksnummer mangler`() {
            lagAgDel(fnr = "10000000020", saksnummer = "SAK-100")
            val utenSaksnummer = lagAgDel(fnr = "10000000021", saksnummer = null)
            // Ikke-kandidat (etter cutoff) – skal ikke dukke opp i listen
            lagAgDel(fnr = "10000000022", innsendtDato = etterCutoff, saksnummer = "SAK-999")

            val body = resend()

            body.antallSendt shouldBe 2
            body.saksnumre shouldContainExactlyInAnyOrder listOf("SAK-100", utenSaksnummer.id.toString())
        }

        @Test
        fun `skal sende for radgiver uten fullmakt`() {
            lagAgDel(representasjonstype = Representasjonstype.RADGIVER)

            resend().antallSendt shouldBe 1
        }

        @Test
        fun `skal ikke sende naar arbeidstaker allerede har sendt sin del med overlappende periode`() {
            lagAgDel(fnr = "10000000005", periode = periodeA)
            lagAtDel(fnr = "10000000005", periode = periodeOverlapp)

            resend().antallSendt shouldBe 0
            verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal fortsatt sende naar arbeidstakers del er for en ikke-overlappende periode`() {
            lagAgDel(fnr = "10000000006", periode = periodeA)
            lagAtDel(fnr = "10000000006", periode = periodeIngenOverlapp)

            resend().antallSendt shouldBe 1
        }

        @Test
        fun `skal ikke sende for AG-del innsendt etter cutoff`() {
            lagAgDel(innsendtDato = etterCutoff)

            resend().antallSendt shouldBe 0
            verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal ikke sende naar saken er avsluttet i melosys-api`() {
            lagAgDel(saksnummer = "SAK-200", saksstatus = Saksstatus.AVSLUTTET)

            resend().antallSendt shouldBe 0
            verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal fortsatt sende naar saken er mottatt i melosys-api`() {
            lagAgDel(saksnummer = "SAK-201", saksstatus = Saksstatus.MOTTATT)

            resend().antallSendt shouldBe 1
        }

        @Test
        fun `skal ikke sende for med-fullmakt AG-del`() {
            lagAgDel(representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT)

            resend().antallSendt shouldBe 0
            verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal ikke sende naar arbeidstaker har paabegynt utkast`() {
            val skjema = lagAgDel(fnr = "10000000007")
            // Arbeidstaker har påbegynt sin del for samme juridiske enhet (utkast, ikke sendt)
            skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = skjema.fnr,
                    status = SkjemaStatus.UTKAST,
                    metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                        representasjonstype = Representasjonstype.DEG_SELV,
                        skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                    )
                )
            )

            resend().antallSendt shouldBe 0
            verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal ikke sende dobbelt naar AG-del er erstattet av en nyere versjon`() {
            // Arbeidsgiver sendte AG-delen, korrigerte og sendte inn på nytt før cutoff. Begge versjonene
            // er SENDT, handlingspliktige og venter på AT-del, men den nye erstatter den gamle – kun ett varsel.
            val gammel = lagAgDel(fnr = "10000000030", saksnummer = "SAK-ERST")
            lagAgDel(fnr = "10000000030", saksnummer = "SAK-ERST", erstatterSkjemaId = gammel.id)

            val body = resend()

            body.antallSendt shouldBe 1
            body.saksnumre shouldBe listOf("SAK-ERST")
            verify(exactly = 1) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal telle bare de faktiske kandidatene blant en blanding`() {            lagAgDel(fnr = "10000000010", saksnummer = "SAK-A")            // kandidat
            lagAgDel(fnr = "10000000011", saksnummer = "SAK-B")            // kandidat
            lagAgDel(fnr = "10000000012", innsendtDato = etterCutoff)       // etter cutoff – nei
            lagAgDel(fnr = "10000000013", representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT) // med fullmakt – nei
            lagAgDel(fnr = "10000000014").also { lagAtDel(fnr = "10000000014") }   // AT-del sendt – nei

            val body = resend()

            body.antallSendt shouldBe 2
            body.saksnumre shouldContainExactlyInAnyOrder listOf("SAK-A", "SAK-B")
            verify(exactly = 2) { brukervarselProducer.sendBrukervarsel(any()) }
        }

        @Test
        fun `skal returnere 403 naar azp ikke matcher tillatt klient`() {
            adminClient.post().uri("/admin/varsler/resend")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .exchange()
                .expectStatus().isForbidden
        }
    }

}
