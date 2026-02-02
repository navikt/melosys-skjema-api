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
            Representasjonstype.ARBEIDSGIVER -> validerArbeidsgiver(request)
            Representasjonstype.RADGIVER -> validerRadgiver(request)
            Representasjonstype.ANNEN_PERSON -> validerAnnenPerson(request)
        }

        log.info { "Validering OK for representasjonstype: ${request.representasjonstype}" }
    }

    /**
     * Validerer DEG_SELV scenario:
     * - Innlogget person er arbeidstaker
     * - Arbeidsgiver finnes
     * - Ingen fullmakt (selv-representasjon)
     */
    private fun validerDegSelv(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer DEG_SELV scenario" }

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        // 3. Ingen fullmakt (selv-representasjon)
        if (request.harFullmakt) {
            throw IllegalArgumentException("harFullmakt kan ikke være true for DEG_SELV")
        }
    }

    /**
     * Validerer ARBEIDSGIVER scenario:
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Arbeidstaker valideres (med eller uten fullmakt)
     */
    private fun validerArbeidsgiver(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer ARBEIDSGIVER scenario" }

        // 2. Validere Altinn-tilgang
        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        // 3. Arbeidsgiver finnes (Altinn validerer delvis, men sjekk også EREG)
        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        // 4. Arbeidstaker-validering
        validerArbeidstakerForArbeidsgiver(request)
    }

    /**
     * Validerer RADGIVER scenario:
     * - Rådgiverfirma må oppgis og finnes
     * - Innlogget bruker har Altinn-tilgang til arbeidsgiver
     * - Arbeidsgiver finnes
     * - Arbeidstaker valideres (med eller uten fullmakt)
     */
    private fun validerRadgiver(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer RADGIVER scenario" }

        // 1. Rådgiverfirma må oppgis
        if (request.radgiverfirma == null) {
            throw IllegalArgumentException("Rådgiverfirma må oppgis for RADGIVER")
        }

        // 2. Rådgiverfirma finnes
        if (!eregService.organisasjonsnummerEksisterer(request.radgiverfirma!!.orgnr)) {
            throw IllegalArgumentException("Rådgiverfirma med organisasjonsnummer ${request.radgiverfirma!!.orgnr} finnes ikke")
        }

        // 4. Validere Altinn-tilgang til arbeidsgiver
        if (!altinnService.harBrukerTilgang(request.arbeidsgiver.orgnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke Altinn-tilgang til arbeidsgiver ${request.arbeidsgiver.orgnr}")
        }

        // 5. Arbeidsgiver finnes (Altinn validerer delvis, men sjekk også EREG)
        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        // 6. Arbeidstaker-validering (samme som ARBEIDSGIVER)
        validerArbeidstakerForArbeidsgiver(request)
    }

    /**
     * Validerer ANNEN_PERSON scenario:
     * - Arbeidstaker må oppgis
     * - Innlogget bruker må ha fullmakt fra arbeidstaker
     * - Arbeidsgiver finnes
     * - harFullmakt må være true
     */
    private fun validerAnnenPerson(request: OpprettSoknadMedKontekstRequest) {
        log.debug { "Validerer ANNEN_PERSON scenario" }

        // 2. Innlogget bruker må ha fullmakt fra arbeidstaker
        if (!reprService.harSkriverettigheterForMedlemskap(request.arbeidstaker.fnr)) {
            throw AccessDeniedException("Innlogget bruker har ikke fullmakt fra arbeidstaker ${request.arbeidstaker.fnr}")
        }
        // Dette sjekker også at arbeidstaker finnes i PDL (repr-api validerer det)

        if (!eregService.organisasjonsnummerEksisterer(request.arbeidsgiver.orgnr)) {
            throw IllegalArgumentException("Arbeidsgiver med organisasjonsnummer ${request.arbeidsgiver.orgnr} finnes ikke")
        }

        // 4. harFullmakt må være true (siden dette er fullmektig-scenario)
        if (!request.harFullmakt) {
            throw IllegalArgumentException("harFullmakt må være true for ANNEN_PERSON")
        }
    }

    /**
     * Validerer arbeidstaker for ARBEIDSGIVER og RADGIVER scenarioer.
     *
     * Med fullmakt: Validerer via repr-api (som også verifiserer PDL)
     * Uten fullmakt: Validerer direkte mot PDL med etternavn
     */
    private fun validerArbeidstakerForArbeidsgiver(
        request: OpprettSoknadMedKontekstRequest
    ) {
        if (request.harFullmakt) {
            // Med fullmakt: Validere via repr-api
            log.debug { "Validerer arbeidstaker med fullmakt via repr-api" }
            if (!reprService.harSkriverettigheterForMedlemskap(request.arbeidstaker.fnr)) {
                throw AccessDeniedException("Innlogget bruker har ikke fullmakt fra arbeidstaker ${request.arbeidstaker.fnr}")
            }
            // repr-api validerer også at person finnes i PDL
        } else {
            // Uten fullmakt: Validere at person finnes i PDL med etternavn-matching
            log.debug { "Validerer arbeidstaker uten fullmakt via PDL" }

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
}
