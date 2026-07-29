package no.nav.melosys.skjema.types.utsendtarbeidstaker

/**
 * Hvordan et skjema ble startet, når det skjedde via en annen inngang enn ordinær flyt
 * (null = ordinær flyt). Brukes kun til aggregert bruksstatistikk i admin.
 */
enum class OpprettetVia {
    /** Startet fra «motpart har sendt inn sin del»-oppfordringen på oversikten. */
    MOTPART_CTA
}
