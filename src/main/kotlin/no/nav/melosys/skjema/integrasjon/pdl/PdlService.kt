package no.nav.melosys.skjema.integrasjon.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
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

        val personEtternavn = person.navn.firstOrNull()?.etternavn
        if (!personEtternavn.equals(etternavn, ignoreCase = true)) {
            log.warn { "Etternavn matcher ikke ved verifisering" }
            throw PersonVerifiseringException("Fødselsnummer og etternavn matcher ikke")
        }

        val navn = person.navn.firstOrNull()?.fulltNavn()
            ?: throw IllegalArgumentException("Person har ingen navn registrert i PDL")

        val fodselsdatoString = person.foedselsdato.firstOrNull()?.foedselsdato
            ?: throw IllegalArgumentException("Person har ingen fødselsdato registrert i PDL")

        val fodselsdato = LocalDate.parse(fodselsdatoString)

        return Pair(navn, fodselsdato)
    }
}
