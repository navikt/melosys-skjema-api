package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.Vedlegg
import no.nav.melosys.skjema.exception.VedleggVirusFunnetException
import no.nav.melosys.skjema.integrasjon.clamav.ClamAvClient
import no.nav.melosys.skjema.integrasjon.storage.VedleggStorageClient
import no.nav.melosys.skjema.repository.VedleggRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import org.springframework.web.multipart.MultipartFile

class VedleggServiceTest : FunSpec({

    val mockUtsendtArbeidstakerService = mockk<UtsendtArbeidstakerService>()
    val mockVedleggRepository = mockk<VedleggRepository>()
    val mockClamAvClient = mockk<ClamAvClient>()
    val mockVedleggStorageClient = mockk<VedleggStorageClient>()
    val mockSubjectHandler = mockk<SubjectHandler>()

    val vedleggService = VedleggService(
        mockUtsendtArbeidstakerService,
        mockVedleggRepository,
        mockClamAvClient,
        mockVedleggStorageClient
    )

    val skjemaId = UUID.randomUUID()
    val vedleggId = UUID.randomUUID()
    val fnr = "12345678901"

    fun lagSkjema(status: SkjemaStatus = SkjemaStatus.UTKAST): Skjema {
        val skjema = mockk<Skjema>()
        every { skjema.id } returns skjemaId
        every { skjema.status } returns status
        every { skjema.fnr } returns fnr
        return skjema
    }

    fun lagMultipartFile(): MultipartFile {
        val fil = mockk<MultipartFile>()
        every { fil.originalFilename } returns "test.pdf"
        every { fil.size } returns 1024
        every { fil.bytes } returns "%PDF-1.4 content".toByteArray()
        every { fil.contentType } returns "application/pdf"
        every { fil.inputStream } answers { "%PDF-1.4 content".toByteArray().inputStream() }
        return fil
    }

    beforeTest {
        SubjectHandler.set(mockSubjectHandler)
        every { mockSubjectHandler.getUserID() } returns fnr
    }

    context("lastOpp") {
        test("laster opp vedlegg vellykket") {
            val skjema = lagSkjema()
            val fil = lagMultipartFile()

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema
            every { mockVedleggRepository.countBySkjemaId(skjemaId) } returns 0
            every { mockClamAvClient.scan(fil) } just Runs
            every { mockVedleggStorageClient.lastOpp(any(), any(), any()) } just Runs
            every { mockVedleggRepository.save(any()) } answers {
                val vedlegg = firstArg<Vedlegg>()
                vedlegg
            }

            val result = vedleggService.lastOpp(skjemaId, fil)

            result.filnavn shouldBe "test.pdf"
            result.filtype shouldBe VedleggFiltype.PDF
            verify { mockClamAvClient.scan(fil) }
            verify { mockVedleggStorageClient.lastOpp(any(), any(), any()) }
        }

        test("feiler når skjema ikke er UTKAST") {
            val skjema = lagSkjema(status = SkjemaStatus.SENDT)
            val fil = lagMultipartFile()

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema

            shouldThrow<IllegalArgumentException> {
                vedleggService.lastOpp(skjemaId, fil)
            }.message shouldBe "Kan kun laste opp vedlegg til skjemaer med status UTKAST"
        }

        test("feiler når maks antall vedlegg er nådd") {
            val skjema = lagSkjema()
            val fil = lagMultipartFile()

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema
            every { mockVedleggRepository.countBySkjemaId(skjemaId) } returns 10

            shouldThrow<IllegalArgumentException> {
                vedleggService.lastOpp(skjemaId, fil)
            }.message shouldBe "Maks antall vedlegg (10) er nådd"
        }

        test("feiler når ClamAV finner virus") {
            val skjema = lagSkjema()
            val fil = lagMultipartFile()

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema
            every { mockVedleggRepository.countBySkjemaId(skjemaId) } returns 0
            every { mockClamAvClient.scan(fil) } throws VedleggVirusFunnetException("Virus funnet")

            shouldThrow<VedleggVirusFunnetException> {
                vedleggService.lastOpp(skjemaId, fil)
            }
        }
    }

    context("list") {
        test("returnerer vedlegg for skjema") {
            val skjema = lagSkjema()
            val vedlegg = mockk<Vedlegg>()
            every { vedlegg.id } returns vedleggId
            every { vedlegg.originalFilnavn } returns "test.pdf"
            every { vedlegg.filtype } returns VedleggFiltype.PDF
            every { vedlegg.filstorrelse } returns 1024
            every { vedlegg.opprettetDato } returns java.time.Instant.now()

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema
            every { mockVedleggRepository.findBySkjemaId(skjemaId) } returns listOf(vedlegg)

            val result = vedleggService.list(skjemaId)

            result.size shouldBe 1
            result[0].filnavn shouldBe "test.pdf"
        }
    }

    context("slett") {
        test("sletter vedlegg vellykket") {
            val skjema = lagSkjema()
            val vedlegg = mockk<Vedlegg>()
            every { vedlegg.storageReferanse } returns "skjemaer/$skjemaId/vedlegg/$vedleggId/test.pdf"

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema
            every { mockVedleggRepository.findByIdAndSkjemaId(vedleggId, skjemaId) } returns vedlegg
            every { mockVedleggStorageClient.slett(any()) } just Runs
            every { mockVedleggRepository.delete(vedlegg) } just Runs

            vedleggService.slett(skjemaId, vedleggId)

            verify { mockVedleggStorageClient.slett(any()) }
            verify { mockVedleggRepository.delete(vedlegg) }
        }

        test("feiler når vedlegg ikke finnes") {
            val skjema = lagSkjema()

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema
            every { mockVedleggRepository.findByIdAndSkjemaId(vedleggId, skjemaId) } returns null

            shouldThrow<NoSuchElementException> {
                vedleggService.slett(skjemaId, vedleggId)
            }
        }

        test("feiler når skjema ikke er UTKAST") {
            val skjema = lagSkjema(status = SkjemaStatus.SENDT)

            every { mockUtsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId) } returns skjema

            shouldThrow<IllegalArgumentException> {
                vedleggService.slett(skjemaId, vedleggId)
            }
        }
    }
})
