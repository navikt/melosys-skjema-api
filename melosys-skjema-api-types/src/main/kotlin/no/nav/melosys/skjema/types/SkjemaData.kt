package no.nav.melosys.skjema.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidsgiverOgArbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UtsendtArbeidstakerArbeidstakersSkjemaDataDto::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL"),
    JsonSubTypes.Type(value = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVERS_DEL"),
    JsonSubTypes.Type(value = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL")
)
interface SkjemaData {
    val type: String
}
