package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging

import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

private val log = KotlinLogging.logger { }

@Service
class NotificationService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${app.kafka.topic.notifications:aapen-brukervarsel-v1}")
    private val notificationTopic: String
) {

    fun sendNotification(ident: String, notificationText: String) {
        val varselId = UUID.randomUUID().toString()

        val varsel = VarselActionBuilder.opprett {
            produsent = Produsent("test", "test", "test")
            type = Varseltype.Beskjed
            this.ident = ident
            this.varselId = varselId
            sensitivitet = Sensitivitet.High
            tekster += Tekst(
                    spraakkode = "nb",
                    tekst = notificationText,
                    default = true
            )
            aktivFremTil = ZonedDateTime.now(ZoneId.of("Z")).plusDays(14) //TODO finn ut hva vi skal bruke som deadline
        }
        
        try {
            kafkaTemplate.send(notificationTopic, varselId, varsel) //TODO vet ikke om det gir mening att kafkaId er det samme som varselId
            log.info { "Sendt notifikasjon med varselId: $varselId til ident: $ident" }
        } catch (e: Exception) {
            log.error(e) { "Feil ved sending av notifikasjon til ident: $ident" }
            throw e
        }
    }
}