package no.nav.melosys.skjema.types.utsendtarbeidstaker

/**
 * Hvordan et skjema ble startet. Brukes kun til aggregert bruksstatistikk i admin.
 * Null i databasen betyr opprettet før målingen fantes.
 */
enum class OpprettetVia {
    /** Startet fra «motpart har sendt inn sin del»-oppfordringen på oversikten. */
    MOTPART_CTA,

    /** Startet på eget initiativ via ordinær flyt. */
    ORDINAER
}
