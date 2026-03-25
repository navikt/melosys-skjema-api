package no.nav.melosys.skjema.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime
import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = UtsendtArbeidstakerSkjemaDto::class, name = "UTSENDT_ARBEIDSTAKER")
)
interface SkjemaDto {
    val id: UUID
    val status: SkjemaStatus
    val type: SkjemaType
    val fnr: String
    val orgnr: String
    val opprettetDato: LocalDateTime
    val endretDato: LocalDateTime
    val metadata: SkjemaMetadata
    val data: SkjemaData
}
