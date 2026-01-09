package no.nav.melosys.skjema.config

import tools.jackson.databind.json.JsonMapper
import no.nav.melosys.skjema.kafka.SkjemaMottattMelding
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
        return DefaultKafkaProducerFactory(
            kafkaProperties.buildProducerProperties(),
            StringSerializer(),
            StringSerializer()
        )
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
        return DefaultKafkaProducerFactory(
            kafkaProperties.buildProducerProperties(),
            StringSerializer(),
            JacksonJsonSerializer(jsonMapper)
        )
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
