package no.nav.melosys.skjema.integrasjon.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson
import no.nav.melosys.skjema.integrasjon.pdl.exception.PersonVerifiseringException
import no.nav.melosys.skjema.validators.felles.ErFodselsEllerDNummerValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class PdlService(
    private val pdlConsumer: PdlConsumer,
    @param:Value("\${validation.fodselsnummer.synthetic-mode}")
    private val validerSyntetiskFnr: Boolean
) {

    /**
     * Verifiserer at en person med gitt fødselsnummer og etternavn eksisterer,
     * og returnerer persondata hvis verifisering lykkes.
     *
     * @param fodselsnummer Fødselsnummer eller d-nummer
     * @param etternavn Etternavnet som skal matches
     * @return Pair med navn og fødselsdato
     * @throws PersonVerifiseringException hvis person ikke finnes eller etternavn ikke matcher
     * @throws IllegalArgumentException hvis person ikke har nødvendig data registrert
     */
    fun verifiserOgHentPerson(fodselsnummer: String, etternavn: String): Pair<String, LocalDate> {
        log.info { "Verifiserer og henter person fra PDL" }

        if (!ErFodselsEllerDNummerValidator.validerFnrOgDnrFormat(fodselsnummer, validerSyntetiskFnr)) {
            throw PersonVerifiseringException("Ugyldig format på fødselsnummer eller d-nummer")
        }

        val person = try {
            pdlConsumer.hentPerson(fodselsnummer)
        } catch (e: IllegalArgumentException) {
            log.warn(e) { "Fant ikke person ved verifisering" }
            throw PersonVerifiseringException("Fødselsnummer og etternavn matcher ikke")
        }

        // PDL kan ha flere parallelle navn (ulik master). Aksepterer match mot enhver aktuell verdi.
        val matcherEtternavn = person.aktuelleNavn()
            .any { it.etternavn.equals(etternavn, ignoreCase = true) }
        if (!matcherEtternavn) {
            log.warn { "Etternavn matcher ikke ved verifisering" }
            throw PersonVerifiseringException("Fødselsnummer og etternavn matcher ikke")
        }

        return Pair(person.hentFulltNavn(), person.hentFoedselsdato())
    }

    /**
     * Henter fullt navn fra PDL.
     * @throws IllegalArgumentException hvis person mangler navn registrert i PDL.
     */
    fun hentNavn(fodselsnummer: String): String =
        pdlConsumer.hentPerson(fodselsnummer).hentFulltNavn()

    // PDL kan returnere flere parallelle og historiske verdier per opplysning.
    // Vi filtrerer bort historiske og velger sist registrerte aktuelle verdi.

    private fun PdlPerson.aktuelleNavn() = navn.filter { it.metadata.erIkkeHistorisk() }

    private fun PdlPerson.hentFulltNavn(): String =
        aktuelleNavn().maxByOrNull { it.metadata.datoSistRegistrert() }
            ?.fulltNavn()
            ?: throw IllegalArgumentException("Person har ingen navn registrert i PDL")

    private fun PdlPerson.hentFoedselsdato(): LocalDate {
        val dato = foedselsdato.filter { it.metadata.erIkkeHistorisk() }
            .maxByOrNull { it.metadata.datoSistRegistrert() }
            ?.foedselsdato
            ?: throw IllegalArgumentException("Person har ingen fødselsdato registrert i PDL")
        return LocalDate.parse(dato)
    }
}
