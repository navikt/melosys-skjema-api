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

    /**
     * Blokkerende variant av sendSkjemaMottatt.
     * Venter på at meldingen er sendt før den returnerer.
     *
     * @throws SendSkjemaMottattMeldingFeilet hvis sending feiler
     */
    fun blokkerendeSendSkjemaMottatt(skjemaMottattMelding: SkjemaMottattMelding) =
        runCatching {
            sendSkjemaMottatt(skjemaMottattMelding).get()
        }.onFailure { exception ->
            throw SendSkjemaMottattMeldingFeilet(
                "Feil ved sending av skjema-mottatt melding for skjemaId=${skjemaMottattMelding.skjemaId}",
                exception
            )
        }


    private fun sendSkjemaMottatt(skjemaMottattMelding: SkjemaMottattMelding): CompletableFuture<SendResult<String, SkjemaMottattMelding>> {
        val key = skjemaMottattMelding.skjemaId.toString()

        log.info { "Sender skjema-mottatt melding for skjemaId=${skjemaMottattMelding.skjemaId}" }

        return kafkaTemplate.sendDefault(key, skjemaMottattMelding)
            .toCompletableFuture()
            .thenApply { result ->
                log.info {
                    "Kafka-melding sendt OK for skjemaId=${skjemaMottattMelding.skjemaId}, " +
                            "partition=${result.recordMetadata.partition()}, " +
                            "offset=${result.recordMetadata.offset()}"
                }
                result
            }
    }
}
