package no.nav.melosys.skjema.dto.skjemadefinisjon.flerspraklig

import no.nav.melosys.skjema.dto.skjemadefinisjon.SeksjonDefinisjon
import no.nav.melosys.skjema.service.skjemadefinisjon.Språk

/**
 * Flerspråklig definisjon av en seksjon i et skjema.
 */
data class FlersprakligSeksjonDto(
    val tittel: FlersprakligTekst,
    val beskrivelse: FlersprakligTekst? = null,
    val felter: Map<String, FlersprakligFeltDto>
) {
    /**
     * Transformerer til enkeltspråklig SeksjonDefinisjon.
     */
    fun tilSeksjonDto(språk: Språk) = SeksjonDefinisjon(
        tittel = tittel.hent(språk),
        beskrivelse = beskrivelse?.hent(språk),
        felter = felter.mapValues { it.value.tilFeltDto(språk) }
    )
}
