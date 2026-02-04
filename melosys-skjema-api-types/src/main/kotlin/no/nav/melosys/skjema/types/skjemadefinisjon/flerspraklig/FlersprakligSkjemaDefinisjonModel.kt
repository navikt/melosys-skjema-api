package no.nav.melosys.skjema.types.skjemadefinisjon.flerspraklig

import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto

/**
 * Flerspråklig hovedklasse for en 
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
data class FlersprakligSkjemaDefinisjonModel(
    val type: String,
    val versjon: String,
    val seksjoner: Map<String, FlersprakligSeksjonModel>
) {
    /**
     * Transformerer til enkeltspråklig SkjemaDefinisjonDto.
     */
    fun tilSkjemaDefinisjonDto(språk: Språk) = SkjemaDefinisjonDto(
        type = type,
        versjon = versjon,
        seksjoner = seksjoner.mapValues { it.value.tilSeksjonDto(språk) }
    )
}
