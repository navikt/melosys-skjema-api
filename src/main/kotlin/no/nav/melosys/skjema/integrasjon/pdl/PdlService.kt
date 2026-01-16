package no.nav.melosys.skjema.integrasjon.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.pdl.exception.PersonVerifiseringException
import org.springframework.stereotype.Service
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Service
class PdlService(
    private val pdlConsumer: PdlConsumer
) {

    /**
     * Verifiserer at en person med gitt fødselsnummer og navn eksisterer,
     * og returnerer persondata hvis verifisering lykkes.
     *
     * @param fodselsnummer Fødselsnummer eller d-nummer
     * @param navn Navnet som skal matches
     * @return Pair med navn og fødselsdato
     * @throws PersonVerifiseringException hvis person ikke finnes eller navn ikke matcher
     * @throws IllegalArgumentException hvis person ikke har nødvendig data registrert
     */
    fun verifiserOgHentPerson(fodselsnummer: String, navn: String): Pair<String, LocalDate> {
        log.info { "Verifiserer og henter person fra PDL" }

        val person = try {
            pdlConsumer.hentPerson(fodselsnummer)
        } catch (e: IllegalArgumentException) {
            log.warn(e) { "Fant ikke person ved verifisering" }
            throw PersonVerifiseringException("Fødselsnummer og navn matcher ikke")
        }

        val personNavn = person.navn.firstOrNull()?.fulltNavn()
            ?: throw IllegalArgumentException("Person har ingen navn registrert i PDL")

        if (!personNavn.equals(navn, ignoreCase = true)) {
            log.warn { "Navn matcher ikke ved verifisering" }
            throw PersonVerifiseringException("Fødselsnummer og navn matcher ikke")
        }

        val fodselsdatoString = person.foedselsdato.firstOrNull()?.foedselsdato
            ?: throw IllegalArgumentException("Person har ingen fødselsdato registrert i PDL")

        val fodselsdato = LocalDate.parse(fodselsdatoString)

        return Pair(personNavn, fodselsdato)
    }
}
