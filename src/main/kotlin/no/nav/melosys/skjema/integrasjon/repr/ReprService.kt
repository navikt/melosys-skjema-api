package no.nav.melosys.skjema.integrasjon.repr

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class ReprService(
    private val reprConsumer: ReprConsumer
) {

    /**
     * Henter liste over personer som innlogget bruker kan representere gjennom fullmakt for MED-området.
     * Bruker repr-api /api/v2/eksternbruker/fullmakt/kan-representere som returnerer fullmakter
     * hvor innlogget bruker er fullmektig.
     */
    @Cacheable(value = ["fullmakter"], key = "@reprService.getBrukerPid()")
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

    fun getBrukerPid(): String {
        return SubjectHandler.getInstance().getUserID()
    }
}
