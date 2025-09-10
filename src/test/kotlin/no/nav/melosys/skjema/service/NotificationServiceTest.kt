package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class NotificationServiceTest : FunSpec({
    
    val mockKafkaTemplate = mockk<KafkaTemplate<String, Any>>()
    val notificationTopic = "test-notifications"
    val service = NotificationService(mockKafkaTemplate, notificationTopic)
    
    test("sendNotification skal sende notifikasjon til Kafka med riktige data") {
        val ident = "12345678901"
        val topicSlot = slot<String>()
        val keySlot = slot<String>()
        val valueSlot = slot<Map<String, Any>>()
        val mockFuture = mockk<CompletableFuture<SendResult<String, Any>>>()
        
        every { 
            mockKafkaTemplate.send(capture(topicSlot), capture(keySlot), capture(valueSlot)) 
        } returns mockFuture
        
        service.sendNotification(ident)
        
        verify(exactly = 1) { mockKafkaTemplate.send(any<String>(), any<String>(), any<Map<String, Any>>()) }
        
        topicSlot.captured shouldBe notificationTopic
        
        val notification = valueSlot.captured
        notification["type"] shouldBe "beskjed"
        notification["ident"] shouldBe ident
        notification["sensitivitet"] shouldBe "substantial"
        notification["aktiv"] shouldBe true
        
        val tekster = notification["tekster"] as Map<String, Any>
        val nbTekst = tekster["nb"] as Map<String, Any>
        nbTekst["tekst"] shouldBe "Du har mottatt en melding fra Melosys"
        nbTekst["default"] shouldBe true
        
        // Verify varselId is used as key and is present in the notification
        val varselId = keySlot.captured
        notification["varselId"] shouldBe varselId
    }
    
    test("sendNotification skal kaste exception når Kafka feiler") {
        val ident = "12345678901"
        
        every { 
            mockKafkaTemplate.send(any<String>(), any<String>(), any<Map<String, Any>>()) 
        } throws RuntimeException("Kafka connection failed")
        
        try {
            service.sendNotification(ident)
            throw AssertionError("Expected exception was not thrown")
        } catch (e: RuntimeException) {
            e.message shouldBe "Kafka connection failed"
        }
        
        verify(exactly = 1) { mockKafkaTemplate.send(any<String>(), any<String>(), any<Map<String, Any>>()) }
    }
})