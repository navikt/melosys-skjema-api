package no.nav.melosys.skjema.types.common

/**
 * Brukervendt status for saken i melosys-api, synkronisert til melosys-skjema-api via M2M-kall.
 *
 * Bevisst begrenset til to verdier inntil faglig/juridisk avklaring åpner for mer detaljert
 * statusvisning. melosys-api mapper sine interne saksstatuser til disse (kun OPPRETTET
 * regnes som MOTTATT; alt annet, inkludert LOVVALG_AVKLART og henlagt/annullert, er AVSLUTTET).
 */
enum class Saksstatus {
    /** Saken er aktiv/under behandling i melosys-api */
    MOTTATT,

    /** Saken er ferdigbehandlet (avsluttet, lovvalg avklart, henlagt, annullert m.m.) */
    AVSLUTTET
}
