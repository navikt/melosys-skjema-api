package no.nav.melosys.skjema.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("local-q1 | local-q2")
class BrukervarselProducerLocal : BrukervarselProducer {

    private val log = KotlinLogging.logger {}

    override fun sendBrukervarsel(brukervarselMelding: BrukervarselMelding) {
        log.info { "Lokal kjøring: Skipper Kafka-sending av brukervarsel" }
    }
}
