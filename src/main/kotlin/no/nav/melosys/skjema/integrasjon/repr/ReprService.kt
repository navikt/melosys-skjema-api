package no.nav.melosys.skjema.integrasjon.repr

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import no.nav.melosys.skjema.controller.dto.PersonMedFullmaktDto
import no.nav.melosys.skjema.integrasjon.pdl.PdlConsumer
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class ReprService(
    private val reprConsumer: ReprConsumer,
    private val pdlConsumer: PdlConsumer
) {

    /**
     * Henter liste over personer som innlogget bruker kan representere gjennom fullmakt for MED-området.
     * Bruker repr-api /api/v2/eksternbruker/fullmakt/kan-representere som returnerer fullmakter
     * hvor innlogget bruker er fullmektig.
     */
    fun hentKanRepresentere(): List<Fullmakt> {
        log.info { "Henter fullmakter for innlogget bruker fra repr-api" }

        return try {
            reprConsumer.hentKanRepresentere()
                .filter { fullmakt ->
                    fullmakt.leserettigheter.contains(FullmaktOmrade.MEDLEMSKAP) ||
                            fullmakt.skriverettigheter.contains(FullmaktOmrade.MEDLEMSKAP)
                }
        } catch (e: Exception) {
            log.error(e) { "Feil ved henting av fullmakter fra repr-api" }
            throw RuntimeException("Kunne ikke hente fullmakter fra repr-api", e)
        }
    }

    /**
     * Validerer at innlogget bruker har fullmakt med skriverettigheter for MED (Medlemskap) fra gitt fødselsnummer.
     *
     * Skriverettigheter betyr:
     * - Kan søke på ytelser, klage på vedtak, og sende og endre dokumentasjon på vegne av fullmaktsgiver
     * - Kan også lese dokumenter, saker og meldinger
     * - Kan snakke med Nav på vegne av fullmaktsgiver
     *
     * @param fnr Fødselsnummer til fullmaktsgiver (personen som gir fullmakten)
     * @return true hvis innlogget bruker har fullmakt med skriverettigheter for MED fra gitt fnr, false ellers
     */
    fun harSkriverettigheterForMedlemskap(fnr: String): Boolean {
        log.info { "Validerer skriverettigheter for medlemskap" }
        return harRettigheter(fnr, RettighetsType.SKRIVE)
    }

    /**
     * Validerer at innlogget bruker har fullmakt med leserettigheter for MED (Medlemskap) fra gitt fødselsnummer.
     *
     * Leserettigheter betyr:
     * - Kan se dokumenter, saker, meldinger og annen informasjon via nav.no
     * - Kan snakke med Nav på vegne av fullmaktsgiver
     *
     * @param fnr Fødselsnummer til fullmaktsgiver (personen som gir fullmakten)
     * @return true hvis innlogget bruker har fullmakt med leserettigheter for MED fra gitt fnr, false ellers
     */
    fun harLeserettigheterForMedlemskap(fnr: String): Boolean {
        log.info { "Validerer leserettigheter for medlemskap" }
        return harRettigheter(fnr, RettighetsType.LESE)
    }

    /**
     * Validerer om innlogget bruker har mottatt fullmakt fra gitt fnr med angitte rettigheter.
     *
     * Logikken: Vi henter fullmakter hvor innlogget bruker er fullmektig (kan representere andre).
     * Hvis fullmaktsgiver (den som gir fullmakten) matcher angitt fnr, så har innlogget bruker
     * fullmakt til å handle på vegne av denne personen.
     */
    private fun harRettigheter(fnr: String, rettighetsType: RettighetsType): Boolean {
        return try {
            val fullmakter = hentKanRepresentere()

            fullmakter.any { fullmakt ->
                fullmakt.fullmaktsgiver == fnr && when (rettighetsType) {
                    RettighetsType.LESE -> fullmakt.leserettigheter.contains(FullmaktOmrade.MEDLEMSKAP)
                    RettighetsType.SKRIVE -> fullmakt.skriverettigheter.contains(FullmaktOmrade.MEDLEMSKAP)
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Feil ved validering av ${rettighetsType.name.lowercase()}rettigheter" }
            false
        }
    }

    private enum class RettighetsType {
        LESE,
        SKRIVE
    }

    /**
     * Henter personer som innlogget bruker har fullmakt fra, beriket med navn og fødselsdato fra PDL.
     * Bruker bulk-query til PDL for å hente alle personer på én gang.
     *
     * @return Liste med PersonMedFullmaktDto (kun personer som finnes i PDL)
     */
    fun hentPersonerMedFullmakt(): List<PersonMedFullmaktDto> {
        log.debug { "Henter personer med fullmakt for innlogget bruker" }

        val fullmakter = hentKanRepresentere().filter {
            // skriverettigheter med MED
            it.skriverettigheter.contains(FullmaktOmrade.MEDLEMSKAP)
        }

        if (fullmakter.isEmpty()) {
            log.debug { "Ingen fullmakter funnet for bruker" }
            return emptyList()
        }

        // Hent alle unike fnr fra fullmaktsgivere
        val fnrListe = fullmakter
            .map { it.fullmaktsgiver }
            .distinct()

        // Hent alle personer fra PDL i én bulk-query
        val personerMap = pdlConsumer.hentPersonerBolk(fnrListe)

        // Map til PersonMedFullmaktDto, kun for personer vi fikk fra PDL
        return fullmakter
            .mapNotNull { mapTilPersonMedFullmakt(it, personerMap) }
            .also { result ->
                log.debug { "Returnerer ${result.size} personer med fullmakt (${fullmakter.size - result.size} personer ikke funnet i PDL)" }
            }
    }

    /**
     * Mapper en fullmakt til PersonMedFullmaktDto ved å berike med persondata fra PDL.
     * Returnerer null hvis person ikke finnes i PDL eller har ugyldig data.
     */
    private fun mapTilPersonMedFullmakt(
        fullmakt: Fullmakt,
        personerMap: Map<String, no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson>
    ): PersonMedFullmaktDto? {
        val person = personerMap[fullmakt.fullmaktsgiver] ?: return null
        val navn = person.navn.firstOrNull()?.fulltNavn() ?: return null
        val fodselsdatoString = person.foedselsdato.firstOrNull()?.foedselsdato ?: return null

        val fodselsdato = try {
            LocalDate.parse(fodselsdatoString)
        } catch (e: Exception) {
            log.warn { "Ugyldig fødselsdato for fullmaktsgiver" }
            return null
        }

        return PersonMedFullmaktDto(
            fnr = fullmakt.fullmaktsgiver,
            navn = navn,
            fodselsdato = fodselsdato
        )
    }

    fun getBrukerPid(): String {
        return SubjectHandler.getInstance().getUserID()
    }
}
