package no.nav.melosys.skjema.controller.admin

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.integrasjon.storage.VedleggStorageClient
import no.nav.melosys.skjema.m2mTokenWithoutAccess
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.InnsendingService
import no.nav.melosys.skjema.sikkerhet.AdminApiKeyInterceptor.Companion.API_KEY_HEADER
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
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
    private lateinit var vedleggStorageClient: VedleggStorageClient

    /** WebTestClient som sender gyldig admin-API-nøkkel på alle kall (jf. application-test.yml). */
    private val adminClient by lazy {
        webTestClient.mutate().defaultHeader(API_KEY_HEADER, TEST_ADMIN_APIKEY).build()
    }

    @BeforeEach
    fun setUp() {
        // Native delete (cascade) – rydder også gamle SLETTET-rader som seedes via SQL i denne testen.
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
            periode: PeriodeDto = periodeA,
            representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
            sprak: Språk = Språk.NORSK_BOKMAL,
            erstatterSkjemaId: UUID? = null,
            innsendtDato: Instant = Instant.now(),
            innsenderFnr: String = "12345678901"
        ): Skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = fnr,
                orgnr = orgnr,
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
                innsendingMedDefaultVerdier(skjema = skjema, innsendtSprak = sprak, opprettetDato = innsendtDato, innsenderFnr = innsenderFnr)
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
        fun `saksdekning - mulige dobbeltinnsendinger, men ikke versjon-erstatninger`() {
            // Ekte dobbeltinnsending: samme person sender arbeidsgivers del to ganger, overlappende periode
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "30000000001", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSGIVERS_DEL, fnr = "30000000001", periode = periodeOverlapp)

            // Versjon-erstatning: ny arbeidstaker-del erstatter en eldre (skal IKKE telles som duplikat)
            val gammel = lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "30000000002", periode = periodeA)
            lagInnsendt(Skjemadel.ARBEIDSTAKERS_DEL, fnr = "30000000002", periode = periodeOverlapp, erstatterSkjemaId = gammel.id)

            hentBruk().saksdekning.antallMuligeDobbeltinnsendinger shouldBe 2
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
    @DisplayName("POST /admin/utkast/rydd-slettede")
    inner class RyddSletteUtkast {

        /** Seeder en SLETTET-skjemarad (med valgfritt vedlegg) direkte via SQL, jf. gammel soft-delete. */
        private fun seedSletteSkjema(storageReferanse: String? = null): UUID {
            val skjemaId = UUID.randomUUID()
            jdbcTemplate.update(
                """
                INSERT INTO skjema (id, status, type, fnr, orgnr, metadata, opprettet_av, endret_av)
                VALUES (?, 'SLETTET', 'UTSENDT_ARBEIDSTAKER', '01816023404', '123456789',
                        '{"representasjonstype":"DEG_SELV"}'::jsonb, '01816023404', '01816023404')
                """.trimIndent(),
                skjemaId
            )
            if (storageReferanse != null) {
                jdbcTemplate.update(
                    """
                    INSERT INTO vedlegg (id, skjema_id, filnavn, original_filnavn, filtype, filstorrelse, storage_referanse, opprettet_av)
                    VALUES (?, ?, 'fil.pdf', 'fil.pdf', 'PDF', 123, ?, '01816023404')
                    """.trimIndent(),
                    UUID.randomUUID(), skjemaId, storageReferanse
                )
            }
            return skjemaId
        }

        private fun antallSkjemaIDb(): Int =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skjema", Int::class.java)!!

        private fun antallVedleggIDb(): Int =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vedlegg", Int::class.java)!!

        @Test
        fun `skal hard-slette SLETTET-skjema, cascade vedlegg-rader og slette blobs`() {
            seedSletteSkjema(storageReferanse = "skjemaer/a/vedlegg/b/fil.pdf")
            seedSletteSkjema(storageReferanse = "skjemaer/c/vedlegg/d/fil.pdf")
            seedSletteSkjema(storageReferanse = null)
            // Et innsendt skjema som IKKE skal røres
            skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT, data = arbeidstakersSkjemaDataDtoMedDefaultVerdier())
            )

            val body = adminClient.post().uri("/admin/utkast/rydd-slettede")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<RyddUtkastResultatDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.antallSkjema shouldBe 3
            body.antallVedleggSlettet shouldBe 2
            body.antallVedleggFeilet shouldBe 0

            // Kun det innsendte skjemaet er igjen, alle vedlegg-rader er cascade-slettet
            antallSkjemaIDb() shouldBe 1
            antallVedleggIDb() shouldBe 0
            verify(exactly = 1) { vedleggStorageClient.slett("skjemaer/a/vedlegg/b/fil.pdf") }
            verify(exactly = 1) { vedleggStorageClient.slett("skjemaer/c/vedlegg/d/fil.pdf") }
        }

        @Test
        fun `skal telle feilede blob-slettinger men likevel slette radene`() {
            seedSletteSkjema(storageReferanse = "skjemaer/x/vedlegg/y/fil.pdf")
            every { vedleggStorageClient.slett("skjemaer/x/vedlegg/y/fil.pdf") } throws RuntimeException("bucket nede")

            val body = adminClient.post().uri("/admin/utkast/rydd-slettede")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<RyddUtkastResultatDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.antallSkjema shouldBe 1
            body.antallVedleggSlettet shouldBe 0
            body.antallVedleggFeilet shouldBe 1
            antallSkjemaIDb() shouldBe 0
        }

        @Test
        fun `skal returnere nuller når ingen SLETTET-utkast finnes`() {
            val body = adminClient.post().uri("/admin/utkast/rydd-slettede")
                .header("Authorization", "Bearer ${mockOAuth2Server.adminTokenMedTilgang()}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody<RyddUtkastResultatDto>()
                .returnResult().responseBody.shouldNotBeNull()

            body.antallSkjema shouldBe 0
            body.antallVedleggSlettet shouldBe 0
            body.antallVedleggFeilet shouldBe 0
        }

        @Test
        fun `skal returnere 403 når azp ikke matcher tillatt klient`() {
            adminClient.post().uri("/admin/utkast/rydd-slettede")
                .header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutAccess()}")
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `JPA skal kunne laste legacy SLETTET-rad uten å kaste i opprydding-vinduet`() {
            // Regresjon: SLETTET er fjernet som aktiv status, men beholdt i enumet slik at eksisterende
            // rader kan mappes (EnumType.STRING) før admin-oppryddingen er kjørt i prod. Uten dette ville
            // findById på en gammel soft-deletet rad kaste og gi 500 i klient-kodeløp.
            val skjemaId = skjemaRepository.save(
                skjemaMedDefaultVerdier(status = SkjemaStatus.SLETTET)
            ).id.shouldNotBeNull()

            val skjema = skjemaRepository.findById(skjemaId)

            skjema.isPresent shouldBe true
            skjema.get().status shouldBe SkjemaStatus.SLETTET
        }
    }
}
