package no.nav.melosys.skjema.vedlegg

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import org.springframework.web.multipart.MultipartFile

class FilValidatorTest : FunSpec({

    fun lagMultipartFile(bytes: ByteArray, size: Long = bytes.size.toLong(), filename: String = "test.pdf"): MultipartFile {
        val fil = mockk<MultipartFile>()
        every { fil.inputStream } answers { bytes.inputStream() }
        every { fil.bytes } returns bytes
        every { fil.size } returns size
        every { fil.originalFilename } returns filename
        every { fil.contentType } returns "application/octet-stream"
        return fil
    }

    context("valider") {
        test("godtar gyldig PDF") {
            val pdfBytes = "%PDF-1.4 rest of content".toByteArray()
            val fil = lagMultipartFile(pdfBytes)
            shouldNotThrow<IllegalArgumentException> { FilValidator.valider(fil) }
        }

        test("godtar gyldig JPEG") {
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(10)
            val fil = lagMultipartFile(jpegBytes, filename = "test.jpg")
            shouldNotThrow<IllegalArgumentException> { FilValidator.valider(fil) }
        }

        test("godtar gyldig PNG") {
            val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(10)
            val fil = lagMultipartFile(pngBytes, filename = "test.png")
            shouldNotThrow<IllegalArgumentException> { FilValidator.valider(fil) }
        }

        test("avviser ugyldig filformat") {
            val docBytes = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte()) + ByteArray(10)
            val fil = lagMultipartFile(docBytes, filename = "test.doc")
            shouldThrow<IllegalArgumentException> { FilValidator.valider(fil) }
                .message shouldBe "Ugyldig filformat. Kun PDF, JPEG og PNG er tillatt."
        }

        test("avviser for stor fil") {
            val pdfBytes = "%PDF-1.4".toByteArray()
            val fil = lagMultipartFile(pdfBytes, size = 11 * 1024 * 1024)
            shouldThrow<IllegalArgumentException> { FilValidator.valider(fil) }
                .message shouldBe "Filen er for stor. Maks filstørrelse er 10 MB."
        }

        test("avviser tom fil") {
            val pdfBytes = "%PDF-1.4".toByteArray()
            val fil = lagMultipartFile(pdfBytes, size = 0)
            shouldThrow<IllegalArgumentException> { FilValidator.valider(fil) }
                .message shouldBe "Filen er tom."
        }
    }

    context("detekterFiltype") {
        test("detekterer PDF") {
            val pdfBytes = "%PDF-1.4 rest of content".toByteArray()
            val fil = lagMultipartFile(pdfBytes)
            FilValidator.detekterFiltype(fil) shouldBe VedleggFiltype.PDF
        }

        test("detekterer JPEG") {
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(10)
            val fil = lagMultipartFile(jpegBytes)
            FilValidator.detekterFiltype(fil) shouldBe VedleggFiltype.JPEG
        }

        test("detekterer PNG") {
            val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(10)
            val fil = lagMultipartFile(pngBytes)
            FilValidator.detekterFiltype(fil) shouldBe VedleggFiltype.PNG
        }

        test("kaster feil for ukjent filtype") {
            val unknownBytes = ByteArray(10) { 0x00 }
            val fil = lagMultipartFile(unknownBytes)
            shouldThrow<IllegalArgumentException> { FilValidator.detekterFiltype(fil) }
        }
    }

    context("sanitizeFilnavn") {
        test("fjerner path separators") {
            FilValidator.sanitizeFilnavn("../../etc/passwd") shouldBe ".etcpasswd"
        }

        test("fjerner doble punktum") {
            FilValidator.sanitizeFilnavn("fil..navn.pdf") shouldBe "fil.navn.pdf"
        }

        test("erstatter spesialtegn") {
            FilValidator.sanitizeFilnavn("fil<>:\"|?*.pdf") shouldBe "fil_______.pdf"
        }

        test("begrenser lengde til 200 tegn") {
            val langtNavn = "a".repeat(250) + ".pdf"
            FilValidator.sanitizeFilnavn(langtNavn).length shouldBe 200
        }

        test("returnerer 'vedlegg' for tomt filnavn") {
            FilValidator.sanitizeFilnavn("") shouldBe "vedlegg"
        }

        test("bevarer norske tegn") {
            FilValidator.sanitizeFilnavn("søknad_ÆØÅ.pdf") shouldBe "søknad_ÆØÅ.pdf"
        }
    }
})
