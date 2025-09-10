package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class NotificationDto(
    val type: String = "beskjed",
    val varselId: String,
    val ident: String,
    val tekster: Map<String, TekstDto>,
    val sensitivitet: String = "substantial",
    val aktiv: Boolean = true,
    val forstBehandlet: LocalDateTime = LocalDateTime.now()
)

data class TekstDto(
    val tekst: String,
    @JsonProperty("default")
    val isDefault: Boolean = true
)