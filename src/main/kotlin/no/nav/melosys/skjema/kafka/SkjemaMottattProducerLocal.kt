package no.nav.melosys.skjema.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("local-q1 | local-q2")
class SkjemaMottattProducerLocal : SkjemaMottattProducer {

    private val log = KotlinLogging.logger {}

    override fun blokkerendeSendSkjemaMottatt(skjemaMottattMelding: SkjemaMottattMelding): Result<*> {
        log.info { "Lokal kjøring: Skipper Kafka-sending av skjema-mottatt melding for skjemaId=${skjemaMottattMelding.skjemaId}" }
        return Result.success(Unit)
    }
}
