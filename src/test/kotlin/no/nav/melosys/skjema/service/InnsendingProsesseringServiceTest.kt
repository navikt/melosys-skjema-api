package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.kafka.SkjemaMottattProducer
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet
import java.util.*

class InnsendingProsesseringServiceTest : FunSpec({

    val mockInnsendingStatusService = mockk<InnsendingStatusService>()
    val mockSkjemaMottattProducer = mockk<SkjemaMottattProducer>(relaxed = true)

    afterTest {
        clearMocks(mockInnsendingStatusService)
    }

    val service = InnsendingProsesseringService(
        mockInnsendingStatusService,
        mockSkjemaMottattProducer
    )

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

        test("skal oppdatere status til KAFKA_FEILET ved SendSkjemaMottattMeldingFeilet") {
            val skjemaId = UUID.randomUUID()

            every { mockInnsendingStatusService.startProsessering(skjemaId) } just Runs
            every { mockSkjemaMottattProducer.blokkerendeSendSkjemaMottatt(any()) } throws SendSkjemaMottattMeldingFeilet(
                "Feil ved sending av skjema-mottatt melding for skjemaId=$skjemaId",
                RuntimeException("Kafka er nede")
            )
            every { mockInnsendingStatusService.oppdaterStatus(skjemaId, any(), any()) } just Runs

            service.prosesserInnsendingAsync(skjemaId)

            verify { mockInnsendingStatusService.oppdaterStatus(skjemaId, InnsendingStatus.KAFKA_FEILET, match { it.contains(skjemaId.toString()) }) }
        }
    }
})
