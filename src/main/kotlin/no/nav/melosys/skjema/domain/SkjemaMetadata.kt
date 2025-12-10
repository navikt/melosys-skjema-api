package no.nav.melosys.skjema.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Wrapper for metadata på A1-skjemaer.
 *
 * Bruker @JsonIgnoreProperties for å bevare eventuelle eksisterende felter
 * vi ikke kjenner til, og for å være bakoverkompatibel.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SkjemaMetadata(
    val innsending: InnsendingMetadata? = null
)
