package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import no.nav.melosys.skjema.domain.InnsendingStatus
import java.util.*

class InnsendingProsesseringServiceTest : FunSpec({

    val mockInnsendingStatusService = mockk<InnsendingStatusService>()

    afterTest {
        clearMocks(mockInnsendingStatusService)
    }

    val service = InnsendingProsesseringService(mockInnsendingStatusService)

    context("prosesserInnsendingAsync") {

        test("skal starte prosessering og sette FERDIG ved suksess") {
            val skjemaId = UUID.randomUUID()

            every { mockInnsendingStatusService.startProsessering(skjemaId) } just Runs
            every { mockInnsendingStatusService.oppdaterStatus(skjemaId, any(), any()) } just Runs

            service.prosesserInnsendingAsync(skjemaId)

            verify(exactly = 1) { mockInnsendingStatusService.startProsessering(skjemaId) }
            verify(exactly = 1) { mockInnsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.FERDIG, null) }
        }

        test("skal oppdatere status til JOURNALFORING_FEILET ved exception") {
            val skjemaId = UUID.randomUUID()

            every { mockInnsendingStatusService.startProsessering(skjemaId) } throws RuntimeException("DB feil")
            every { mockInnsendingStatusService.oppdaterStatus(skjemaId, any(), any()) } just Runs

            service.prosesserInnsendingAsync(skjemaId)

            verify { mockInnsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.JOURNALFORING_FEILET, "DB feil") }
        }
    }
})
