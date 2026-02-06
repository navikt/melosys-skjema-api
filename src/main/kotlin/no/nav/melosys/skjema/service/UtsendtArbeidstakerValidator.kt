package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.types.OpprettSoknadMedKontekstRequest
import no.nav.melosys.skjema.types.Representasjonstype
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Validator for Utsendt Arbeidstaker søknader.
 * Håndterer all forretningslogikk-validering for ulike representasjonstyper. Validering forutsetter innlogget kontekst.
 */
@Component
class UtsendtArbeidstakerValidator(
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val pdlService: PdlService,
    private val eregService: EregService
) {

    /**
     * Validerer at en opprettelsesforespørsel er gyldig basert på representasjonstype.
     *
     * @param request Forespørsel om å opprette søknad
     * @throws IllegalArgumentException hvis validering feiler
     */
    fun validerOpprettelse(
        request: OpprettSoknadMedKontekstRequest,
    ) {
        log.info { "Validerer opprettelse av søknad for representasjonstype: ${request.representasjonstype}" }

        when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> validerDegSelv(request)
            Representasjonstype.ARBEIDSGIVER -> validerArbeidsgiverUtenFullmakt(request)
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> validerArbeidsgiverMedFullmakt(request)
            Representasjonstype.RADGIVER -> validerRadgiverUtenFullmakt(request)
            Representasjonstype.RADGIVER_MED_FULLMAKT -> validerRadgiverMedFullmakt(request)
            Representasjonstype.ANNEN_PERSON -> validerAnnenPerson(request)
        }

        log.info { "Validering OK for representasjonstype: ${request.representasjonstype}" }
    }

    /**
     * Validerer DEG_SELV scenario:
     * - Innlogget person er arbeidstaker
     * - Arbeidsgiver finnes
     */
    private fun validerDegSelv(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer DEG_SELV scenario" }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }
    }

    /**
     * Validerer ARBEIDSGIVER scenario (uten fullmakt):
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Arbeidstaker valideres via PDL med etternavn
     */
    private fun validerArbeidsgiverUtenFullmakt(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer ARBEIDSGIVER scenario (uten fullmakt)" }

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        validerArbeidstakerViaPdl(request)
    }

    /**
     * Validerer ARBEIDSGIVER_MED_FULLMAKT scenario:
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Innlogget bruker har fullmakt fra arbeidstaker
     */
    private fun validerArbeidsgiverMedFullmakt(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer ARBEIDSGIVER_MED_FULLMAKT scenario" }

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        validerFullmaktFraArbeidstaker(request)
    }

    /**
     * Validerer RADGIVER scenario (uten fullmakt):
     * - Rådgiverfirma må oppgis og finnes
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Arbeidstaker valideres via PDL med etternavn
     */
    private fun validerRadgiverUtenFullmakt(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer RADGIVER scenario (uten fullmakt)" }

        validerRadgiverfirma(request)

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        validerArbeidstakerViaPdl(request)
    }

    /**
     * Validerer RADGIVER_MED_FULLMAKT scenario:
     * - Rådgiverfirma må oppgis og finnes
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Innlogget bruker har fullmakt fra arbeidstaker
     */
    private fun validerRadgiverMedFullmakt(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer RADGIVER_MED_FULLMAKT scenario" }

        validerRadgiverfirma(request)

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        validerFullmaktFraArbeidstaker(request)
    }

    /**
     * Validerer ANNEN_PERSON scenario:
     * - Innlogget bruker må ha fullmakt fra arbeidstaker
     * - Arbeidsgiver finnes
     */
    private fun validerAnnenPerson(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer ANNEN_PERSON scenario" }

        validerFullmaktFraArbeidstaker(request)

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }
    }

    private fun validerRadgiverfirma(request: OpprettSoknadMedKontekstRequest) {
        if (request.radgiverfirma == null) {
            throw IllegalArgumentException("Rådgiverfirma må oppgis for ${request.representasjonstype}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.radgiverfirma!!.orgnr)) {
            throw IllegalArgumentException("Rådgiverfirma med organisasjonsnummer ${request.radgiverfirma!!.orgnr} finnes ikke")
        }
    }

    private fun validerFullmaktFraArbeidstaker(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer fullmakt fra arbeidstaker via repr-api" }
        if (!reprService.harSkriverettigheterForMedlemskap(request.arbeidstaker.fnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke fullmakt fra arbeidstaker ${request.arbeidstaker.fnr}")
        }
        // repr-api validerer også at person finnes i PDL
    }

    private fun validerArbeidstakerViaPdl(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer arbeidstaker via PDL" }

        if (request.arbeidstaker.etternavn == null) {
            throw IllegalArgumentException("Etternavn må oppgis for arbeidstaker uten fullmakt")
        }

        try {
            pdlService.verifiserOgHentPerson(
                request.arbeidstaker.fnr,
                request.arbeidstaker.etternavn!!
            )
        } catch (e: Exception) {
            log.warn(e) { "Arbeidstaker kunne ikke verifiseres i PDL" }
            throw IllegalArgumentException(
                "Arbeidstaker med fødselsnummer ${request.arbeidstaker.fnr} finnes ikke eller etternavn matcher ikke",
                e
            )
        }
    }
}
