package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.skjema.domain.InnsendingMetadata
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.domain.SkjemaMetadata
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.SkjemaRepository
import java.util.*

class InnsendingProsesseringServiceTest : FunSpec({

    val mockRepository = mockk<SkjemaRepository>()
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    val service = InnsendingProsesseringService(mockRepository, objectMapper)

    val testSkjemaId = UUID.randomUUID()
    val testFnr = "12345678901"
    val testOrgnr = "123456789"

    context("oppdaterStatus") {

        test("skal oppdatere status til FERDIG") {
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr, objectMapper)
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.FERDIG)

            verify { mockRepository.save(any()) }

            val lagretMetadata = objectMapper.treeToValue(
                skjemaSlot.captured.metadata,
                SkjemaMetadata::class.java
            )
            lagretMetadata.innsending shouldNotBe null
            lagretMetadata.innsending!!.status shouldBe InnsendingStatus.FERDIG
            lagretMetadata.innsending!!.antallForsok shouldBe 1
        }

        test("skal oppdatere status med feilmelding") {
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr, objectMapper)
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(
                testSkjemaId,
                InnsendingStatus.JOURNALFORING_FEILET,
                feilmelding = "Joark er nede"
            )

            val lagretMetadata = objectMapper.treeToValue(
                skjemaSlot.captured.metadata,
                SkjemaMetadata::class.java
            )
            lagretMetadata.innsending!!.status shouldBe InnsendingStatus.JOURNALFORING_FEILET
            lagretMetadata.innsending!!.feilmelding shouldBe "Joark er nede"
        }

        test("skal inkrementere antallForsok ved gjentatte oppdateringer") {
            // Første oppdatering
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr, objectMapper)
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.JOURNALFORING_FEILET)

            var lagretMetadata = objectMapper.treeToValue(
                skjemaSlot.captured.metadata,
                SkjemaMetadata::class.java
            )
            lagretMetadata.innsending!!.antallForsok shouldBe 1

            // Andre oppdatering - skjema har nå metadata med antallForsok=1
            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjemaSlot.captured)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.JOURNALFORING_FEILET)

            lagretMetadata = objectMapper.treeToValue(
                skjemaSlot.captured.metadata,
                SkjemaMetadata::class.java
            )
            lagretMetadata.innsending!!.antallForsok shouldBe 2
        }

        test("skal bevare journalpostId ved statusoppdatering") {
            val skjemaMetadata = SkjemaMetadata(
                innsending = InnsendingMetadata(
                    status = InnsendingStatus.JOURNALFORT,
                    journalpostId = "12345"
                )
            )
            val skjema = createTestSkjema(testSkjemaId, testFnr, testOrgnr, objectMapper).apply {
                metadata = objectMapper.valueToTree(skjemaMetadata)
            }
            val skjemaSlot = slot<Skjema>()

            every { mockRepository.findById(testSkjemaId) } returns Optional.of(skjema)
            every { mockRepository.save(capture(skjemaSlot)) } answers { skjemaSlot.captured }

            service.oppdaterStatus(testSkjemaId, InnsendingStatus.FERDIG)

            val lagretMetadata = objectMapper.treeToValue(
                skjemaSlot.captured.metadata,
                SkjemaMetadata::class.java
            )
            lagretMetadata.innsending!!.journalpostId shouldBe "12345"
            lagretMetadata.innsending!!.status shouldBe InnsendingStatus.FERDIG
        }
    }
})

private fun createTestSkjema(
    id: UUID,
    fnr: String,
    orgnr: String,
    objectMapper: ObjectMapper
): Skjema {
    return Skjema(
        id = id,
        status = SkjemaStatus.SENDT,
        fnr = fnr,
        orgnr = orgnr,
        metadata = objectMapper.valueToTree(SkjemaMetadata()),
        opprettetAv = fnr,
        endretAv = fnr
    )
}
