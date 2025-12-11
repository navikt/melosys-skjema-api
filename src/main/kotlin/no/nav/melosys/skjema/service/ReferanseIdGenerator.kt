package no.nav.melosys.skjema.service

import no.nav.melosys.skjema.repository.SkjemaRepository
import org.springframework.stereotype.Component
import java.security.SecureRandom

/**
 * Genererer unike, brukervennlige referanse-IDer for søknader.
 *
 * Format: MEL-XXXXXX (prefiks + 6 alfanumeriske tegn)
 * Eksempel: MEL-AB12CD
 *
 * Unngår forvekslingsbare tegn (0/O, 1/I/L) for bedre lesbarhet.
 */
@Component
class ReferanseIdGenerator(
    private val skjemaRepository: SkjemaRepository
) {
    private val random = SecureRandom()

    companion object {
        private const val PREFIKS = "MEL"
        private const val KODE_LENGDE = 6
        private const val MAKS_FORSOK = 5
        // Unngår forvekslingsbare tegn: 0/O, 1/I/L
        private const val GYLDIGE_TEGN = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }

    fun generer(): String {
        repeat(MAKS_FORSOK) {
            val kode = (1..KODE_LENGDE)
                .map { GYLDIGE_TEGN[random.nextInt(GYLDIGE_TEGN.length)] }
                .joinToString("")
            val referanseId = "$PREFIKS-$kode"

            if (!skjemaRepository.existsByReferanseId(referanseId)) {
                return referanseId
            }
        }
        throw IllegalStateException("Kunne ikke generere unik referanseId etter $MAKS_FORSOK forsøk")
    }
}
