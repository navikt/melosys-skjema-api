package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

class InnsendingProsesseringServiceTest : FunSpec({

    val mockInnsendingRepository = mockk<InnsendingRepository>()
    val mockSkjemaRepository = mockk<SkjemaRepository>()

    val service = InnsendingProsesseringService(mockInnsendingRepository, mockSkjemaRepository)

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

            every { mockInnsendingRepository.findBySkjema(skjema) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.oppdaterStatus(skjema, InnsendingStatus.FERDIG)

            verify { mockInnsendingRepository.save(any()) }

            innsendingSlot.captured.status shouldBe InnsendingStatus.FERDIG
            innsendingSlot.captured.antallForsok shouldBe 1
            innsendingSlot.captured.sisteForsoek shouldNotBe null
        }

        test("skal oppdatere status med feilmelding") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val innsending = createTestInnsending(skjema)
            val innsendingSlot = slot<Innsending>()

            every { mockInnsendingRepository.findBySkjema(skjema) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.oppdaterStatus(
                skjema,
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

            every { mockInnsendingRepository.findBySkjema(skjema) } returns innsending
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            // Første oppdatering
            service.oppdaterStatus(skjema, InnsendingStatus.JOURNALFORING_FEILET)
            innsendingSlot.captured.antallForsok shouldBe 1

            // Andre oppdatering - innsending har nå antallForsok=1
            every { mockInnsendingRepository.findBySkjema(skjema) } returns innsendingSlot.captured
            every { mockInnsendingRepository.save(capture(innsendingSlot)) } answers { innsendingSlot.captured }

            service.oppdaterStatus(skjema, InnsendingStatus.JOURNALFORING_FEILET)
            innsendingSlot.captured.antallForsok shouldBe 2
        }
    }

    context("oppdaterSkjemaJournalpostId") {

        test("skal sette journalpostId på skjema") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val skjemaSlot = slot<Skjema>()

            every { mockSkjemaRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterSkjemaJournalpostId(skjema, "67890")

            skjemaSlot.captured.journalpostId shouldBe "67890"
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
