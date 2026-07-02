package no.nav.melosys.skjema.kafka

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import no.nav.melosys.skjema.kafka.exception.SendBrukervarselFeilet
import no.nav.melosys.skjema.types.common.Språk
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult

class BrukervarselProducerTest {

    private val kafkaTemplate: KafkaTemplate<String, String> = mockk()
    private val properties = BrukervarselProducerProperties(
        topic = "test-topic",
        cluster = "test-cluster",
        namespace = "test-namespace",
        appname = "test-app"
    )
    private val producer = BrukervarselProducerKafka(kafkaTemplate, properties)

    @Test
    fun `sendBrukervarsel skal sende melding til Kafka`() {
        val ident = "12345678901"
        val melding = BrukervarselMelding(
            ident = ident,
            tekster = listOf(Varseltekst(Språk.NORSK_BOKMAL, "Test notifikasjon", true))
        )
        val sendResult: SendResult<String, String> = mockk(relaxed = true)

        every { kafkaTemplate.send(any<String>(), any(), any()) } returns CompletableFuture.completedFuture(sendResult)

        producer.sendBrukervarsel(melding)

        verify { kafkaTemplate.send("test-topic", any(), any()) }
    }

    @Test
    fun `sendBrukervarsel skal aktivere SMS-varsling naar sms er true`() {
        val melding = BrukervarselMelding(
            ident = "12345678901",
            tekster = listOf(Varseltekst(Språk.NORSK_BOKMAL, "Test notifikasjon", true)),
            sms = true
        )
        val sendResult: SendResult<String, String> = mockk(relaxed = true)
        val varselSlot = slot<String>()
        every { kafkaTemplate.send(any<String>(), any(), capture(varselSlot)) } returns CompletableFuture.completedFuture(sendResult)

        producer.sendBrukervarsel(melding)

        varselSlot.captured shouldContain "SMS"
    }

    @Test
    fun `sendBrukervarsel skal ikke aktivere SMS-varsling naar sms er false`() {
        val melding = BrukervarselMelding(
            ident = "12345678901",
            tekster = listOf(Varseltekst(Språk.NORSK_BOKMAL, "Test notifikasjon", true)),
            sms = false
        )
        val sendResult: SendResult<String, String> = mockk(relaxed = true)
        val varselSlot = slot<String>()
        every { kafkaTemplate.send(any<String>(), any(), capture(varselSlot)) } returns CompletableFuture.completedFuture(sendResult)

        producer.sendBrukervarsel(melding)

        varselSlot.captured shouldNotContain "SMS"
    }

    @Test
    fun `sendBrukervarsel skal kaste SendBrukervarselFeilet ved feil`() {
        val ident = "12345678901"
        val melding = BrukervarselMelding(
            ident = ident,
            tekster = listOf(Varseltekst(Språk.NORSK_BOKMAL, "Test notifikasjon", true))
        )

        val failedFuture = CompletableFuture<SendResult<String, String>>()
        failedFuture.completeExceptionally(RuntimeException("Kafka er nede"))

        every { kafkaTemplate.send(any<String>(), any(), any()) } returns failedFuture

        val exception = assertThrows(SendBrukervarselFeilet::class.java) {
            producer.sendBrukervarsel(melding)
        }

        exception.message.shouldNotBeNull()
        exception.message.run {
            this shouldContain "brukervarsel"
        }
    }
}

