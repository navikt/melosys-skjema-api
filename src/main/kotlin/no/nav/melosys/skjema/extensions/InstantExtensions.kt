package no.nav.melosys.skjema.extensions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val OSLO_ZONE = ZoneId.of("Europe/Oslo")

fun Instant.toOsloLocalDateTime(): LocalDateTime = this.atZone(OSLO_ZONE).toLocalDateTime()
