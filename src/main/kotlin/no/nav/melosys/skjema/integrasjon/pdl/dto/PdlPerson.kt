package no.nav.melosys.skjema.integrasjon.pdl.dto

import java.time.LocalDate

data class PdlPerson(
    val navn: List<PdlNavn>,
    val foedselsdato: List<PdlFoedselsdato>
) {
    /**
     * PDL kan returnere flere parallelle navn, f.eks. fra Folkeregisteret og NAV.
     * Historiske navn filtreres bort her. Blant aktuelle verdier
     * velger vi navnet med siste ikke-opphørte registrering, som i melosys-api.
     */
    fun hentNavn(): PdlNavn = gjeldendeNavn()
        .maxByOrNull { it.metadata.datoSistRegistrert() }
        ?: throw IllegalArgumentException("Person har ingen gjeldende navn registrert i PDL")

    fun hentFulltNavn(): String = hentNavn().fulltNavn()

    /**
     * Returnerer kun gjeldende (ikke-historiske) navn. For alle navn inkludert historikk, bruk [navn] direkte.
     */
    fun gjeldendeNavn(): List<PdlNavn> = navn.filter { it.metadata.erGjeldende() }

    fun hentFoedselsdato(): LocalDate {
        val dato = foedselsdato
            .maxByOrNull { it.metadata.datoSistRegistrert() }
            ?.foedselsdato
            ?: throw IllegalArgumentException("Person har ingen fødselsdato registrert i PDL")
        return LocalDate.parse(dato)
    }
}
