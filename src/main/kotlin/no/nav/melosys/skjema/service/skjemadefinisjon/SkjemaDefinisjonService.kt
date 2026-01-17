package no.nav.melosys.skjema.service.skjemadefinisjon

import no.nav.melosys.skjema.dto.skjemadefinisjon.SkjemaDefinisjonDto
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for å hente skjemadefinisjoner.
 *
 * Leser JSON-filer fra classpath og cacher dem i minne.
 * Støtter versjonering og flere språk.
 */
@Service
class SkjemaDefinisjonService {
    private val jsonMapper: JsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .build()
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Cache for skjemadefinisjoner. Nøkkel: "type:versjon:språk" */
    private val cache = ConcurrentHashMap<String, SkjemaDefinisjonDto>()

    /** Aktive versjoner per skjematype */
    private val aktiveVersjoner: Map<String, String> = mapOf(
        "A1" to "1"
    )

    /**
     * Henter skjemadefinisjon for gitt type, versjon og språk.
     *
     * @param type Skjematype (f.eks. "A1")
     * @param versjon Versjon (valgfri - bruker aktiv versjon hvis null)
     * @param språk Språkkode (f.eks. "nb", "nn", "en")
     * @return Skjemadefinisjon
     * @throws IllegalArgumentException hvis type/versjon ikke finnes
     */
    fun hent(type: String, versjon: String?, språk: String): SkjemaDefinisjonDto {
        val faktiskVersjon = versjon ?: hentAktivVersjon(type)
        val cacheKey = "$type:$faktiskVersjon:$språk"

        return cache.getOrPut(cacheKey) {
            logger.debug("Laster skjemadefinisjon fra fil: type=$type, versjon=$faktiskVersjon, språk=$språk")
            lastFraFil(type, faktiskVersjon, språk)
        }
    }

    /**
     * Henter aktiv versjon for en skjematype.
     *
     * @param type Skjematype
     * @return Aktiv versjon
     * @throws IllegalArgumentException hvis type ikke er kjent
     */
    fun hentAktivVersjon(type: String): String {
        return aktiveVersjoner[type]
            ?: throw IllegalArgumentException("Ukjent skjematype: $type. Støttede typer: ${aktiveVersjoner.keys}")
    }

    /**
     * Sjekker om en skjematype er støttet.
     */
    fun erStøttetType(type: String): Boolean {
        return aktiveVersjoner.containsKey(type)
    }

    /**
     * Henter liste over alle støttede skjematyper.
     */
    fun hentStøttedeTyper(): Set<String> {
        return aktiveVersjoner.keys
    }

    /**
     * Laster skjemadefinisjon fra fil.
     * Prøver først ønsket språk, deretter fallback til norsk bokmål.
     */
    private fun lastFraFil(type: String, versjon: String, språk: String): SkjemaDefinisjonDto {
        val path = byggFilsti(type, versjon, språk)
        val resource = ClassPathResource(path)

        if (!resource.exists()) {
            // Fallback til norsk bokmål hvis ønsket språk ikke finnes
            if (språk != "nb") {
                logger.info("Språk '$språk' ikke funnet for $type v$versjon, bruker fallback til 'nb'")
                return lastFraFil(type, versjon, "nb")
            }
            throw IllegalArgumentException(
                "Skjemadefinisjon ikke funnet: $path. " +
                    "Sjekk at filen eksisterer i resources/skjema-definisjoner/"
            )
        }

        return try {
            resource.inputStream.use { stream ->
                jsonMapper.readValue(stream, SkjemaDefinisjonDto::class.java)
            }
        } catch (e: Exception) {
            logger.error("Feil ved lesing av skjemadefinisjon fra $path", e)
            throw IllegalStateException("Kunne ikke lese skjemadefinisjon fra $path: ${e.message}", e)
        }
    }

    /**
     * Bygger filsti for en skjemadefinisjon.
     */
    private fun byggFilsti(type: String, versjon: String, språk: String): String {
        return "skjema-definisjoner/$type/v$versjon/$språk.json"
    }

    /**
     * Tømmer cachen. Nyttig for testing.
     */
    fun tømCache() {
        cache.clear()
        logger.info("Skjemadefinisjon-cache tømt")
    }
}
