package no.nav.melosys.skjema.service.skjemadefinisjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.skjemadefinisjon.SkjemaDefinisjon
import no.nav.melosys.skjema.dto.skjemadefinisjon.flerspraklig.FlersprakligSkjemaDefinisjonDto
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Service for å hente skjemadefinisjoner.
 *
 * Leser flerspråklige JSON-filer fra classpath og transformerer til
 * enkeltspråklige DTOer basert på språkparameter.
 * Støtter versjonering og flere språk.
 *
 * Aktive versjoner konfigureres via application.yml:
 * ```yaml
 * skjemadefinisjon:
 *   aktive-versjoner:
 *     A1: "1"
 * ```
 *
 * Cache-invalidering: Cachen tømmes ikke automatisk. Ved oppdatering av
 * JSON-filer i produksjon kreves restart av applikasjonen.
 */
@Service
@EnableConfigurationProperties(SkjemaDefinisjonProperties::class)
class SkjemaDefinisjonService(
    private val properties: SkjemaDefinisjonProperties,
    private val jsonMapper: JsonMapper
) {

    /** Cache for enkeltspråklige skjemadefinisjoner. Nøkkel: "type:versjon:språk" */
    private val cache = ConcurrentHashMap<String, SkjemaDefinisjon>()

    /** Cache for flerspråklige skjemadefinisjoner. Nøkkel: "type:versjon" */
    private val flersprakligCache = ConcurrentHashMap<String, FlersprakligSkjemaDefinisjonDto>()

    /**
     * Henter skjemadefinisjon for gitt type, versjon og språk.
     *
     * @param type Skjematype (f.eks. "A1")
     * @param versjon Versjon (valgfri - bruker aktiv versjon hvis null)
     * @param språk Språk enum
     * @return Skjemadefinisjon
     * @throws IllegalArgumentException hvis type/versjon ikke finnes
     */
    fun hent(type: String, versjon: String?, språk: Språk): SkjemaDefinisjon {
        val faktiskVersjon = versjon ?: hentAktivVersjon(type)
        val cacheKey = "$type:$faktiskVersjon:${språk.kode}"

        return cache.getOrPut(cacheKey) {
            log.debug { "Henter skjemadefinisjon: type=$type, versjon=$faktiskVersjon, språk=${språk.kode}" }
            val flerspraklig = hentFlerspraklig(type, faktiskVersjon)
            flerspraklig.tilSkjemaDefinisjonDto(språk)
        }
    }

    /**
     * Henter flerspråklig skjemadefinisjon fra cache eller fil.
     */
    private fun hentFlerspraklig(type: String, versjon: String): FlersprakligSkjemaDefinisjonDto {
        val cacheKey = "$type:$versjon"
        return flersprakligCache.getOrPut(cacheKey) {
            log.debug { "Laster flerspråklig skjemadefinisjon fra fil: type=$type, versjon=$versjon" }
            lastFlersprakligFraFil(type, versjon)
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
        return properties.aktiveVersjoner[type]
            ?: throw IllegalArgumentException("Ukjent skjematype: $type. Støttede typer: ${properties.aktiveVersjoner.keys}")
    }

    /**
     * Sjekker om en skjematype er støttet.
     */
    fun erStøttetType(type: String): Boolean {
        return properties.aktiveVersjoner.containsKey(type)
    }

    /**
     * Henter liste over alle støttede skjematyper.
     */
    fun hentStøttedeTyper(): Set<String> {
        return properties.aktiveVersjoner.keys
    }

    /**
     * Laster flerspråklig skjemadefinisjon fra fil.
     */
    private fun lastFlersprakligFraFil(type: String, versjon: String): FlersprakligSkjemaDefinisjonDto {
        val path = byggFilsti(type, versjon)
        val resource = ClassPathResource(path)

        if (!resource.exists()) {
            throw IllegalArgumentException(
                "Skjemadefinisjon ikke funnet: $path. " +
                    "Sjekk at filen eksisterer i resources/skjema-definisjoner/"
            )
        }

        return try {
            resource.inputStream.use { stream ->
                jsonMapper.readValue(stream, FlersprakligSkjemaDefinisjonDto::class.java)
            }
        } catch (e: Exception) {
            log.error(e) { "Feil ved lesing av skjemadefinisjon fra $path" }
            throw IllegalStateException("Kunne ikke lese skjemadefinisjon fra $path: ${e.message}", e)
        }
    }

    /**
     * Bygger filsti for en flerspråklig skjemadefinisjon.
     */
    private fun byggFilsti(type: String, versjon: String): String {
        return "skjema-definisjoner/$type/v$versjon/definisjon.json"
    }

    /**
     * Tømmer begge cachene. Nyttig for testing.
     */
    fun tømCache() {
        cache.clear()
        flersprakligCache.clear()
        log.info { "Skjemadefinisjon-cacher tømt" }
    }
}
