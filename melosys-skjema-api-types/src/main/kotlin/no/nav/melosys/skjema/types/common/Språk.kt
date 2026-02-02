package no.nav.melosys.skjema.types.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Støttede språk for skjemadefinisjoner.
 *
 * Språkene finnes i én flerspråklig JSON-fil:
 * resources/skjema-definisjoner/{type}/v{versjon}/definisjon.json
 *
 * Serialiseres/deserialiseres som språkkode ("nb", "en") via Jackson.
 */
enum class Språk(@get:JsonValue val kode: String) {
    NORSK_BOKMAL("nb"),
    ENGELSK("en");

    companion object {
        private val kodeMap = entries.associateBy { it.kode }

        /**
         * Konverterer språkkode til Språk enum.
         *
         * @param kode Språkkode (f.eks. "nb", "en")
         * @return Språk enum
         * @throws IllegalArgumentException hvis kode ikke er gyldig
         */
        @JvmStatic
        @JsonCreator
        fun fraKode(kode: String): Språk {
            return kodeMap[kode.lowercase()]
                ?: throw IllegalArgumentException(
                    "Ugyldig språkkode: '$kode'. Gyldige verdier: ${kodeMap.keys}"
                )
        }

        /**
         * Sjekker om en språkkode er gyldig.
         */
        fun erGyldig(kode: String): Boolean {
            return kodeMap.containsKey(kode.lowercase())
        }
    }
}
