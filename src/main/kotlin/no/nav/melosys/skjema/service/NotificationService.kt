package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.NotificationDto
import no.nav.melosys.skjema.dto.TekstDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger { }

@Service
class NotificationService(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Value("\${app.kafka.topic.notifications:aapen-brukervarsel-v1}") 
    private val notificationTopic: String
) {

    fun sendNotification(ident: String) {
        val varselId = UUID.randomUUID().toString()
        
        val notification = NotificationDto(
            varselId = varselId,
            ident = ident,
            tekster = mapOf(
                "nb" to TekstDto(
                    tekst = "Du har mottatt en melding fra Melosys"
                )
            )
        )
        
        try {
            kafkaTemplate.send(notificationTopic, varselId, notification)
            log.info { "Sendt notifikasjon med varselId: $varselId til ident: $ident" }
        } catch (e: Exception) {
            log.error(e) { "Feil ved sending av notifikasjon til ident: $ident" }
            throw e
        }
    }
}