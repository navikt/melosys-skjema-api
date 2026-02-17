package no.nav.melosys.skjema.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.melosys.skjema.kafka.exception.SendBrukervarselFeilet
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
@Profile("!local-q1 & !local-q2")
class BrukervarselProducerKafka(
    private val brukervarselKafkaTemplate: KafkaTemplate<String, String>,
    private val brukervarselProducerProperties: BrukervarselProducerProperties
) : BrukervarselProducer {

    private val log = KotlinLogging.logger {}

    override fun sendBrukervarsel(brukervarselMelding: BrukervarselMelding) {
        val varselId = UUID.randomUUID().toString()

        runCatching {
            val varsel = buildVarsel(brukervarselMelding, varselId)
            val key = UUID.randomUUID().toString()

            log.info { "Sender brukervarsel med varselId=$varselId" }

            brukervarselKafkaTemplate.send(brukervarselProducerProperties.topic, key, varsel)
                .toCompletableFuture()
                .get()
        }.onFailure { exception ->
            throw SendBrukervarselFeilet(
                "Feil ved sending av brukervarsel",
                exception
            )
        }.onSuccess { result ->
            log.info {
                "Brukervarsel sendt OK med varselId=$varselId, partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()}"
            }
        }
    }

    private fun buildVarsel(brukervarselMelding: BrukervarselMelding, varselId: String): String {
        return VarselActionBuilder.opprett {
            produsent = Produsent(
                brukervarselProducerProperties.cluster,
                brukervarselProducerProperties.namespace,
                brukervarselProducerProperties.appname
            )
            type = Varseltype.Beskjed
            this.ident = brukervarselMelding.ident
            this.varselId = varselId
            sensitivitet = Sensitivitet.High
            brukervarselMelding.tekster.forEach { varseltekst ->
                tekster += Tekst(
                    spraakkode = varseltekst.språk.kode,
                    tekst = varseltekst.tekst,
                    default = varseltekst.default
                )
            }
            brukervarselMelding.link?.let { this.link = it }
            aktivFremTil = ZonedDateTime.now(ZoneId.of("Z")).plusDays(14)
        }
    }
}
