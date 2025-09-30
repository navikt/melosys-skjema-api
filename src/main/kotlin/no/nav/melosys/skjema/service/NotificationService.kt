package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.ArbeidsgiverNotifikasjonConsumer
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.BeskjedRequest
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

private val log = KotlinLogging.logger { }

@Service
class NotificationService(
    @Autowired(required = false) private val arbeidsgiverNotifikasjonConsumer: ArbeidsgiverNotifikasjonConsumer?,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${kafka.topic.brukervarsel}")
    private val topic: String,
    @Value("\${kafka.producer.cluster}")
    private val cluster: String,
    @Value("\${kafka.producer.namespace}")
    private val namespace: String,
    @Value("\${kafka.producer.appname}")
    private val appName: String
) {

    fun sendNotificationToArbeidstaker(ident: String, notificationText: String) {
        val varselId = UUID.randomUUID().toString()

        val varsel = VarselActionBuilder.opprett {
            produsent = Produsent(cluster, namespace, appName)
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
            kafkaTemplate.send(topic, UUID.randomUUID().toString(), varsel)
            log.info { "Sendt notifikasjon med varselId: $varselId til ident: $ident" }
        } catch (e: Exception) {
            log.error(e) { "Feil ved sending av notifikasjon til ident: $ident" }
            throw e
        }
    }

    fun sendNotificationToArbeidsgiver(
        virksomhetsnummer: String,
        notificationText: String,
        lenke: String,
        eksternId: String? = null
    ): String {
        if (arbeidsgiverNotifikasjonConsumer == null) {
            throw RuntimeException("ArbeidsgiverNotifikasjonConsumer ikke konfigurert")
        }

        try {
            val beskjedId = arbeidsgiverNotifikasjonConsumer.opprettBeskjed(
                BeskjedRequest(
                    virksomhetsnummer = virksomhetsnummer,
                    tekst = notificationText,
                    lenke = lenke,
                    eksternId = eksternId ?: UUID.randomUUID().toString(),
                )
            )
            log.info { "Sendt arbeidsgiver notifikasjon med beskjed id: $beskjedId til virksomhet: $virksomhetsnummer" }
            return beskjedId
        } catch (e: Exception) {
            log.error(e) { "Feil ved sending av arbeidsgiver notifikasjon til virksomhet: $virksomhetsnummer" }
            throw e
        }
    }

}