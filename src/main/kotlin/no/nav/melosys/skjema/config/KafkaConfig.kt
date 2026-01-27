package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.config.observability.CorrelationIdKafkaProducerInterceptor
import tools.jackson.databind.json.JsonMapper
import no.nav.melosys.skjema.kafka.SkjemaMottattMelding
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
class KafkaConfig {

    @Bean
    fun stringProducerFactory(kafkaProperties: KafkaProperties): ProducerFactory<String, String> {
        val props = kafkaProperties.buildProducerProperties().withCorrelationId()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun brukervarselKafkaTemplate(stringProducerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(stringProducerFactory)
    }

    @Bean
    fun skjemaMottattProducerFactory(
        kafkaProperties: KafkaProperties,
        jsonMapper: JsonMapper
    ): ProducerFactory<String, SkjemaMottattMelding> {
        val props = kafkaProperties.buildProducerProperties().withCorrelationId()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        val factory = DefaultKafkaProducerFactory<String, SkjemaMottattMelding>(props)
        factory.valueSerializer = JacksonJsonSerializer(jsonMapper)
        return factory
    }

    @Bean
    fun skjemaMottattKafkaTemplate(
        skjemaMottattProducerFactory: ProducerFactory<String, SkjemaMottattMelding>,
        @Value("\${kafka.topic.skjema-mottak}") skjemaMottakTopic: String
    ): KafkaTemplate<String, SkjemaMottattMelding> {
        val kafkaTemplate = KafkaTemplate(skjemaMottattProducerFactory)
        kafkaTemplate.setDefaultTopic(skjemaMottakTopic)
        return kafkaTemplate
    }
}

private fun Map<String, Any>.withCorrelationId(): MutableMap<String, Any> =
    toMutableMap().apply {
        this[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] = listOf(CorrelationIdKafkaProducerInterceptor::class.java.name)
    }