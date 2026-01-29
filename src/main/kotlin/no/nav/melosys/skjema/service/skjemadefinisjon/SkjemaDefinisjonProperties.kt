package no.nav.melosys.skjema.service.skjemadefinisjon

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Konfigurasjon for skjemadefinisjoner.
 *
 * Eksempel i application.yml:
 * ```yaml
 * skjemadefinisjon:
 *   aktive-versjoner:
 *     A1: "1"
 *     A2: "2"
 * ```
 */
@ConfigurationProperties(prefix = "skjemadefinisjon")
data class SkjemaDefinisjonProperties(
    /**
     * Map fra skjematype til aktiv versjon.
     * Nøkkel: Skjematype (f.eks. "A1")
     * Verdi: Versjonsnummer (f.eks. "1")
     */
    val aktiveVersjoner: Map<String, String> = mapOf("A1" to "1")
)
