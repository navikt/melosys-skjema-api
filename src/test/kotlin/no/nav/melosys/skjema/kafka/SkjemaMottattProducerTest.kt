package no.nav.melosys.skjema.kafka

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.UUID
import java.util.concurrent.CompletableFuture

class SkjemaMottattProducerTest {

    private val kafkaTemplate: KafkaTemplate<String, SkjemaMottattMelding> = mockk()
    private val producer = SkjemaMottattProducer(kafkaTemplate)

    @Test
    fun `blokkerendeSendSkjemaMottatt skal sende melding til Kafka`() {
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId = skjemaId)
        val sendResult: SendResult<String, SkjemaMottattMelding> = mockk(relaxed = true)

        every { kafkaTemplate.sendDefault(any(), any()) } returns CompletableFuture.completedFuture(sendResult)

        producer.blokkerendeSendSkjemaMottatt(melding)

        verify { kafkaTemplate.sendDefault(skjemaId.toString(), melding) }
    }

    @Test
    fun `blokkerendeSendSkjemaMottatt skal kaste SendSkjemaMottattMeldingFeilet ved feil`() {
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId = skjemaId)

        val failedFuture = CompletableFuture<SendResult<String, SkjemaMottattMelding>>()
        failedFuture.completeExceptionally(RuntimeException("Kafka er nede"))

        every { kafkaTemplate.sendDefault(any(), any()) } returns failedFuture

        val exception = assertThrows(SendSkjemaMottattMeldingFeilet::class.java) {
            producer.blokkerendeSendSkjemaMottatt(melding)
        }

        exception.message.shouldNotBeNull()
        exception.message.run {
            this shouldContain skjemaId.toString()
        }
    }
}
