package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.skjemadefinisjon.Språk
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class InnsendingStatusServiceTest : ApiTestBase() {

    @Autowired
    private lateinit var innsendingStatusService: InnsendingStatusService

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
    }

    @Nested
    @DisplayName("opprettInnsending")
    inner class OpprettInnsendingTests {

        @Test
        @DisplayName("Skal opprette innsending med status MOTTATT")
        fun `skal opprette innsending med status MOTTATT`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))

            val innsending = innsendingStatusService.opprettInnsending(
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
        @DisplayName("Skal oppdatere status til FERDIG")
        fun `skal oppdatere status til FERDIG`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(innsendingMedDefaultVerdier(
                skjema = skjema,
                referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
            ))

            innsendingStatusService.oppdaterStatus(skjema.id!!, InnsendingStatus.FERDIG)

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.status shouldBe InnsendingStatus.FERDIG
            oppdatert.antallForsok shouldBe 1
            oppdatert.sisteForsoekTidspunkt shouldNotBe null
        }

        @Test
        @DisplayName("Skal oppdatere status med feilmelding")
        fun `skal oppdatere status med feilmelding`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(innsendingMedDefaultVerdier(
                skjema = skjema,
                referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
            ))

            innsendingStatusService.oppdaterStatus(
                skjema.id!!,
                InnsendingStatus.JOURNALFORING_FEILET,
                feilmelding = "Joark er nede"
            )

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.status shouldBe InnsendingStatus.JOURNALFORING_FEILET
            oppdatert.feilmelding shouldBe "Joark er nede"
        }

        @Test
        @DisplayName("Skal inkrementere antallForsok ved gjentatte oppdateringer")
        fun `skal inkrementere antallForsok ved gjentatte oppdateringer`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(innsendingMedDefaultVerdier(
                skjema = skjema,
                referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
            ))

            innsendingStatusService.oppdaterStatus(skjema.id!!, InnsendingStatus.JOURNALFORING_FEILET)
            innsendingRepository.findBySkjemaId(skjema.id!!)!!.antallForsok shouldBe 1

            innsendingStatusService.oppdaterStatus(skjema.id!!, InnsendingStatus.JOURNALFORING_FEILET)
            innsendingRepository.findBySkjemaId(skjema.id!!)!!.antallForsok shouldBe 2
        }

        @Test
        @DisplayName("Skal trunkere lange feilmeldinger til 2000 tegn")
        fun `skal trunkere lange feilmeldinger til 2000 tegn`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(innsendingMedDefaultVerdier(
                skjema = skjema,
                referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
            ))
            val langFeilmelding = "x".repeat(3000)

            innsendingStatusService.oppdaterStatus(
                skjema.id!!,
                InnsendingStatus.JOURNALFORING_FEILET,
                feilmelding = langFeilmelding
            )

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.feilmelding?.length shouldBe 2000
        }

        @Test
        @DisplayName("Skal kaste exception når innsending ikke finnes")
        fun `skal kaste exception når innsending ikke finnes`() {
            val skjemaId = UUID.randomUUID()

            val exception = shouldThrow<IllegalStateException> {
                innsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)
            }

            exception.message shouldBe "Innsending for skjema $skjemaId ikke funnet"
        }
    }

    @Nested
    @DisplayName("startProsessering")
    inner class StartProsesseringTests {

        @Test
        @DisplayName("Skal sette status til UNDER_BEHANDLING og oppdatere sisteForsoekTidspunkt")
        fun `skal sette status til UNDER_BEHANDLING og oppdatere sisteForsoekTidspunkt`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            innsendingRepository.save(innsendingMedDefaultVerdier(
                skjema = skjema,
                referanseId = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}"
            ))

            innsendingStatusService.startProsessering(skjema.id!!)

            val oppdatert = innsendingRepository.findBySkjemaId(skjema.id!!)!!
            oppdatert.status shouldBe InnsendingStatus.UNDER_BEHANDLING
            oppdatert.sisteForsoekTidspunkt shouldNotBe null
        }
    }

    @Nested
    @DisplayName("oppdaterSkjemaJournalpostId")
    inner class OppdaterSkjemaJournalpostIdTests {

        @Test
        @DisplayName("Skal sette journalpostId på skjema")
        fun `skal sette journalpostId på skjema`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))

            innsendingStatusService.oppdaterSkjemaJournalpostId(skjema.id!!, "67890")

            val oppdatert = skjemaRepository.findById(skjema.id!!).get()
            oppdatert.journalpostId shouldBe "67890"
        }
    }
}
