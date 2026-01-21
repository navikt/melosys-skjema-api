package no.nav.melosys.skjema.extensions

import no.nav.melosys.skjema.dto.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/**
 * Extension functions for JsonMapper for type-sikker parsing av skjemadata.
 */

fun JsonMapper.toUtsendtArbeidstakerMetadata(node: JsonNode): UtsendtArbeidstakerMetadata =
    this.treeToValue(node, UtsendtArbeidstakerMetadata::class.java)

fun JsonMapper.toArbeidsgiversSkjemaDataDto(node: JsonNode): ArbeidsgiversSkjemaDataDto =
    this.treeToValue(node, ArbeidsgiversSkjemaDataDto::class.java)

fun JsonMapper.toArbeidstakersSkjemaDataDto(node: JsonNode): ArbeidstakersSkjemaDataDto =
    this.treeToValue(node, ArbeidstakersSkjemaDataDto::class.java)
