package no.nav.melosys.skjema.featuretoggle

/**
 * Toggle-navn i teamets Unleash-instans. Konvensjon: `melosys.skjema.<beskrivelse>`,
 * lowercase uten æøå. Frontend sender de samme strengene til /api/featuretoggle,
 * så navnene her og i melosys-skjema-web må matche eksakt.
 */
object ToggleNavn {
    /** «Motpart har sendt inn – start din del»-CTA på oversikten i melosys-skjema-web. */
    const val MOTPART_CTA = "melosys.skjema.motpart-cta"

    /** Sammendrag over innsendte søknader (antall innsendt / venter på motpart) på oversikten. */
    const val INNSENDT_SAMMENDRAG = "melosys.skjema.innsendt-sammendrag"
}
