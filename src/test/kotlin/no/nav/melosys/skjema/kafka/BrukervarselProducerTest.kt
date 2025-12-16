package no.nav.melosys.skjema.kafka

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.kafka.exception.SendBrukervarselFeilet
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class BrukervarselProducerTest {

    private val kafkaTemplate: KafkaTemplate<String, String> = mockk()
    private val properties = BrukervarselProducerProperties(
        topic = "test-topic",
        cluster = "test-cluster",
        namespace = "test-namespace",
        appname = "test-app"
    )
    private val producer = BrukervarselProducer(kafkaTemplate, properties)

    @Test
    fun `sendBrukervarsel skal sende melding til Kafka`() {
        val ident = "12345678901"
        val notificationText = "Test notifikasjon"
        val melding = BrukervarselMelding(ident = ident, notificationText = notificationText)
        val sendResult: SendResult<String, String> = mockk(relaxed = true)

        every { kafkaTemplate.send(any<String>(), any(), any()) } returns CompletableFuture.completedFuture(sendResult)

        producer.sendBrukervarsel(melding)

        verify { kafkaTemplate.send("test-topic", any(), any()) }
    }

    @Test
    fun `sendBrukervarsel skal kaste SendBrukervarselFeilet ved feil`() {
        val ident = "12345678901"
        val notificationText = "Test notifikasjon"
        val melding = BrukervarselMelding(ident = ident, notificationText = notificationText)

        val failedFuture = CompletableFuture<SendResult<String, String>>()
        failedFuture.completeExceptionally(RuntimeException("Kafka er nede"))

        every { kafkaTemplate.send(any<String>(), any(), any()) } returns failedFuture

        val exception = assertThrows(SendBrukervarselFeilet::class.java) {
            producer.sendBrukervarsel(melding)
        }

        exception.message.shouldNotBeNull()
        exception.message.run {
            this shouldContain ident
        }
    }
}
