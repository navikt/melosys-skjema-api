package no.nav.melosys.skjema.integrasjon.ereg.dto

/**
 * Finds the top-level juridisk enhet organisasjonsnummer by traversing the organization hierarchy.
 *
 * For JuridiskEnhet:
 * - Returns its own organisasjonsnummer (already at the top)
 *
 * For Virksomhet:
 * - If inngaarIJuridiskEnheter is present, return its organisasjonsnummer
 * - Otherwise, traverse up through bestaarAvOrganisasjonsledd and continue recursively
 *
 * For Organisasjonsledd:
 * - If inngaarIJuridiskEnheter is present, return its organisasjonsnummer
 * - Otherwise, traverse up through organisasjonsleddOver and continue recursively
 *
 * @return The organisasjonsnummer of the juridisk enhet at the top of the hierarchy, or null if not found
 */
fun Organisasjon.finnJuridiskEnhetOrganisasjonsnummer(): String? {
    return when (this) {
        is JuridiskEnhet -> {
            // Already at the top level
            organisasjonsnummer
        }

        is Virksomhet -> inngaarIJuridiskEnheter?.firstOrNull()?.organisasjonsnummer
            ?: bestaarAvOrganisasjonsledd
                ?.firstOrNull()?.organisasjonsledd?.finnJuridiskEnhetOrganisasjonsnummer()

        is Organisasjonsledd -> inngaarIJuridiskEnheter?.firstOrNull()?.organisasjonsnummer
            ?: organisasjonsleddOver
                ?.firstOrNull()?.organisasjonsledd?.finnJuridiskEnhetOrganisasjonsnummer()
    }
}