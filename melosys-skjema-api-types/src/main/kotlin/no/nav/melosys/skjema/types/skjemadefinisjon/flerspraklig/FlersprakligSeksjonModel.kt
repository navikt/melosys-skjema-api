package no.nav.melosys.skjema.types.skjemadefinisjon.flerspraklig

import no.nav.melosys.skjema.types.skjemadefinisjon.SeksjonDefinisjonDto
import no.nav.melosys.skjema.types.common.Språk

/**
 * Flerspråklig definisjon av en seksjon i et skjema.
 */
data class FlersprakligSeksjonModel(
    val tittel: FlersprakligTekst,
    val beskrivelse: FlersprakligTekst? = null,
    val felter: Map<String, FlersprakligFeltModel>
) {
    /**
     * Transformerer til enkeltspråklig SeksjonDefinisjonDto.
     */
    fun tilSeksjonDto(språk: Språk) = SeksjonDefinisjonDto(
        tittel = tittel.hent(språk),
        beskrivelse = beskrivelse?.hent(språk),
        felter = felter.mapValues { it.value.tilFeltDto(språk) }
    )
}
