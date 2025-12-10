package no.nav.melosys.skjema.scheduler

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import no.nav.melosys.skjema.config.InnsendingRetryConfig
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.service.InnsendingProsesseringService
import java.time.Instant
import java.util.*

class InnsendingRetrySchedulerTest : FunSpec({

    val mockRepository = mockk<InnsendingRepository>()
    val mockProsesseringService = mockk<InnsendingProsesseringService>()
    val retryConfig = InnsendingRetryConfig().apply {
        fixedDelayMinutes = 5
        initialDelaySeconds = 60
        maxAttempts = 5
        staleThresholdMinutes = 5
    }

    val scheduler = InnsendingRetryScheduler(mockRepository, mockProsesseringService, retryConfig)

    afterTest {
        clearMocks(mockRepository, mockProsesseringService)
    }

    context("retryFeiledeInnsendinger") {

        test("skal ikke kalle prosesserInnsendingAsync når ingen kandidater finnes") {
            every { mockRepository.findRetryKandidater(any(), any()) } returns emptyList()

            scheduler.retryFeiledeInnsendinger()

            verify(exactly = 0) { mockProsesseringService.prosesserInnsendingAsync(any()) }
        }

        test("skal kalle prosesserInnsendingAsync for hver kandidat med skjemaId") {
            val innsending1 = createTestInnsending()
            val innsending2 = createTestInnsending()

            every { mockRepository.findRetryKandidater(any(), any()) } returns listOf(innsending1, innsending2)
            every { mockProsesseringService.prosesserInnsendingAsync(any()) } just runs

            scheduler.retryFeiledeInnsendinger()

            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(innsending1.skjema.id!!) }
            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(innsending2.skjema.id!!) }
        }

        test("skal fortsette med neste kandidat selv om en feiler") {
            val innsending1 = createTestInnsending()
            val innsending2 = createTestInnsending()

            every { mockRepository.findRetryKandidater(any(), any()) } returns listOf(innsending1, innsending2)
            every { mockProsesseringService.prosesserInnsendingAsync(innsending1.skjema.id!!) } throws RuntimeException("Test error")
            every { mockProsesseringService.prosesserInnsendingAsync(innsending2.skjema.id!!) } just runs

            scheduler.retryFeiledeInnsendinger()

            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(innsending1.skjema.id!!) }
            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(innsending2.skjema.id!!) }
        }

        test("skal bruke maxAttempts fra config") {
            every { mockRepository.findRetryKandidater(any(), eq(5)) } returns emptyList()

            scheduler.retryFeiledeInnsendinger()

            verify { mockRepository.findRetryKandidater(any(), eq(5)) }
        }
    }
})

private fun createTestInnsending(): Innsending {
    val skjema = Skjema(
        id = UUID.randomUUID(),
        status = SkjemaStatus.SENDT,
        fnr = "12345678901",
        orgnr = "123456789",
        opprettetAv = "12345678901",
        endretAv = "12345678901"
    )
    return Innsending(
        id = UUID.randomUUID(),
        skjema = skjema,
        status = InnsendingStatus.JOURNALFORING_FEILET,
        antallForsok = 1,
        sisteForsoek = Instant.now().minusSeconds(600)
    )
}
