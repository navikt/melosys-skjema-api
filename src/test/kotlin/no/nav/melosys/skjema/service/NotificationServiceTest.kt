package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.ArbeidsgiverNotifikasjonConsumer
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class NotificationServiceTest : FunSpec({
    
    val mockArbeidsgiverNotifikasjonConsumer = mockk<ArbeidsgiverNotifikasjonConsumer>()
    val mockKafkaTemplate = mockk<KafkaTemplate<String, String>>()
    val brukervarselTopic = "test-brukervarsel"
    val producerCluster = "test-cluster"
    val producerNamespace = "test-namespace"
    val producerApp = "test-app"
    val service = NotificationService(mockArbeidsgiverNotifikasjonConsumer, mockKafkaTemplate, brukervarselTopic, producerCluster, producerNamespace, producerApp)
    
    test("sendNotification skal sende notifikasjon til Kafka med riktige data") {
        val ident = "12345678901"
        val notificationText = "Du har mottatt en melding fra Melosys"
        val topicSlot = slot<String>()
        val keySlot = slot<String>()
        val valueSlot = slot<String>()
        val mockFuture = mockk<CompletableFuture<SendResult<String, String>>>()
        
        every { 
            mockKafkaTemplate.send(capture(topicSlot), capture(keySlot), capture(valueSlot)) 
        } returns mockFuture
        
        service.sendNotificationToArbeidstaker(ident, notificationText)
        
        verify(exactly = 1) { mockKafkaTemplate.send(any<String>(), any<String>(), any<String>()) }
        
        topicSlot.captured shouldBe brukervarselTopic
        
        val varselJson = valueSlot.captured
        varselJson shouldContain ident
        varselJson shouldContain notificationText
        
        keySlot.captured.length shouldBe 36 // UUID format
    }
    
    test("sendNotification skal kaste exception når Kafka feiler") {
        val ident = "12345678901"
        val notificationText = "Du har mottatt en melding fra Melosys"
        
        every { 
            mockKafkaTemplate.send(any<String>(), any<String>(), any<String>()) 
        } throws RuntimeException("Kafka connection failed")
        
        try {
            service.sendNotificationToArbeidstaker(ident, notificationText)
            throw AssertionError("Expected exception was not thrown")
        } catch (e: RuntimeException) {
            e.message shouldBe "Kafka connection failed"
        }
    }
})