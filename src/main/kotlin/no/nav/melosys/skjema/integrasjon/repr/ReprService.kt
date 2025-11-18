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

    @Cacheable(value = ["fullmakter"], key = "#root.target.getBrukerPid()")
    fun hentFullmakterForInnloggetBruker(): List<Fullmakt> {
        val pid = getBrukerPid()
        log.info { "Henter fullmakter for innlogget bruker (PID: $pid) fra repr-api" }

        return try {
            reprConsumer.hentKanRepresentere().fullmakter
                .filter { fullmakt ->
                    fullmakt.leserettigheter.contains(FullmaktOmrade.MEDLEMSKAP) ||
                    fullmakt.skriverettigheter.contains(FullmaktOmrade.MEDLEMSKAP)
                }
        } catch (e: Exception) {
            log.error(e) { "Feil ved henting av fullmakter fra repr-api" }
            throw RuntimeException("Kunne ikke hente fullmakter fra repr-api", e)
        }
    }

    fun hentKanRepresenteresAvForInnloggetBruker(): List<Fullmakt> {
        log.info { "Henter hvem som kan representere innlogget bruker fra repr-api" }

        return try {
            reprConsumer.hentKanRepresenteresAvForInnloggetBruker().fullmakter
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

        return try {
            val fullmakter = hentKanRepresenteresAvForInnloggetBruker()

            fullmakter.any { fullmakt ->
                fullmakt.fullmaktsgiver == fnr &&
                fullmakt.skriverettigheter.contains(FullmaktOmrade.MEDLEMSKAP)
            }
        } catch (e: Exception) {
            log.error(e) { "Feil ved validering av skriverettigheter" }
            false
        }
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

        return try {
            val fullmakter = hentKanRepresenteresAvForInnloggetBruker()

            fullmakter.any { fullmakt ->
                fullmakt.fullmaktsgiver == fnr &&
                fullmakt.leserettigheter.contains(FullmaktOmrade.MEDLEMSKAP)
            }
        } catch (e: Exception) {
            log.error(e) { "Feil ved validering av leserettigheter" }
            false
        }
    }

    fun getBrukerPid(): String {
        return SubjectHandler.getInstance().getUserID()
    }
}
