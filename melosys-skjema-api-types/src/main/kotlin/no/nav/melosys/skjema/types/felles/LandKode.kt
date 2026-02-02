package no.nav.melosys.skjema.types.felles

import no.nav.melosys.skjema.types.common.Språk

// https://github.com/navikt/melosys-kodeverk/blob/master/src/landkoder.js

/**
 * EØS-land og Sveits som er relevante for A1-søknader.
 * Inneholder flerspråklige landnavn (norsk og engelsk).
 */
enum class LandKode(
    private val navnNorsk: String,
    private val navnEngelsk: String
) {
    AT("Østerrike", "Austria"),
    AX("Åland", "Åland Islands"),
    BE("Belgia", "Belgium"),
    BG("Bulgaria", "Bulgaria"),
    CH("Sveits", "Switzerland"),
    CY("Kypros", "Cyprus"),
    CZ("Tsjekkia", "Czech Republic"),
    DE("Tyskland", "Germany"),
    DK("Danmark", "Denmark"),
    EE("Estland", "Estonia"),
    ES("Spania", "Spain"),
    FI("Finland", "Finland"),
    FO("Færøyene", "Faroe Islands"),
    FR("Frankrike", "France"),
    GB("Storbritannia", "United Kingdom"),
    GL("Grønland", "Greenland"),
    GR("Hellas", "Greece"),
    HR("Kroatia", "Croatia"),
    HU("Ungarn", "Hungary"),
    IE("Irland", "Ireland"),
    IS("Island", "Iceland"),
    IT("Italia", "Italy"),
    LI("Liechtenstein", "Liechtenstein"),
    LT("Litauen", "Lithuania"),
    LU("Luxembourg", "Luxembourg"),
    LV("Latvia", "Latvia"),
    MT("Malta", "Malta"),
    NL("Nederland", "Netherlands"),
    PL("Polen", "Poland"),
    PT("Portugal", "Portugal"),
    RO("Romania", "Romania"),
    SE("Sverige", "Sweden"),
    SI("Slovenia", "Slovenia"),
    SJ("Svalbard og Jan Mayen", "Svalbard and Jan Mayen"),
    SK("Slovakia", "Slovakia");

    /**
     * Henter landnavn på gitt språk.
     */
    fun hentNavn(språk: Språk): String = when (språk) {
        Språk.NORSK_BOKMAL -> navnNorsk
        Språk.ENGELSK -> navnEngelsk
    }

    companion object {
        /**
         * Finner LandKode fra ISO 3166-1 alpha-2 kode (case-insensitive).
         * Returnerer null hvis koden ikke finnes.
         */
        fun fraKode(kode: String): LandKode? {
            return entries.find { it.name.equals(kode, ignoreCase = true) }
        }

        /**
         * Henter landnavn fra landskode og språk.
         * Returnerer landskoden selv hvis den ikke finnes.
         */
        fun hentLandnavn(kode: String, språk: Språk = Språk.NORSK_BOKMAL): String {
            return fraKode(kode)?.hentNavn(språk) ?: kode
        }
    }
}