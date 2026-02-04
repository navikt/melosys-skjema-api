package no.nav.melosys.skjema.service.skjemadefinisjon

import no.nav.melosys.skjema.types.SkjemaType
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Konfigurasjon for skjemadefinisjoner.
 *
 * Eksempel i application.yml:
 * ```yaml
 * skjemadefinisjon:
 *   aktive-versjoner:
 *     UTSENDT_ARBEIDSTAKER: "1"
 * ```
 */
@ConfigurationProperties(prefix = "skjemadefinisjon")
data class SkjemaDefinisjonProperties(
    /**
     * Map fra skjematype til aktiv versjon.
     * Nøkkel: Skjematype (f.eks. "A1")
     * Verdi: Versjonsnummer (f.eks. "1")
     */
    val aktiveVersjoner: Map<SkjemaType, String> = mapOf(SkjemaType.UTSENDT_ARBEIDSTAKER to "1")
)
