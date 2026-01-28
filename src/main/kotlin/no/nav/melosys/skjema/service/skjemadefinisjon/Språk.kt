package no.nav.melosys.skjema.service.skjemadefinisjon

/**
 * Støttede språk for skjemadefinisjoner.
 *
 * Merk: Nynorsk (nn) og engelsk (en) kan legges til senere ved å:
 * 1. Legge til enum-verdier her
 * 2. Opprette tilhørende JSON-filer i resources/skjema-definisjoner/{type}/v{versjon}/
 */
enum class Språk(val kode: String) {
    NORSK_BOKMAL("nb");

    companion object {
        private val kodeMap = entries.associateBy { it.kode }

        /**
         * Konverterer språkkode til Språk enum.
         *
         * @param kode Språkkode (f.eks. "nb", "nn", "en")
         * @return Språk enum
         * @throws IllegalArgumentException hvis kode ikke er gyldig
         */
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
