package no.nav.melosys.skjema.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.skjema.ApiTestBase
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration

@EmbeddedKafka(
    partitions = 1,
    topics = ["test-notifications"]
)
@DirtiesContext
class NotificationServiceIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var notificationService: NotificationService
    
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker
    
    @TestConfiguration
    class TestConfig {
        @Bean
        fun testConsumerFactory(embeddedKafkaBroker: EmbeddedKafkaBroker): ConsumerFactory<String, Map<String, Any>> {
            val consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker)
            consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
            consumerProps[JsonDeserializer.TRUSTED_PACKAGES] = "*"
            consumerProps[JsonDeserializer.VALUE_DEFAULT_TYPE] = "java.util.Map"
            
            return DefaultKafkaConsumerFactory(consumerProps)
        }
    }

    @Test
    fun `sendNotification skal sende melding til Kafka topic`() {
        val ident = "12345678901"
        
        // Create test consumer
        val consumerFactory = testConsumerFactory(embeddedKafkaBroker)
        val consumer = consumerFactory.createConsumer() as KafkaConsumer<String, Map<String, Any>>
        consumer.subscribe(listOf("test-notifications"))
        
        // Send notification
        notificationService.sendNotification(ident)
        
        // Consume message
        val records = consumer.poll(Duration.ofSeconds(10))
        consumer.close()
        
        // Verify message was sent
        val recordsList = records.toList()
        recordsList.size shouldBe 1
        
        val record = recordsList.first()
        val notification = record.value()
        
        // Verify message content
        notification["type"] shouldBe "beskjed"
        notification["ident"] shouldBe ident
        notification["sensitivitet"] shouldBe "substantial"
        notification["aktiv"] shouldBe true
        notification["varselId"] shouldNotBe null
        
        // Verify key matches varselId
        record.key() shouldBe notification["varselId"]
        
        val tekster = notification["tekster"] as Map<String, Any>
        val nbTekst = tekster["nb"] as Map<String, Any>
        nbTekst["tekst"] shouldBe "Du har mottatt en melding fra Melosys"
        nbTekst["default"] shouldBe true
    }
    
    private fun testConsumerFactory(embeddedKafkaBroker: EmbeddedKafkaBroker): ConsumerFactory<String, Map<String, Any>> {
        val consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker)
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        consumerProps[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        consumerProps[JsonDeserializer.VALUE_DEFAULT_TYPE] = "java.util.Map"
        
        return DefaultKafkaConsumerFactory(consumerProps)
    }
}