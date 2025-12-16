package no.nav.melosys.skjema

import no.nav.melosys.skjema.kafka.BrukervarselProducerProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication


@SpringBootApplication
@EnableConfigurationProperties(BrukervarselProducerProperties::class)
class MelosysSkjemaApiApplication

fun main(args: Array<String>) {
    runApplication<MelosysSkjemaApiApplication>(*args)
}
