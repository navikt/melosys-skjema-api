package no.nav.melosys.skjema.dto.skjemadefinisjon.flerspraklig

import no.nav.melosys.skjema.dto.skjemadefinisjon.SkjemaDefinisjon
import no.nav.melosys.skjema.service.skjemadefinisjon.Språk

/**
 * Flerspråklig hovedklasse for en skjemadefinisjon.
 * Inneholder alle seksjoner med flerspråklige tekster.
 *
 * Eksempel JSON-struktur:
 * ```json
 * {
 *   "type": "A1",
 *   "versjon": "1",
 *   "seksjoner": {
 *     "utenlandsoppdraget": {
 *       "tittel": {
 *         "nb": "Utenlandsoppdraget",
 *         "en": "Foreign assignment"
 *       },
 *       "felter": {
 *         "land": {
 *           "type": "COUNTRY_SELECT",
 *           "label": {
 *             "nb": "Hvilket land?",
 *             "en": "Which country?"
 *           },
 *           "pakrevd": true
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 */
data class FlersprakligSkjemaDefinisjonDto(
    val type: String,
    val versjon: String,
    val seksjoner: Map<String, FlersprakligSeksjonDto>
) {
    /**
     * Transformerer til enkeltspråklig SkjemaDefinisjon.
     */
    fun tilSkjemaDefinisjonDto(språk: Språk) = SkjemaDefinisjon(
        type = type,
        versjon = versjon,
        seksjoner = seksjoner.mapValues { it.value.tilSeksjonDto(språk) }
    )
}
