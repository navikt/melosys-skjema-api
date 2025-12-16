package no.nav.melosys.skjema.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka.brukervarsel")
data class BrukervarselProducerProperties(
    val topic: String,
    val cluster: String,
    val namespace: String,
    val appname: String
)
