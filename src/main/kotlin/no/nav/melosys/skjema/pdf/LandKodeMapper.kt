package no.nav.melosys.skjema.pdf

import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.LandKode

/**
 * Mapper fra landskoder til landnavn.
 * Delegerer til LandKode-enumet som inneholder flerspråklige navn.
 */
object LandKodeMapper {

    /**
     * Henter landnavn fra landskode.
     * Returnerer landskoden selv hvis den ikke finnes i mappingen.
     *
     * @param kode ISO 3166-1 alpha-2 landskode
     * @param språk Språk for landnavnet (default: norsk bokmål)
     */
    fun hentLandnavn(kode: String, språk: Språk = Språk.NORSK_BOKMAL): String {
        return LandKode.hentLandnavn(kode, språk)
    }
}
