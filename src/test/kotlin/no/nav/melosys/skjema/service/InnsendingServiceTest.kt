package no.nav.melosys.skjema.service

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.kafka.SkjemaMottattProducer
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
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
        every { skjemaMottattProducer.blokkerendeSendSkjemaMottatt(any()) } answers { Result.success(mockk()) }
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
                referanseId = "MEL-TEST01",
                skjemaDefinisjonVersjon = "1",
                innsendtSprak = Språk.NORSK_BOKMAL
            )

            innsending.skjema.id shouldBe skjema.id
            innsending.status shouldBe InnsendingStatus.MOTTATT
            innsending.antallForsok shouldBe 0
            innsending.referanseId shouldBe "MEL-TEST01"
            innsending.skjemaDefinisjonVersjon shouldBe "1"
            innsending.innsendtSprak shouldBe Språk.NORSK_BOKMAL

            val lagret = innsendingRepository.findBySkjemaId(skjema.id!!)
            lagret shouldNotBe null
            lagret!!.status shouldBe InnsendingStatus.MOTTATT
            lagret.referanseId shouldBe "MEL-TEST01"
            lagret.skjemaDefinisjonVersjon shouldBe "1"
            lagret.innsendtSprak shouldBe Språk.NORSK_BOKMAL
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
                    referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
                )
            )

            innsendingService.prosesserInnsending(skjema.id!!)

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id)!!
            oppdatert.status shouldBe InnsendingStatus.FERDIG
            oppdatert.antallForsok shouldBe 1
            oppdatert.sisteForsoekTidspunkt shouldNotBe null

            verify {
                skjemaMottattProducer.blokkerendeSendSkjemaMottatt(SkjemaMottattMelding(skjema.id))
            }
        }

        @Test
        @DisplayName("Skal inkrementere antallForsok ved gjentatte oppdateringer")
        fun `skal inkrementere antallForsok ved gjentatte oppdateringer`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
                )
            )

            every {
                skjemaMottattProducer.blokkerendeSendSkjemaMottatt(any())
            } throws SendSkjemaMottattMeldingFeilet("", Exception())

            innsendingService.prosesserInnsending(skjema.id!!)
            innsendingRepository.findBySkjemaId(skjema.id)!!.antallForsok shouldBe 1

            innsendingService.prosesserInnsending(skjema.id)
            innsendingRepository.findBySkjemaId(skjema.id)!!.antallForsok shouldBe 2
        }

        @Test
        @DisplayName("Skal trunkere lange feilmeldinger til 2000 tegn")
        fun `skal trunkere lange feilmeldinger til 2000 tegn`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
                )
            )
            val langFeilmelding = "x".repeat(3000)

            every {
                skjemaMottattProducer.blokkerendeSendSkjemaMottatt(any())
            } throws SendSkjemaMottattMeldingFeilet(langFeilmelding, Exception())

            innsendingService.prosesserInnsending(skjema.id!!)

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id)!!
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
                referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}",
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
}
