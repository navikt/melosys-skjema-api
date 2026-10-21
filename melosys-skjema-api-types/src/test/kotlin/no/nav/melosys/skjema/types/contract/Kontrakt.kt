package no.nav.melosys.skjema.types.contract

import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.shouldEqualJson
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

private val mapper = jsonMapper { addModule(kotlinModule()) }

/**
 * Golden-filene i `src/test/resources/contract` er kontrakten for data som er lagret i databaser
 * (skjema-api og melosys-api) og sendt til konsumenter. De skal ha alle felter populert, slik at
 * fjerning, navneendring, typeendring og nye påkrevde felter feiler her. Nye valgfrie felter er
 * bakoverkompatible og tillates (lenient sammenligning).
 *
 * Feiler testen etter en DTO-endring, er endringen breaking. Oppdater golden-filen kun hvis
 * endringen er tilsiktet og lagrede data/konsumenter er håndtert.
 */
internal fun <T : Any> verifiserKontrakt(fil: String, type: Class<T>): T {
    val golden = checkNotNull(type.getResource("/contract/$fil")) { "Fant ikke golden-fil $fil" }.readText()
    val verdi = mapper.readValue(golden, type)

    mapper.writeValueAsString(verdi) shouldEqualJson {
        fieldComparison = FieldComparison.Lenient
        golden
    }
    return verdi
}
