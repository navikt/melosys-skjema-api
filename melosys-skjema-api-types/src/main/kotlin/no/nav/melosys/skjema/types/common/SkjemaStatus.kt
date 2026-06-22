package no.nav.melosys.skjema.types.common

/**
 * Overordnet status for et skjema.
 *
 * For detaljert sporing av asynkron prosessering, se [Skjema.innsendingStatus].
 */
enum class SkjemaStatus {
    /** Bruker jobber med søknaden - ikke sendt ennå */
    UTKAST,

    /** Bruker har sendt inn søknaden. Asynkron prosessering pågår eller er fullført. */
    SENDT,

    /**
     * LEGACY/MIDLERTIDIG: gammel soft-delete-status. Skrives ikke lenger – utkast hard-slettes nå.
     *
     * Beholdes kun så JPA fortsatt kan mappe eksisterende rader med `status='SLETTET'` uten å kaste
     * (EnumType.STRING) i tidsvinduet før admin-oppryddingen (POST /admin/utkast/rydd-slettede) har
     * fysisk slettet dem i prod. Fjernes – sammen med DB-constraintet – i en oppfølgings-PR etter at
     * oppryddingen er kjørt.
     */
    @Deprecated("Legacy soft-delete – fjernes etter at prod er ryddet via admin-endepunktet")
    SLETTET
}
