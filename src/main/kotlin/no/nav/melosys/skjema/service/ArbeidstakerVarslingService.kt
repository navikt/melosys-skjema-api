package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.kafka.BrukervarselMelding
import no.nav.melosys.skjema.kafka.BrukervarselProducer
import no.nav.melosys.skjema.kafka.Varseltekst
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
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

        if (metadata.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL) {
            log.debug { "Ingen varsling for kombinert skjemadel (skjema $skjemaId)" }
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

        val navn = metadata.arbeidsgiverNavn.take(MAX_ARBEIDSGIVERNAVN_LENGDE)
        val tekster = lagVarselteksterUtenFullmakt(navn)
        brukervarselProducer.sendBrukervarsel(BrukervarselMelding(fnr, tekster, byggSkjemaLenke(orgnr)))
        log.info { "Sendt varsel til arbeidstaker om AG-innsending (skjemadel=${metadata.skjemadel})" }
    }

    private fun varsleOmFullmaktsInnsending(fnr: String, orgnr: String, metadata: UtsendtArbeidstakerMetadata) {
        val navn = metadata.arbeidsgiverNavn.take(MAX_ARBEIDSGIVERNAVN_LENGDE)
        val tekster = lagVarselteksterMedFullmakt(navn)
        // Mottaker trenger ikke foreta seg noe, så vi sender ikke SMS – kun varsel i innboks
        brukervarselProducer.sendBrukervarsel(BrukervarselMelding(fnr, tekster, sms = false))
        log.info { "Sendt informasjonsvarsel til arbeidstaker om fullmaktsinnsending" }
    }

    /**
     * MELOSYS-8168 (midlertidig): Resender det handlingspliktige varselet (med SMS) til arbeidstaker
     * for et gitt skjema. Brukes av admin-endepunktet for å nå AT-brukere som ikke fikk SMS før
     * SMS-prodsettingen.
     *
     * Sender KUN for handlingspliktige caser (arbeidsgiver/rådgiver uten fullmakt, skjemadel = ARBEIDSGIVERS_DEL).
     * Bypasser [no.nav.melosys.skjema.entity.Innsending.brukervarselSendt]-sjekken (resend er eksplisitt)
     * og endrer ikke det feltet, men beholder utkast-guarden slik at de som har påbegynt sin del ikke purres.
     *
     * @return true hvis varsel ble sendt på nytt, false hvis caset ble hoppet over.
     */
    fun resendVarselTilArbeidstaker(skjemaId: UUID): Boolean {
        val skjema = skjemaRepository.findById(skjemaId).orElse(null)
        if (skjema == null) {
            log.warn { "Resend: fant ikke skjema $skjemaId, hopper over" }
            return false
        }

        val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata
        if (metadata == null) {
            log.warn { "Resend: skjema $skjemaId har ikke UtsendtArbeidstakerMetadata, hopper over" }
            return false
        }

        if (metadata.skjemadel != Skjemadel.ARBEIDSGIVERS_DEL) {
            log.info { "Resend: skjemadel=${metadata.skjemadel} (skjema $skjemaId) er ikke resend-kandidat, hopper over" }
            return false
        }

        return when (metadata) {
            is ArbeidsgiverMetadata, is RadgiverMetadata -> resendUtenFullmakt(skjema.fnr, skjema.orgnr, metadata)
            else -> {
                log.info { "Resend: ${metadata.representasjonstype} (skjema $skjemaId) er ikke handlingspliktig, hopper over" }
                false
            }
        }
    }

    private fun resendUtenFullmakt(fnr: String, orgnr: String, metadata: UtsendtArbeidstakerMetadata): Boolean {
        if (harEksisterendeArbeidstakerUtkast(fnr, metadata.juridiskEnhetOrgnr)) {
            log.info { "Resend: arbeidstaker har eksisterende utkast, sender ikke varsel" }
            return false
        }

        val navn = metadata.arbeidsgiverNavn.take(MAX_ARBEIDSGIVERNAVN_LENGDE)
        val tekster = lagResendVarselteksterUtenFullmakt(navn)
        brukervarselProducer.sendBrukervarsel(BrukervarselMelding(fnr, tekster, byggSkjemaLenke(orgnr)))
        log.info { "Resend: sendt varsel på nytt til arbeidstaker om AG-innsending (skjemadel=${metadata.skjemadel})" }
        return true
    }

    private fun harEksisterendeArbeidstakerUtkast(fnr: String, juridiskEnhetOrgnr: String): Boolean =
        skjemaRepository.findByFnrAndTypeAndStatus(fnr, SkjemaType.UTSENDT_ARBEIDSTAKER, SkjemaStatus.UTKAST).any { utkast ->
            val m = utkast.metadata as? UtsendtArbeidstakerMetadata
            m != null && m.juridiskEnhetOrgnr == juridiskEnhetOrgnr && m.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL
        }

    // Ruter arbeidstaker rett til DEG_SELV-forsiden med arbeidsgivers orgnr forhåndsutfylt
    private fun byggSkjemaLenke(arbeidsgiverOrgnr: String): String =
        "$skjemaLenke$ARBEIDSTAKER_SKJEMA_PATH?representasjonstype=${Representasjonstype.DEG_SELV}&arbeidsgiverOrgnr=$arbeidsgiverOrgnr"

    private fun lagVarselteksterUtenFullmakt(arbeidsgiverNavn: String): List<Varseltekst> {
        return listOf(
            Varseltekst(
                språk = Språk.NORSK_BOKMAL,
                tekst = "Arbeidsgiveren din, $arbeidsgiverNavn, har meldt til Nav at du skal jobbe i EU/EØS eller Sveits. Du må sende inn din del av søknaden slik at Nav kan vurdere om du beholder medlemskapet i folketrygden.",
                default = true
            ),
            Varseltekst(
                språk = Språk.ENGELSK,
                tekst = "Your employer, $arbeidsgiverNavn, has notified Nav that you will work in the EU/EEA or Switzerland. You must submit your part of the application so Nav can assess your National Insurance membership.",
                default = false
            )
        )
    }

    /**
     * MELOSYS-8168 (midlertidig): Samme handlingspliktige tekst som [lagVarselteksterUtenFullmakt],
     * men med en ekstra setning om at de som allerede har sendt inn sin del kan se bort fra meldingen.
     * Tillegget gjelder kun resend, ikke den vanlige varselstien.
     */
    private fun lagResendVarselteksterUtenFullmakt(arbeidsgiverNavn: String): List<Varseltekst> =
        lagVarselteksterUtenFullmakt(arbeidsgiverNavn).map { varseltekst ->
            val tillegg = when (varseltekst.språk) {
                Språk.NORSK_BOKMAL -> " Hvis du allerede har fylt ut og sendt inn din del nylig, kan du se bort fra denne meldingen."
                Språk.ENGELSK -> " If you have already submitted your part recently, you can disregard this message."
                else -> ""
            }
            varseltekst.copy(tekst = varseltekst.tekst + tillegg)
        }

    private fun lagVarselteksterMedFullmakt(arbeidsgiverNavn: String): List<Varseltekst> {
        return listOf(
            Varseltekst(
                språk = Språk.NORSK_BOKMAL,
                tekst = "En søknad om medlemskap i folketrygden under arbeid i utlandet er sendt til Nav på dine vegne av $arbeidsgiverNavn. Du trenger ikke foreta deg noe.",
                default = true
            ),
            Varseltekst(
                språk = Språk.ENGELSK,
                tekst = "An application for National Insurance membership while working abroad has been submitted to Nav on your behalf by $arbeidsgiverNavn. No action is needed.",
                default = false
            )
        )
    }

    companion object {
        private const val MAX_ARBEIDSGIVERNAVN_LENGDE = 100
        private const val ARBEIDSTAKER_SKJEMA_PATH = "/medlemskap-lovvalg/soknad/oversikt"
    }
}
