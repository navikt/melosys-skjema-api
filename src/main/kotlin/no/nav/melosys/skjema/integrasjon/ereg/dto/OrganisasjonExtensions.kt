package no.nav.melosys.skjema.integrasjon.ereg.dto

import no.nav.melosys.skjema.dto.SimpleOrganisasjonDto

/**
 * Finner organisasjonsnummeret til den øverste juridiske enheten ved å traversere organisasjonshierarkiet.
 *
 * For JuridiskEnhet:
 * - Returnerer sitt eget organisasjonsnummer (allerede på toppen)
 *
 * For Virksomhet:
 * - Hvis inngaarIJuridiskEnheter er tilstede, returner dets organisasjonsnummer
 * - Ellers, traverser oppover gjennom bestaarAvOrganisasjonsledd og fortsett rekursivt
 *
 * For Organisasjonsledd:
 * - Hvis inngaarIJuridiskEnheter er tilstede, returner dets organisasjonsnummer
 * - Ellers, traverser oppover gjennom organisasjonsleddOver og fortsett rekursivt
 *
 * @return Organisasjonsnummeret til den juridiske enheten på toppen av hierarkiet, eller null hvis ikke funnet
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

fun Organisasjon.toSimpleOrganisasjonDto() = SimpleOrganisasjonDto(
        orgnr = this.organisasjonsnummer,
        navn = this.navn?.sammensattnavn ?: "Ukjent navn",
    )
