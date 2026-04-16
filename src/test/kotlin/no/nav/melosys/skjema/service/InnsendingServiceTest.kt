package no.nav.melosys.skjema.service

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.kafka.SkjemaMottattProducer
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class InnsendingServiceTest : ApiTestBase() {

    @Autowired
    private lateinit var innsendingService: InnsendingService

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @MockkBean
    private lateinit var skjemaMottattProducer: SkjemaMottattProducer

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
        clearAllMocks()
        every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(any()) } returns Result.success(Unit)
    }

    @Nested
    @DisplayName("opprettInnsending")
    inner class OpprettInnsendingTests {

        @Test
        @DisplayName("Skal opprette innsending med status MOTTATT")
        fun `skal opprette innsending med status MOTTATT`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))

            val innsending = innsendingService.opprettInnsending(
                skjema = skjema,
                referanseId = "TEST01",
                skjemaDefinisjonVersjon = "1",
                innsendtSprak = Språk.NORSK_BOKMAL,
                innsenderFnr = "12345678901"
            )

            innsending.skjema.id shouldBe skjema.id
            innsending.status shouldBe InnsendingStatus.MOTTATT
            innsending.antallForsok shouldBe 0
            innsending.referanseId shouldBe "TEST01"
            innsending.skjemaDefinisjonVersjon shouldBe "1"
            innsending.innsendtSprak shouldBe Språk.NORSK_BOKMAL
            innsending.innsenderFnr shouldBe "12345678901"

            val lagret = innsendingRepository.findBySkjemaId(skjema.id!!)
            lagret shouldNotBe null
            lagret!!.status shouldBe InnsendingStatus.MOTTATT
            lagret.referanseId shouldBe "TEST01"
            lagret.skjemaDefinisjonVersjon shouldBe "1"
            lagret.innsendtSprak shouldBe Språk.NORSK_BOKMAL
            lagret.innsenderFnr shouldBe "12345678901"
        }
    }

    @Nested
    @DisplayName("oppdaterStatus")
    inner class OppdaterStatusTests {

        @Test
        @DisplayName("prosseserInnsending skal sende kafkamelding og sette status til FERDIG ved suksess")
        fun `skal oppdatere status til FERDIG`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            innsendingService.prosesserInnsending(skjema.id!!)

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.status shouldBe InnsendingStatus.FERDIG
            oppdatert.antallForsok shouldBe 1
            oppdatert.sisteForsoekTidspunkt shouldNotBe null

            verify {
                skjemaMottattProducer.blokkerendeSendSkjemaMottatt(SkjemaMottattMelding(skjema.id!!))
            }
        }

        @Test
        @DisplayName("Skal inkrementere antallForsok ved gjentatte oppdateringer")
        fun `skal inkrementere antallForsok ved gjentatte oppdateringer`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            every {
                skjemaMottattProducer.blokkerendeSendSkjemaMottatt(any())
            } throws SendSkjemaMottattMeldingFeilet("", Exception())

            innsendingService.prosesserInnsending(skjema.id!!)
            innsendingRepository.findBySkjemaId(skjema.id!!)!!.antallForsok shouldBe 1

            innsendingService.prosesserInnsending(skjema.id!!)
            innsendingRepository.findBySkjemaId(skjema.id!!)!!.antallForsok shouldBe 2
        }

        @Test
        @DisplayName("Skal trunkere lange feilmeldinger til 2000 tegn")
        fun `skal trunkere lange feilmeldinger til 2000 tegn`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )
            val langFeilmelding = "x".repeat(3000)

            every {
                skjemaMottattProducer.blokkerendeSendSkjemaMottatt(any())
            } throws SendSkjemaMottattMeldingFeilet(langFeilmelding, Exception())

            innsendingService.prosesserInnsending(skjema.id!!)

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.feilmelding?.length shouldBe 2000
        }

        @Test
        @DisplayName("Skal kaste exception når innsending ikke finnes")
        fun `skal kaste exception når innsending ikke finnes`() {
            val skjemaId = UUID.randomUUID()

            val exception = shouldThrow<IllegalStateException> {
                innsendingService.prosesserInnsending(skjemaId)
            }

            exception.message shouldBe "Innsending for skjema $skjemaId ikke funnet"
        }
    }

    @Test
    @DisplayName("Skal oppdatere status til KAFKA_FEILET ved SendSkjemaMottattMeldingFeilet")
    fun `skal oppdatere status til KAFKA_FEILET ved SendSkjemaMottattMeldingFeilet`() {

        val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
        val innsending = innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = skjema,
                referanseId = UUID.randomUUID().toString().take(6).uppercase(),
                status = InnsendingStatus.MOTTATT
            )
        )

        every {
            skjemaMottattProducer.blokkerendeSendSkjemaMottatt(any())
        } throws SendSkjemaMottattMeldingFeilet("", Exception())

        innsendingService.prosesserInnsending(skjema.id!!)

        innsendingRepository.findByIdOrNull(innsending.id!!)
            .shouldNotBeNull().status shouldBe InnsendingStatus.KAFKA_FEILET
    }

    @Nested
    @DisplayName("samleRelaterteSkjemaIder (via prosesserInnsending)")
    inner class SamleRelaterteSkjemaIderTests {

        private val overlappendePeriode = PeriodeDto(
            fraDato = LocalDate.of(2024, 1, 1),
            tilDato = LocalDate.of(2024, 12, 31)
        )

        private val skjemaDataMedPeriode = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
            utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                utsendelseLand = LandKode.SE,
                utsendelsePeriode = overlappendePeriode
            )
        )

        private fun lagSkjemaMedPeriodeOgOrg(
            fnr: String = korrektSyntetiskFnr,
            juridiskEnhetOrgnr: String = korrektSyntetiskOrgnr,
            periode: PeriodeDto = overlappendePeriode,
            kobletSkjemaId: UUID? = null,
            erstatterSkjemaId: UUID? = null
        ) = skjemaMedDefaultVerdier(
            fnr = fnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                    utsendelseLand = LandKode.SE,
                    utsendelsePeriode = periode
                )
            ),
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                kobletSkjemaId = kobletSkjemaId,
                erstatterSkjemaId = erstatterSkjemaId
            )
        )

        @Test
        @DisplayName("Ingen tidligere soknader gir tom relaterteSkjemaIder")
        fun `ingen tidligere soknader gir tom relaterteSkjemaIder`() {
            val skjema = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjema.id!!)

            meldingSlot.captured.relaterteSkjemaIder.shouldBeEmpty()
        }

        @Test
        @DisplayName("Tidligere soknad med samme FNR, org og overlappende periode inkluderes")
        fun `tidligere soknad med samme fnr org og overlappende periode inkluderes`() {
            val tidligereSkjema = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            val skjema = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjema.id!!)

            meldingSlot.captured.relaterteSkjemaIder shouldBe listOf(tidligereSkjema.id!!)
        }

        @Test
        @DisplayName("Tidligere soknad med annen juridisk enhet inkluderes IKKE")
        fun `tidligere soknad med annen juridisk enhet inkluderes ikke`() {
            skjemaRepository.save(lagSkjemaMedPeriodeOgOrg(juridiskEnhetOrgnr = "999999999"))
            val skjema = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjema.id!!)

            meldingSlot.captured.relaterteSkjemaIder.shouldBeEmpty()
        }

        @Test
        @DisplayName("Tidligere soknad med ikke-overlappende periode inkluderes IKKE")
        fun `tidligere soknad med ikke-overlappende periode inkluderes ikke`() {
            val ikkeOverlappendePeriode = PeriodeDto(
                fraDato = LocalDate.of(2025, 6, 1),
                tilDato = LocalDate.of(2025, 12, 31)
            )
            skjemaRepository.save(lagSkjemaMedPeriodeOgOrg(periode = ikkeOverlappendePeriode))
            val skjema = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjema.id!!)

            meldingSlot.captured.relaterteSkjemaIder.shouldBeEmpty()
        }

        @Test
        @DisplayName("Flere soknader med samme FNR, org og overlappende periode inkluderes alle")
        fun `flere soknader med samme fnr org og overlappende periode inkluderes alle`() {
            val tidligereSkjema1 = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            val tidligereSkjema2 = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            val skjema = skjemaRepository.save(lagSkjemaMedPeriodeOgOrg())
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjema.id!!)

            meldingSlot.captured.relaterteSkjemaIder shouldContainExactlyInAnyOrder
                listOf(tidligereSkjema1.id!!, tidligereSkjema2.id!!)
        }

        @Test
        @DisplayName("KobletSkjemaId inkluderes selv om det har annen juridisk enhet")
        fun `kobletSkjemaId inkluderes selv om det har annen juridisk enhet`() {
            val kobletSkjema = skjemaRepository.save(
                lagSkjemaMedPeriodeOgOrg(juridiskEnhetOrgnr = "999999999")
            )
            val skjema = skjemaRepository.save(
                lagSkjemaMedPeriodeOgOrg(kobletSkjemaId = kobletSkjema.id)
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjema.id!!)

            meldingSlot.captured.relaterteSkjemaIder shouldBe listOf(kobletSkjema.id!!)
        }

        @Test
        @DisplayName("Soknad uten utsendelsesperiode gir tom relaterteSkjemaIder")
        fun `soknad uten utsendelsesperiode gir tom relaterteSkjemaIder`() {
            val skjemaUtenPeriode = skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = korrektSyntetiskFnr,
                    orgnr = korrektSyntetiskOrgnr,
                    status = SkjemaStatus.SENDT,
                    data = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                        utsendingsperiodeOgLand = null
                    ),
                    metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                        representasjonstype = Representasjonstype.DEG_SELV,
                        skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                    )
                )
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjemaUtenPeriode,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjemaUtenPeriode.id!!)

            meldingSlot.captured.relaterteSkjemaIder.shouldBeEmpty()
        }

        @Test
        @DisplayName("Skjema uten data gir tom relaterteSkjemaIder")
        fun `skjema uten data gir tom relaterteSkjemaIder`() {
            val skjemaUtenData = skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = korrektSyntetiskFnr,
                    orgnr = korrektSyntetiskOrgnr,
                    status = SkjemaStatus.SENDT,
                    data = null,
                    metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                        representasjonstype = Representasjonstype.DEG_SELV,
                        skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                    )
                )
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjemaUtenData,
                    referanseId = UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            val meldingSlot = slot<SkjemaMottattMelding>()
            every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(capture(meldingSlot)) } returns Result.success(Unit)

            innsendingService.prosesserInnsending(skjemaUtenData.id!!)

            meldingSlot.captured.relaterteSkjemaIder.shouldBeEmpty()
        }
    }
}
