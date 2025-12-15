package no.nav.melosys.skjema.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.skjema.kafka.SkjemaMottattMelding
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaConfig {

    @Bean
    fun skjemaMottattProducerFactory(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper
    ): ProducerFactory<String, SkjemaMottattMelding> {
        return DefaultKafkaProducerFactory(
            kafkaProperties.buildProducerProperties(null),
            StringSerializer(),
            JsonSerializer(objectMapper)
        )
    }

    @Bean
    fun skjemaMottattKafkaTemplate(
        skjemaMottattProducerFactory: ProducerFactory<String, SkjemaMottattMelding>,
        @Value("\${kafka.topic.skjema-mottak}") skjemaMottakTopic: String
    ): KafkaTemplate<String, SkjemaMottattMelding> {
        val kafkaTemplate = KafkaTemplate(skjemaMottattProducerFactory)
        kafkaTemplate.defaultTopic = skjemaMottakTopic
        return kafkaTemplate
    }
}
