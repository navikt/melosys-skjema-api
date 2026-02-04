package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.ArbeidsgiverNotifikasjonClient
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.BeskjedRequest
import no.nav.melosys.skjema.kafka.BrukervarselMelding
import no.nav.melosys.skjema.kafka.BrukervarselProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class NotificationService(
    @param:Autowired(required = false) private val arbeidsgiverNotifikasjonClient: ArbeidsgiverNotifikasjonClient?,
    private val brukervarselProducer: BrukervarselProducer
) {

    fun sendNotificationToArbeidstaker(ident: String, notificationText: String) {
        brukervarselProducer.sendBrukervarsel(BrukervarselMelding(ident, notificationText))
    }

    fun sendNotificationToArbeidsgiver(
        virksomhetsnummer: String,
        notificationText: String,
        lenke: String,
        eksternId: String? = null
    ): String {
        if (arbeidsgiverNotifikasjonClient == null) {
            throw RuntimeException("ArbeidsgiverNotifikasjonConsumer ikke konfigurert")
        }

        try {
            val beskjedId = arbeidsgiverNotifikasjonClient.opprettBeskjed(
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