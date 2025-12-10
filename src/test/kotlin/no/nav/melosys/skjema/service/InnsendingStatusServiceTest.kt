package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import java.util.*

class InnsendingStatusServiceTest : FunSpec({

    val mockInnsendingRepository = mockk<InnsendingRepository>()
    val mockSkjemaRepository = mockk<SkjemaRepository>()

    afterTest {
        clearMocks(mockInnsendingRepository, mockSkjemaRepository)
    }

    val service = InnsendingStatusService(mockInnsendingRepository, mockSkjemaRepository)

    val testFnr = "12345678901"
    val testOrgnr = "123456789"

    context("opprettInnsending") {

        test("skal opprette innsending med status MOTTATT") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val innsendingSlot = slot<Innsending>()

            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.opprettInnsending(skjema)

            verify { mockInnsendingRepository.save(any()) }

            innsendingSlot.captured.skjema shouldBe skjema
            innsendingSlot.captured.status shouldBe InnsendingStatus.MOTTATT
            innsendingSlot.captured.antallForsok shouldBe 0
        }
    }

    context("oppdaterStatus") {

        test("skal oppdatere status til FERDIG") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val innsending = createTestInnsending(skjema)
            val innsendingSlot = slot<Innsending>()

            every { mockInnsendingRepository.findBySkjemaId(skjema.id!!) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.oppdaterStatus(skjema.id!!, InnsendingStatus.FERDIG)

            verify { mockInnsendingRepository.save(any()) }

            innsendingSlot.captured.status shouldBe InnsendingStatus.FERDIG
            innsendingSlot.captured.antallForsok shouldBe 1
            innsendingSlot.captured.sisteForsoek shouldNotBe null
        }

        test("skal oppdatere status med feilmelding") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val innsending = createTestInnsending(skjema)
            val innsendingSlot = slot<Innsending>()

            every { mockInnsendingRepository.findBySkjemaId(skjema.id!!) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.oppdaterStatus(
                skjema.id!!,
                InnsendingStatus.JOURNALFORING_FEILET,
                feilmelding = "Joark er nede"
            )

            innsendingSlot.captured.status shouldBe InnsendingStatus.JOURNALFORING_FEILET
            innsendingSlot.captured.feilmelding shouldBe "Joark er nede"
        }

        test("skal inkrementere antallForsok ved gjentatte oppdateringer") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val innsending = createTestInnsending(skjema)
            val innsendingSlot = slot<Innsending>()

            every { mockInnsendingRepository.findBySkjemaId(skjema.id!!) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            // Første oppdatering
            service.oppdaterStatus(skjema.id!!, InnsendingStatus.JOURNALFORING_FEILET)
            innsendingSlot.captured.antallForsok shouldBe 1

            // Andre oppdatering - innsending har nå antallForsok=1
            every { mockInnsendingRepository.findBySkjemaId(skjema.id!!) } returns innsendingSlot.captured
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.oppdaterStatus(skjema.id!!, InnsendingStatus.JOURNALFORING_FEILET)
            innsendingSlot.captured.antallForsok shouldBe 2
        }

        test("skal trunkere lange feilmeldinger til 2000 tegn") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val innsending = createTestInnsending(skjema)
            val innsendingSlot = slot<Innsending>()
            val langFeilmelding = "x".repeat(3000)

            every { mockInnsendingRepository.findBySkjemaId(skjema.id!!) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.oppdaterStatus(skjema.id!!, InnsendingStatus.JOURNALFORING_FEILET, feilmelding = langFeilmelding)

            innsendingSlot.captured.feilmelding?.length shouldBe 2000
        }
    }

    context("startProsessering") {

        test("skal sette status til UNDER_BEHANDLING og oppdatere sisteForsoek") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val innsending = createTestInnsending(skjema)
            val innsendingSlot = slot<Innsending>()

            every { mockInnsendingRepository.findBySkjemaId(skjema.id!!) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.startProsessering(skjema.id!!)

            innsendingSlot.captured.status shouldBe InnsendingStatus.UNDER_BEHANDLING
            innsendingSlot.captured.sisteForsoek shouldNotBe null
        }
    }

    context("oppdaterSkjemaJournalpostId") {

        test("skal sette journalpostId på skjema") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val skjemaSlot = slot<Skjema>()

            every { mockSkjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)
            every { mockSkjemaRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterSkjemaJournalpostId(skjema.id!!, "67890")

            skjemaSlot.captured.journalpostId shouldBe "67890"
        }
    }

    context("oppdaterStatus - feilhåndtering") {

        test("skal kaste exception når innsending ikke finnes") {
            val skjemaId = UUID.randomUUID()

            every { mockInnsendingRepository.findBySkjemaId(skjemaId) } returns null

            val exception = shouldThrow<IllegalArgumentException> {
                service.oppdaterStatus(skjemaId, InnsendingStatus.FERDIG)
            }

            exception.message shouldBe "Innsending for skjema $skjemaId ikke funnet"
        }
    }
})

private fun createTestInnsending(skjema: Skjema): Innsending {
    return Innsending(
        id = UUID.randomUUID(),
        skjema = skjema,
        status = InnsendingStatus.MOTTATT
    )
}

private fun createTestSkjema(fnr: String, orgnr: String): Skjema {
    return Skjema(
        id = UUID.randomUUID(),
        status = SkjemaStatus.SENDT,
        fnr = fnr,
        orgnr = orgnr,
        opprettetAv = fnr,
        endretAv = fnr
    )
}
