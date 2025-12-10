package no.nav.melosys.skjema.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import no.nav.melosys.skjema.domain.InnsendingMetadata
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.dto.Representasjonstype
import no.nav.melosys.skjema.dto.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.InnsendingProsesseringService
import java.time.Instant
import java.util.*

class InnsendingRetrySchedulerTest : FunSpec({

    val mockRepository = mockk<SkjemaRepository>()
    val mockProsesseringService = mockk<InnsendingProsesseringService>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    val scheduler = InnsendingRetryScheduler(mockRepository, mockProsesseringService)

    afterTest {
        clearMocks(mockRepository, mockProsesseringService)
    }

    context("retryFeiledeInnsendinger") {

        test("skal ikke kalle prosesserInnsendingAsync når ingen kandidater finnes") {
            every { mockRepository.findRetryKandidater(any()) } returns emptyList()

            scheduler.retryFeiledeInnsendinger()

            verify(exactly = 0) { mockProsesseringService.prosesserInnsendingAsync(any()) }
        }

        test("skal kalle prosesserInnsendingAsync for hver kandidat") {
            val skjema1 = createTestSkjema(UUID.randomUUID(), objectMapper)
            val skjema2 = createTestSkjema(UUID.randomUUID(), objectMapper)

            every { mockRepository.findRetryKandidater(any()) } returns listOf(skjema1, skjema2)

            scheduler.retryFeiledeInnsendinger()

            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(skjema1.id!!) }
            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(skjema2.id!!) }
        }

        test("skal fortsette med neste kandidat selv om en feiler") {
            val skjema1 = createTestSkjema(UUID.randomUUID(), objectMapper)
            val skjema2 = createTestSkjema(UUID.randomUUID(), objectMapper)

            every { mockRepository.findRetryKandidater(any()) } returns listOf(skjema1, skjema2)
            every { mockProsesseringService.prosesserInnsendingAsync(skjema1.id!!) } throws RuntimeException("Test error")

            scheduler.retryFeiledeInnsendinger()

            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(skjema1.id!!) }
            verify(exactly = 1) { mockProsesseringService.prosesserInnsendingAsync(skjema2.id!!) }
        }
    }
})

private fun createTestSkjema(id: UUID, objectMapper: ObjectMapper): Skjema {
    val metadata = UtsendtArbeidstakerMetadata(
        representasjonstype = Representasjonstype.ARBEIDSGIVER,
        harFullmakt = false,
        innsending = InnsendingMetadata(
            status = InnsendingStatus.JOURNALFORING_FEILET,
            antallForsok = 1,
            sisteForsoek = Instant.now().minusSeconds(600)
        )
    )
    return Skjema(
        id = id,
        status = SkjemaStatus.SENDT,
        fnr = "12345678901",
        orgnr = "123456789",
        metadata = objectMapper.valueToTree(metadata),
        opprettetAv = "12345678901",
        endretAv = "12345678901"
    )
}
