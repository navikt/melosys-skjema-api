package no.nav.melosys.skjema.entity

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.skjema.dto.UtsendtArbeidstakerMetadata
import java.time.Instant
import java.util.UUID

/**
 * Domain-wrapper for Skjema av type UTSENDT_ARBEIDSTAKER.
 * Gir type-safe tilgang til metadata og skjemaspesifikk logikk.
 */
data class UtsendtArbeidstakerSkjema(
    val skjema: Skjema,
    private val objectMapper: ObjectMapper
) {
    init {
        require(skjema.type == "A1") {
            "Skjema må være av type A1, var: ${skjema.type}"
        }
    }

    // Type-safe metadata
    val metadata: UtsendtArbeidstakerMetadata
        get() = skjema.metadata?.let {
            objectMapper.treeToValue(it, UtsendtArbeidstakerMetadata::class.java)
        } ?: throw IllegalStateException("Metadata mangler for UtsendtArbeidstakerSkjema ${skjema.id}")

    // Delegerte properties fra Skjema
    val id: UUID
        get() = skjema.id ?: throw IllegalStateException("Skjema ID er null")

    val status: SkjemaStatus
        get() = skjema.status

    val fnr: String?
        get() = skjema.fnr

    val orgnr: String?
        get() = skjema.orgnr

    val opprettetDato: Instant
        get() = skjema.opprettetDato

    val endretDato: Instant
        get() = skjema.endretDato

    val opprettetAv: String
        get() = skjema.opprettetAv

    val endretAv: String
        get() = skjema.endretAv

    // Helper-metoder basert på metadata
    fun erOpprettetAvFullmektig(): Boolean = metadata.fullmektigFnr != null

    fun erRadgiverSoknad(): Boolean = metadata.representasjonstype == no.nav.melosys.skjema.dto.Representasjonstype.RADGIVER

    fun harFullmakt(): Boolean = metadata.harFullmakt

    fun getRadgiverfirmaOrgnr(): String? = metadata.radgiverfirma?.orgnr

    fun getFullmektigFnr(): String? = metadata.fullmektigFnr

    companion object {
        /**
         * Factory-metode for å opprette UtsendtArbeidstakerSkjema fra Skjema.
         * Returnerer null hvis skjemaet ikke er av riktig type.
         */
        fun fromSkjemaOrNull(skjema: Skjema, objectMapper: ObjectMapper): UtsendtArbeidstakerSkjema? {
            return if (skjema.type == "A1") {
                UtsendtArbeidstakerSkjema(skjema, objectMapper)
            } else {
                null
            }
        }
    }
}
