package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.SkjemaRepository
import java.util.*

class InnsendingProsesseringServiceTest : FunSpec({

    val mockRepository = mockk<SkjemaRepository>()

    val service = InnsendingProsesseringService(mockRepository)

    val testSkjemaId = UUID.randomUUID()
    val testFnr = "12345678901"
    val testOrgnr = "123456789"

    context("oppdaterStatus") {

        test("skal oppdatere status til FERDIG") {
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr)
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.FERDIG)

            verify { mockRepository.save(any()) }

            skjemaSlot.captured.innsendingStatus shouldBe InnsendingStatus.FERDIG
            skjemaSlot.captured.innsendingAntallForsok shouldBe 1
            skjemaSlot.captured.innsendingSisteForsoek shouldNotBe null
        }

        test("skal oppdatere status med feilmelding") {
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr)
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(
                testSkjemaId,
                InnsendingStatus.JOURNALFORING_FEILET,
                feilmelding = "Joark er nede"
            )

            skjemaSlot.captured.innsendingStatus shouldBe InnsendingStatus.JOURNALFORING_FEILET
            skjemaSlot.captured.innsendingFeilmelding shouldBe "Joark er nede"
        }

        test("skal inkrementere antallForsok ved gjentatte oppdateringer") {
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr)
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            // Første oppdatering
            service.oppdaterStatus(testSkjemaId, InnsendingStatus.JOURNALFORING_FEILET)
            skjemaSlot.captured.innsendingAntallForsok shouldBe 1

            // Andre oppdatering - skjema har nå antallForsok=1
            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjemaSlot.captured)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.JOURNALFORING_FEILET)
            skjemaSlot.captured.innsendingAntallForsok shouldBe 2
        }

        test("skal bevare journalpostId ved statusoppdatering") {
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr).apply {
                innsendingStatus = InnsendingStatus.JOURNALFORT
                journalpostId = "12345"
            }
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.FERDIG)

            skjemaSlot.captured.journalpostId shouldBe "12345"
            skjemaSlot.captured.innsendingStatus shouldBe InnsendingStatus.FERDIG
        }

        test("skal sette journalpostId når den oppgis") {
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr)
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.JOURNALFORT, journalpostId = "67890")

            skjemaSlot.captured.journalpostId shouldBe "67890"
            skjemaSlot.captured.innsendingStatus shouldBe InnsendingStatus.JOURNALFORT
        }
    }
})

private fun createTestSkjema(
    id: UUID,
    fnr: String,
    orgnr: String
): Skjema {
    return Skjema(
        id = id,
        status = SkjemaStatus.SENDT,
        fnr = fnr,
        orgnr = orgnr,
        opprettetAv = fnr,
        endretAv = fnr
    )
}
