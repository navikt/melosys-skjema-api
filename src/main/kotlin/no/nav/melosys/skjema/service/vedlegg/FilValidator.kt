package no.nav.melosys.skjema.service.vedlegg

import no.nav.melosys.skjema.entity.VedleggFiltype
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class FilValidator {

    companion object {
        const val MAKS_FILSTORRELSE: Long = 10 * 1024 * 1024 // 10 MB
        const val MAKS_FILNAVN_LENGDE = 200

        private val PDF_MAGIC_BYTES = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
        private val PNG_MAGIC_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        private val JPEG_MAGIC_BYTES = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    }

    fun valider(fil: MultipartFile) {
        validerFilstorrelse(fil)
        validerMagicBytes(fil)
    }

    fun detekterFiltype(fil: MultipartFile): VedleggFiltype {
        val bytes = fil.inputStream.use { it.readNBytes(8) }

        return when {
            bytes.startsWith(PDF_MAGIC_BYTES) -> VedleggFiltype.PDF
            bytes.startsWith(PNG_MAGIC_BYTES) -> VedleggFiltype.PNG
            bytes.startsWith(JPEG_MAGIC_BYTES) -> VedleggFiltype.JPEG
            else -> throw IllegalArgumentException("Ugyldig filformat. Kun PDF, JPEG og PNG er tillatt.")
        }
    }

    fun sanitizeFilnavn(filnavn: String): String {
        return filnavn
            .replace(Regex("[/\\\\]"), "") // Fjern path separators
            .replace(Regex("\\.{2,}"), ".") // Fjern doble punktum
            .replace(Regex("[^a-zA-Z0-9æøåÆØÅ._\\- ]"), "_") // Erstatt spesialtegn
            .trim()
            .take(MAKS_FILNAVN_LENGDE)
            .ifBlank { "vedlegg" }
    }

    private fun validerFilstorrelse(fil: MultipartFile) {
        require(fil.size <= MAKS_FILSTORRELSE) {
            "Filen er for stor. Maks filstørrelse er 10 MB."
        }
        require(fil.size > 0) {
            "Filen er tom."
        }
    }

    private fun validerMagicBytes(fil: MultipartFile) {
        val bytes = fil.inputStream.use { it.readNBytes(8) }

        val erGyldig = bytes.startsWith(PDF_MAGIC_BYTES) ||
            bytes.startsWith(PNG_MAGIC_BYTES) ||
            bytes.startsWith(JPEG_MAGIC_BYTES)

        require(erGyldig) {
            "Ugyldig filformat. Kun PDF, JPEG og PNG er tillatt."
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }
}
