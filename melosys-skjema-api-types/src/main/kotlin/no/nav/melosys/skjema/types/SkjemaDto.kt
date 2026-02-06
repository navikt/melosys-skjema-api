package no.nav.melosys.skjema.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = UtsendtArbeidstakerSkjemaDto::class, name = "UTSENDT_ARBEIDSTAKER")
)
sealed interface SkjemaDto {
    val id: UUID
    val status: SkjemaStatus
    val type: SkjemaType
    val fnr: String
    val orgnr: String
    val metadata: SkjemaMetadata
    val data: SkjemaData
}
