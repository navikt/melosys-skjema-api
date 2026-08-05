package no.nav.melosys.skjema.featuretoggle

/**
 * Toggles i teamets Unleash-instans. Konvensjon: `melosys.skjema.<beskrivelse>`,
 * lowercase uten æøå. Frontend sender de samme strengene til /api/featuretoggle,
 * så navnene her og i melosys-skjema-web må matche eksakt.
 *
 * Allowlisten [ALLE] utledes av enum-verdiene – en ny toggle blir dermed automatisk
 * tilgjengelig for frontend uten eget vedlikehold av listen.
 */
enum class ToggleNavn(val navn: String) {
    /** «Motpart har sendt inn – start din del»-CTA på oversikten i melosys-skjema-web. */
    MOTPART_CTA("melosys.skjema.motpart-cta"),

    /** Sammendrag over innsendte søknader (antall innsendt / venter på motpart) på oversikten. */
    INNSENDT_SAMMENDRAG("melosys.skjema.innsendt-sammendrag");

    companion object {
        /** Allowlist for /api/featuretoggle – kun disse kan evalueres av frontend. */
        val ALLE: Set<String> = entries.mapTo(mutableSetOf()) { it.navn }
    }
}
