package no.nav.melosys.skjema.integrasjon.ereg.dto

import no.nav.melosys.skjema.types.felles.SimpleOrganisasjonDto
import java.time.LocalDate

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
 * Robusthet:
 * - Traverserer ALLE grener i hierarkiet (ikke bare den første) slik at en juridisk enhet
 *   som ligger via et annet ledd enn det første fortsatt blir funnet.
 * - Foretrekker juridiske enheter som er gyldige på [idag], men faller tilbake til første
 *   kobling med organisasjonsnummer hvis ingen er eksplisitt gyldige (unngår regresjon).
 * - Ignorerer koblinger uten organisasjonsnummer.
 *
 * @return Organisasjonsnummeret til den juridiske enheten på toppen av hierarkiet, eller null hvis ikke funnet
 */
fun Organisasjon.finnJuridiskEnhetOrganisasjonsnummer(idag: LocalDate = LocalDate.now()): String? {
    return when (this) {
        is JuridiskEnhet -> {
            // Allerede på toppnivå
            organisasjonsnummer
        }

        is Virksomhet -> inngaarIJuridiskEnheter.velgJuridiskEnhetOrganisasjonsnummer(idag)
            ?: bestaarAvOrganisasjonsledd.finnFoersteJuridiskEnhetIHierarki(idag)

        is Organisasjonsledd -> inngaarIJuridiskEnheter.velgJuridiskEnhetOrganisasjonsnummer(idag)
            ?: organisasjonsleddOver.finnFoersteJuridiskEnhetIHierarki(idag)
    }
}

/**
 * Søker gjennom alle ledd og returnerer første juridiske enhet som finnes oppover i hierarkiet.
 */
private fun List<BestaarAvOrganisasjonsledd>?.finnFoersteJuridiskEnhetIHierarki(idag: LocalDate): String? =
    this?.firstNotNullOfOrNull { it.organisasjonsledd?.finnJuridiskEnhetOrganisasjonsnummer(idag) }

/**
 * Velger organisasjonsnummeret til en gyldig juridisk enhet.
 * Foretrekker koblinger som er gyldige på [idag], men faller tilbake til
 * første kobling med organisasjonsnummer hvis ingen er eksplisitt gyldige.
 */
private fun List<InngaarIJuridiskEnhet>?.velgJuridiskEnhetOrganisasjonsnummer(idag: LocalDate): String? {
    val medOrgnummer = this.orEmpty().filterNot { it.organisasjonsnummer.isNullOrBlank() }
    return medOrgnummer.firstOrNull { it.erGyldig(idag) }?.organisasjonsnummer
        ?: medOrgnummer.firstOrNull()?.organisasjonsnummer
}

/**
 * En kobling er gyldig når [idag] er innenfor gyldighetsperioden.
 * Manglende fom/tom tolkes som åpen i den retningen.
 */
private fun InngaarIJuridiskEnhet.erGyldig(idag: LocalDate): Boolean {
    val fom = gyldighetsperiode?.fom
    val tom = gyldighetsperiode?.tom
    return (fom == null || !fom.isAfter(idag)) && (tom == null || !tom.isBefore(idag))
}

fun Organisasjon.toSimpleOrganisasjonDto() = SimpleOrganisasjonDto(
        orgnr = this.organisasjonsnummer,
        navn = this.navn?.sammensattnavn ?: "Ukjent navn",
    )
