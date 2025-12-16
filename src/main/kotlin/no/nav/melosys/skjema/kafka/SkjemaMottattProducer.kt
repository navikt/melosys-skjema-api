package no.nav.melosys.skjema.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet

@Service
class SkjemaMottattProducer(
    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding>
) {
    private val log = KotlinLogging.logger {}

    fun sendSkjemaMottatt(melding: SkjemaMottattMelding): CompletableFuture<SendResult<String, SkjemaMottattMelding>> {
        val key = melding.skjemaId.toString()

        log.info { "Sender skjema-mottatt melding for skjemaId=${melding.skjemaId}" }

        return kafkaTemplate.sendDefault(key, melding)
            .toCompletableFuture()
            .thenApply { result ->
                log.info {
                    "Kafka-melding sendt OK for skjemaId=${melding.skjemaId}, " +
                    "partition=${result.recordMetadata.partition()}, " +
                    "offset=${result.recordMetadata.offset()}"
                }
                result
            }
            .exceptionally { exception ->
                log.error(exception) { "Feil ved sending av Kafka-melding for skjemaId=${melding.skjemaId}" }
                throw exception
            }
    }

    /**
     * Blokkerende variant av sendSkjemaMottatt.
     * Venter på at meldingen er sendt før den returnerer.
     *
     * @throws no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet hvis sending feiler
     */
    fun blokkerendeSendSkjemaMottatt(melding: SkjemaMottattMelding) {
        try {
            sendSkjemaMottatt(melding).get()
        } catch (e: Exception) {
            throw SendSkjemaMottattMeldingFeilet(
                "Feil ved sending av skjema-mottatt melding for skjemaId=${melding.skjemaId}",
                e
            )
        }
    }
}
