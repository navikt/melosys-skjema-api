package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.kafka.BrukervarselMelding
import no.nav.melosys.skjema.kafka.BrukervarselProducer
import no.nav.melosys.skjema.kafka.Varseltekst
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.AnnenPersonMetadata
import no.nav.melosys.skjema.types.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.RadgiverMetadata
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ArbeidstakerVarslingService(
    private val brukervarselProducer: BrukervarselProducer,
    private val skjemaRepository: SkjemaRepository,
    @param:Value("\${varsling.arbeidstaker.skjema-lenke}") private val skjemaLenke: String
) {

    /**
     * Vurderer og sender varsel til arbeidstaker basert på representasjonstype
     * etter at arbeidsgiver/rådgiver har sendt inn sin del.
     *
     * Kaster exception ved feil slik at kaller kan avgjøre om varselet skal markeres som sendt.
     */
    fun varsleArbeidstakerHvisAktuelt(skjemaId: UUID) {
        val skjema = skjemaRepository.findById(skjemaId).orElse(null)
        if (skjema == null) {
            log.warn { "Fant ikke skjema $skjemaId for varsling" }
            return
        }

        val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata
        if (metadata == null) {
            log.debug { "Skjema $skjemaId har ikke UtsendtArbeidstakerMetadata, hopper over varsling" }
            return
        }

        when (metadata) {
            is ArbeidsgiverMetadata, is RadgiverMetadata -> {
                varsleUtenFullmakt(skjema.fnr, skjema.orgnr, metadata)
            }
            is ArbeidsgiverMedFullmaktMetadata, is RadgiverMedFullmaktMetadata -> {
                varsleOmFullmaktsInnsending(skjema.fnr, skjema.orgnr, metadata)
            }
            is DegSelvMetadata, is AnnenPersonMetadata -> {
                log.debug { "Ingen varsling for representasjonstype ${metadata.representasjonstype} (skjema $skjemaId)" }
            }
        }
    }

    private fun varsleUtenFullmakt(fnr: String, orgnr: String, metadata: UtsendtArbeidstakerMetadata) {
        if (harEksisterendeArbeidstakerUtkast(fnr, metadata.juridiskEnhetOrgnr)) {
            log.info { "Arbeidstaker har eksisterende utkast, sender ikke varsel" }
            return
        }

        val tekster = lagVarselteksterUtenFullmakt(metadata.arbeidsgiverNavn, orgnr)
        brukervarselProducer.sendBrukervarsel(BrukervarselMelding(fnr, tekster, skjemaLenke))
        log.info { "Sendt varsel til arbeidstaker om AG-innsending (skjemadel=${metadata.skjemadel})" }
    }

    private fun varsleOmFullmaktsInnsending(fnr: String, orgnr: String, metadata: UtsendtArbeidstakerMetadata) {
        val tekster = lagVarselteksterMedFullmakt(metadata.arbeidsgiverNavn, orgnr)
        brukervarselProducer.sendBrukervarsel(BrukervarselMelding(fnr, tekster))
        log.info { "Sendt informasjonsvarsel til arbeidstaker om fullmaktsinnsending" }
    }

    private fun harEksisterendeArbeidstakerUtkast(fnr: String, juridiskEnhetOrgnr: String): Boolean =
        skjemaRepository.findByFnrAndStatus(fnr, SkjemaStatus.UTKAST).any { utkast ->
            val m = utkast.metadata as? UtsendtArbeidstakerMetadata
            m != null && m.juridiskEnhetOrgnr == juridiskEnhetOrgnr && m.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL
        }

    private fun lagVarselteksterUtenFullmakt(arbeidsgiverNavn: String, orgnr: String): List<Varseltekst> {
        return listOf(
            Varseltekst(
                språk = Språk.NORSK_BOKMAL,
                tekst = "Arbeidsgiveren din, $arbeidsgiverNavn ($orgnr), har informert Nav om at du skal sendes ut for å jobbe innenfor EU/EØS eller Sveits. Du må derfor sende inn en søknad for at Nav skal kunne avklare om du kan beholde medlemskapet i folketrygden mens du jobber i utlandet.",
                default = true
            ),
            Varseltekst(
                språk = Språk.ENGELSK,
                tekst = "Your employer, $arbeidsgiverNavn ($orgnr), has informed Nav that you will be posted to work within the EU/EEA or Switzerland. You must therefore submit an application so that Nav can determine whether you can retain your membership in the National Insurance Scheme while working abroad.",
                default = false
            )
        )
    }

    private fun lagVarselteksterMedFullmakt(arbeidsgiverNavn: String, orgnr: String): List<Varseltekst> {
        return listOf(
            Varseltekst(
                språk = Språk.NORSK_BOKMAL,
                tekst = "En søknad om utsendt arbeidstaker har blitt sendt inn på dine vegne av $arbeidsgiverNavn ($orgnr). Du trenger ikke foreta deg noe.",
                default = true
            ),
            Varseltekst(
                språk = Språk.ENGELSK,
                tekst = "An application for posted worker has been submitted on your behalf by $arbeidsgiverNavn ($orgnr). You do not need to take any action.",
                default = false
            )
        )
    }
}
