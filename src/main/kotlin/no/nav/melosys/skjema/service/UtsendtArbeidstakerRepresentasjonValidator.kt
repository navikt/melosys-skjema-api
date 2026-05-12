package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettUtsendtArbeidstakerSoknadRequest
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Validator for Utsendt Arbeidstaker søknader.
 * Håndterer all forretningslogikk-validering for ulike representasjonstyper. Validering forutsetter innlogget kontekst.
 */
@Component
class UtsendtArbeidstakerRepresentasjonValidator(
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val pdlService: PdlService,
    private val eregService: EregService
) {

    /**
     * Validerer at en opprettelsesforespørsel er gyldig basert på representasjonstype,
     * og returnerer arbeidstakerens fulle navn fra PDL.
     *
     * Navnet caches i metadata slik at listings-endpoints slipper PDL-oppslag.
     *
     * @param request Forespørsel om å opprette søknad
     * @param innloggetBrukerFnr Fnr til innlogget bruker (brukes for DEG_SELV der bruker er arbeidstaker)
     * @return Fullt navn på arbeidstaker fra PDL
     * @throws IllegalArgumentException hvis validering eller PDL-oppslag feiler
     */
    fun validerOpprettelse(
        request: OpprettUtsendtArbeidstakerSoknadRequest,
        innloggetBrukerFnr: String,
    ): String {
        log.info { "Validerer opprettelse av søknad for representasjonstype: ${request.representasjonstype}" }

        val arbeidstakerNavn = when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> validerDegSelv(request, innloggetBrukerFnr)
            Representasjonstype.ARBEIDSGIVER -> validerArbeidsgiverUtenFullmakt(request)
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> validerArbeidsgiverMedFullmakt(request)
            Representasjonstype.RADGIVER -> validerRadgiverUtenFullmakt(request)
            Representasjonstype.RADGIVER_MED_FULLMAKT -> validerRadgiverMedFullmakt(request)
            Representasjonstype.ANNEN_PERSON -> validerAnnenPerson(request)
        }

        log.info { "Validering OK for representasjonstype: ${request.representasjonstype}" }
        return arbeidstakerNavn
    }

    /**
     * Validerer DEG_SELV scenario:
     * - Innlogget person er arbeidstaker
     * - Arbeidsgiver finnes
     */
    private fun validerDegSelv(request: OpprettUtsendtArbeidstakerSoknadRequest, innloggetBrukerFnr: String): String {
        log.debug { "Validerer DEG_SELV scenario" }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        return pdlService.hentNavn(innloggetBrukerFnr)
    }

    /**
     * Validerer ARBEIDSGIVER scenario (uten fullmakt):
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Arbeidstaker valideres via PDL med etternavn
     *
     * @return Fullt navn på arbeidstaker fra PDL
     */
    private fun validerArbeidsgiverUtenFullmakt(request: OpprettUtsendtArbeidstakerSoknadRequest): String {
        log.debug { "Validerer ARBEIDSGIVER scenario (uten fullmakt)" }

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        return validerArbeidstakerViaPdl(request)
    }

    /**
     * Validerer ARBEIDSGIVER_MED_FULLMAKT scenario:
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Innlogget bruker har fullmakt fra arbeidstaker
     */
    private fun validerArbeidsgiverMedFullmakt(request: OpprettUtsendtArbeidstakerSoknadRequest): String {
        log.debug { "Validerer ARBEIDSGIVER_MED_FULLMAKT scenario" }

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        validerFullmaktFraArbeidstaker(request)

        return pdlService.hentNavn(request.arbeidstaker.fnr)
    }

    /**
     * Validerer RADGIVER scenario (uten fullmakt):
     * - Rådgiverfirma må oppgis og finnes
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Arbeidstaker valideres via PDL med etternavn
     *
     * @return Fullt navn på arbeidstaker fra PDL
     */
    private fun validerRadgiverUtenFullmakt(request: OpprettUtsendtArbeidstakerSoknadRequest): String {
        log.debug { "Validerer RADGIVER scenario (uten fullmakt)" }

        validerRadgiverfirma(request)

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        return validerArbeidstakerViaPdl(request)
    }

    /**
     * Validerer RADGIVER_MED_FULLMAKT scenario:
     * - Rådgiverfirma må oppgis og finnes
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Innlogget bruker har fullmakt fra arbeidstaker
     */
    private fun validerRadgiverMedFullmakt(request: OpprettUtsendtArbeidstakerSoknadRequest): String {
        log.debug { "Validerer RADGIVER_MED_FULLMAKT scenario" }

        validerRadgiverfirma(request)

        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        validerFullmaktFraArbeidstaker(request)

        return pdlService.hentNavn(request.arbeidstaker.fnr)
    }

    /**
     * Validerer ANNEN_PERSON scenario:
     * - Innlogget bruker må ha fullmakt fra arbeidstaker
     * - Arbeidsgiver finnes
     */
    private fun validerAnnenPerson(request: OpprettUtsendtArbeidstakerSoknadRequest): String {
        log.debug { "Validerer ANNEN_PERSON scenario" }

        validerFullmaktFraArbeidstaker(request)

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        return pdlService.hentNavn(request.arbeidstaker.fnr)
    }

    private fun validerRadgiverfirma(request: OpprettUtsendtArbeidstakerSoknadRequest) {
        if (request.radgiverfirma == null) {
            throw IllegalArgumentException("Rådgiverfirma må oppgis for ${request.representasjonstype}")
        }

        if (!eregService.organisasjonsnummerEksisterer(request.radgiverfirma!!.orgnr)) {
            throw IllegalArgumentException("Rådgiverfirma med organisasjonsnummer ${request.radgiverfirma!!.orgnr} finnes ikke")
        }
    }

    private fun validerFullmaktFraArbeidstaker(request: OpprettUtsendtArbeidstakerSoknadRequest) {
        log.debug { "Validerer fullmakt fra arbeidstaker via repr-api" }
        if (!reprService.harSkriverettigheterForMedlemskap(request.arbeidstaker.fnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke fullmakt fra arbeidstaker")
        }
        // repr-api validerer også at person finnes i PDL
    }

    private fun validerArbeidstakerViaPdl(request: OpprettUtsendtArbeidstakerSoknadRequest): String {
        log.debug { "Validerer arbeidstaker via PDL" }

        if (request.arbeidstaker.etternavn == null) {
            throw IllegalArgumentException("Etternavn må oppgis for arbeidstaker uten fullmakt")
        }

        val (navn, _) = pdlService.verifiserOgHentPerson(
            request.arbeidstaker.fnr,
            request.arbeidstaker.etternavn!!
        )
        return navn
    }
}
