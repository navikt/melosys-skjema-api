package no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto

data class NyBeskjedVariables(
    val eksternId: String,
    val virksomhetsnummer: String,
    val lenke: String,
    val tekst: String,
    val merkelapp: String, //Merkelapp bestems av produsenten og skal gjøre det tydelig for mottaker hvilken domene notifikasjonen er om. T ex sykemeldte eller tiltak.
    val ressursId: String,
)