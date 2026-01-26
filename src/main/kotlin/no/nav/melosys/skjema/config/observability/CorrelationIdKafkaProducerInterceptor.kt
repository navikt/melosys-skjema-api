package no.nav.melosys.skjema.config.observability

import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

/**
 * Kafka ProducerInterceptor som legger til correlation-id header på utgående meldinger.
 * Henter correlation ID fra MDC hvis tilgjengelig, eller genererer en ny.
 */
class CorrelationIdKafkaProducerInterceptor : ProducerInterceptor<Any, Any> {

    override fun onSend(record: ProducerRecord<Any, Any>): ProducerRecord<Any, Any> {
        record.headers().add(MDCOperations.CORRELATION_ID, MDCOperations.getCorrelationId().toByteArray())
        return record
    }

    override fun onAcknowledgement(metadata: RecordMetadata?, exception: Exception?) {
        // Ingen handling nødvendig
    }

    override fun configure(configs: MutableMap<String, *>?) {
        // Ingen konfigurasjon nødvendig
    }

    override fun close() {
        // Ingen ressurser å frigjøre
    }
}
