package no.nav.melosys.skjema.extensions

import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/**
 * Extension functions for JsonMapper for type-sikker parsing av skjemadata.
 */

fun JsonMapper.parseArbeidsgiversSkjemaDataDto(node: JsonNode): UtsendtArbeidstakerArbeidsgiversSkjemaDataDto =
    this.treeToValue(node, UtsendtArbeidstakerArbeidsgiversSkjemaDataDto::class.java)

fun JsonMapper.parseArbeidstakersSkjemaDataDto(node: JsonNode): UtsendtArbeidstakerArbeidstakersSkjemaDataDto =
    this.treeToValue(node, UtsendtArbeidstakerArbeidstakersSkjemaDataDto::class.java)
