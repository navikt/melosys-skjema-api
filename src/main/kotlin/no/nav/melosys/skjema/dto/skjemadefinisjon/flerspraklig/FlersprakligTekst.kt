package no.nav.melosys.skjema.dto.skjemadefinisjon.flerspraklig

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.melosys.skjema.service.skjemadefinisjon.Språk

/**
 * Representerer en tekst på flere språk.
 * Lagres som et map fra språkkode til tekst.
 *
 * Eksempel JSON:
 * ```json
 * {
 *   "nb": "Norsk tekst",
 *   "en": "English text"
 * }
 * ```
 */
data class FlersprakligTekst
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(
    @JsonValue
    private val tekster: Map<String, String> = emptyMap()
) {
    /**
     * Henter tekst for gitt språk med fallback til norsk bokmål.
     */
    fun hent(språk: Språk): String {
        return tekster[språk.kode]
            ?: tekster[Språk.NORSK_BOKMAL.kode]
            ?: tekster.values.firstOrNull()
            ?: ""
    }

    /**
     * Henter tekst for gitt språkkode med fallback til norsk bokmål.
     */
    fun hent(språkkode: String): String {
        return tekster[språkkode]
            ?: tekster[Språk.NORSK_BOKMAL.kode]
            ?: tekster.values.firstOrNull()
            ?: ""
    }
}
