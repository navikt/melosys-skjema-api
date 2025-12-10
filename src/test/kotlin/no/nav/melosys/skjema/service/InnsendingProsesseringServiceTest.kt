package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import java.util.*

class InnsendingProsesseringServiceTest : FunSpec({

    val mockInnsendingStatusService = mockk<InnsendingStatusService>()

    afterTest {
        clearMocks(mockInnsendingStatusService)
    }

    val service = InnsendingProsesseringService(mockInnsendingStatusService)

    val testFnr = "12345678901"
    val testOrgnr = "123456789"

    context("prosesserInnsendingAsync") {

        test("skal sette UNDER_BEHANDLING og deretter FERDIG ved vellykket prosessering") {
            val skjema = createTestSkjema(testFnr, testOrgnr)
            val statusUpdates = mutableListOf<InnsendingStatus>()

            every { mockInnsendingStatusService.oppdaterStatusUtenInkrementForsok(skjema, capture(statusUpdates)) } just Runs
            every { mockInnsendingStatusService.oppdaterStatus(skjema, capture(statusUpdates), any()) } just Runs

            service.prosesserInnsendingAsync(skjema)

            verify(exactly = 1) { mockInnsendingStatusService.oppdaterStatusUtenInkrementForsok(skjema, InnsendingStatus.UNDER_BEHANDLING) }
            verify(exactly = 1) { mockInnsendingStatusService.oppdaterStatus(skjema, InnsendingStatus.FERDIG, null) }
        }

        test("skal oppdatere status til JOURNALFORING_FEILET ved exception") {
            val skjema = createTestSkjema(testFnr, testOrgnr)

            every { mockInnsendingStatusService.oppdaterStatusUtenInkrementForsok(skjema, any()) } throws RuntimeException("DB feil")
            every { mockInnsendingStatusService.oppdaterStatus(skjema, any(), any()) } just Runs

            service.prosesserInnsendingAsync(skjema)

            verify { mockInnsendingStatusService.oppdaterStatus(skjema, InnsendingStatus.JOURNALFORING_FEILET, "DB feil") }
        }

        test("skal håndtere feil uten å kaste exception videre") {
            val skjema = createTestSkjema(testFnr, testOrgnr)

            every { mockInnsendingStatusService.oppdaterStatusUtenInkrementForsok(skjema, any()) } just Runs
            every { mockInnsendingStatusService.oppdaterStatus(skjema, any(), any()) } just Runs

            // Skal ikke kaste exception
            service.prosesserInnsendingAsync(skjema)
        }
    }
})

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
