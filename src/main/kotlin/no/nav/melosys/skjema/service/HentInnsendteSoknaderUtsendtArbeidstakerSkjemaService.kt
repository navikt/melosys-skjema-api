package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.*
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.entity.UtsendtArbeidstakerSkjema
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

/**
 * Service for henting av innsendte søknader (SENDT/MOTTATT) for Utsendt Arbeidstaker.
 *
 * Håndterer:
 * - Databasepaginering med Spring Data Pageable
 * - Kontekstbasert filtrering basert på representasjonstype
 * - In-memory søk og sortering (pga. JSONB data-felt)
 */
@Service
class HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val objectMapper: ObjectMapper,
    private val subjectHandler: SubjectHandler
) {

    companion object {
        private val INNSENDT_STATUSES = listOf(SkjemaStatus.SENDT, SkjemaStatus.MOTTATT)
    }

    /**
     * Henter innsendte søknader basert på representasjonskontekst med paginering, søk og sortering.
     *
     * Filtrerer søknader med status SENDT eller MOTTATT basert på:
     * - DEG_SELV: fnr = innlogget bruker
     * - ARBEIDSGIVER: ALLE søknader for arbeidsgivere bruker har Altinn-tilgang til
     * - RADGIVER: ALLE søknader for det spesifikke rådgiverfirmaet
     * - ANNEN_PERSON: ALLE søknader for personer bruker har fullmakt for
     *
     * Søk: Fritekst-søk i arbeidsgiver navn/orgnr og arbeidstaker navn (in-memory pga. JSONB data-felt)
     * Sortering: På arbeidsgiver, arbeidstaker, innsendt dato, status (in-memory pga. JSONB data-felt)
     * Paginering: Database-nivå med Spring Data Pageable
     *
     * @param request Forespørsel med søk-, sorterings- og pagineringsparametere
     * @return Paginert liste med innsendte søknader
     */
    fun hentInnsendteSoknader(request: HentInnsendteSoknaderRequest): InnsendteSoknaderResponse {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.debug { "Henter innsendte søknader for representasjonstype: ${request.representasjonstype}" }

        val pageable = PageRequest.of(request.side - 1, request.antall)

        // 1. Hent paginert resultat fra database
        val page = hentSkjemaerFraDatabase(request, innloggetBrukerFnr, pageable)

        // 2. Konverter til DTOs
        val alleDtos = page.content.map { konverterTilInnsendtSoknadDto(it) }

        // 3. Filtrer basert på søk
        val filtrerteSkjemaer = filtrerPaSok(alleDtos, request.sok)

        // 4. Sorter basert på sorteringsparametere
        val sorterteSkjemaer = sorterSkjemaer(filtrerteSkjemaer, request.sortering, request.retning)

        log.debug { "Fant ${page.totalElements} innsendte søknader, returnerer side ${request.side} med ${sorterteSkjemaer.size} resultater" }

        return InnsendteSoknaderResponse(
            soknader = sorterteSkjemaer,
            totaltAntall = page.totalElements.toInt(),
            side = request.side,
            antallPerSide = request.antall
        )
    }

    /**
     * Henter skjemaer fra database basert på representasjonstype med paginering.
     */
    private fun hentSkjemaerFraDatabase(
        request: HentInnsendteSoknaderRequest,
        innloggetBrukerFnr: String,
        pageable: PageRequest
    ): Page<Skjema> {
        return when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> hentForDegSelv(innloggetBrukerFnr, pageable)
            Representasjonstype.ARBEIDSGIVER -> hentForArbeidsgiver(pageable)
            Representasjonstype.RADGIVER -> hentForRadgiver(request.radgiverfirmaOrgnr, pageable)
            Representasjonstype.ANNEN_PERSON -> hentForAnnenPerson(pageable)
        }
    }

    /**
     * Henter søknader for DEG_SELV - arbeidstaker selv.
     */
    private fun hentForDegSelv(innloggetBrukerFnr: String, pageable: PageRequest): Page<Skjema> {
        return skjemaRepository.findByFnrAndStatusIn(innloggetBrukerFnr, INNSENDT_STATUSES, pageable)
    }

    /**
     * Henter søknader for ARBEIDSGIVER - alle søknader for arbeidsgivere bruker har tilgang til.
     */
    private fun hentForArbeidsgiver(pageable: PageRequest): Page<Skjema> {
        val tilganger = altinnService.hentBrukersTilganger()
        val orgnrs = tilganger.map { it.orgnr }

        return if (orgnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else {
            skjemaRepository.findByOrgnrInAndStatusIn(orgnrs, INNSENDT_STATUSES, pageable)
        }
    }

    /**
     * Henter søknader for RADGIVER - alle søknader for det spesifikke rådgiverfirmaet.
     */
    private fun hentForRadgiver(radgiverfirmaOrgnr: String?, pageable: PageRequest): Page<Skjema> {
        requireNotNull(radgiverfirmaOrgnr) { "radgiverfirmaOrgnr er påkrevd for RADGIVER" }

        val tilganger = altinnService.hentBrukersTilganger()
        val orgnrs = tilganger.map { it.orgnr }

        return if (orgnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else {
            skjemaRepository.findInnsendteForRadgiver(orgnrs, INNSENDT_STATUSES, radgiverfirmaOrgnr, pageable)
        }
    }

    /**
     * Henter søknader for ANNEN_PERSON - alle søknader for personer bruker har fullmakt for.
     */
    private fun hentForAnnenPerson(pageable: PageRequest): Page<Skjema> {
        val fullmakter = try {
            reprService.hentKanRepresentere()
        } catch (e: Exception) {
            log.warn(e) { "Feil ved henting av fullmakter for bruker" }
            emptyList()
        }

        val fnrs = fullmakter.map { it.fullmaktsgiver }

        return if (fnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else {
            skjemaRepository.findByFnrInAndStatusIn(fnrs, INNSENDT_STATUSES, pageable)
        }
    }

    /**
     * Filtrerer søknader basert på fritekst-søk.
     *
     * Søker i:
     * - Arbeidsgiver navn og orgnr
     * - Arbeidstaker navn (hvis tilgjengelig)
     *
     * IKKE søk i fødselsnummer (sikkerhet).
     */
    private fun filtrerPaSok(dtos: List<InnsendtSoknadOversiktDto>, sok: String?): List<InnsendtSoknadOversiktDto> {
        if (sok.isNullOrBlank()) {
            return dtos
        }

        val sokTerm = sok.lowercase()
        return dtos.filter { dto ->
            val arbeidsgiverMatch = dto.arbeidsgiverNavn?.lowercase()?.contains(sokTerm) == true ||
                dto.arbeidsgiverOrgnr?.lowercase()?.contains(sokTerm) == true

            val arbeidstakerMatch = dto.arbeidstakerNavn?.lowercase()?.contains(sokTerm) == true

            arbeidsgiverMatch || arbeidstakerMatch
        }
    }

    /**
     * Sorterer søknader basert på sorteringsfelt og retning.
     *
     * Default sortering: Nyeste først (innsendtDato DESC).
     */
    private fun sorterSkjemaer(
        dtos: List<InnsendtSoknadOversiktDto>,
        sortering: SorteringsFelt?,
        retning: Sorteringsretning?
    ): List<InnsendtSoknadOversiktDto> {
        if (sortering == null || retning == null) {
            return dtos.sortedByDescending { it.innsendtDato }
        }

        val comparator = when (sortering) {
            SorteringsFelt.ARBEIDSGIVER -> compareBy<InnsendtSoknadOversiktDto> { it.arbeidsgiverNavn }
            SorteringsFelt.ARBEIDSTAKER -> compareBy { it.arbeidstakerNavn }
            SorteringsFelt.INNSENDT_DATO -> compareBy { it.innsendtDato }
            SorteringsFelt.STATUS -> compareBy { it.status }
        }

        return when (retning) {
            Sorteringsretning.ASC -> dtos.sortedWith(comparator)
            Sorteringsretning.DESC -> dtos.sortedWith(comparator.reversed())
        }
    }

    /**
     * Konverterer Skjema til InnsendtSoknadOversiktDto.
     * Maskerer fnr og henter nødvendige metadata-verdier.
     */
    private fun konverterTilInnsendtSoknadDto(skjema: Skjema): InnsendtSoknadOversiktDto {
        val utsendtSkjema = UtsendtArbeidstakerSkjema(skjema, objectMapper)
        val metadata = utsendtSkjema.metadata

        return InnsendtSoknadOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = null, // TODO: Hent fra data-feltet hvis tilgjengelig
            arbeidstakerFnrMaskert = skjema.fnr?.let { maskerFnr(it) },
            innsendtDato = skjema.endretDato, // Siste endring er når søknaden ble sendt
            status = skjema.status,
            harPdf = false // TODO: Implementer når PDF-funksjonalitet er på plass
        )
    }

    /**
     * Maskerer fødselsnummer for visning.
     * Viser kun de første 6 sifrene (fødselsdato) og skjuler resten.
     *
     * @param fnr Fødselsnummer (11 siffer)
     * @return Maskert fnr (f.eks. "010190*****")
     */
    private fun maskerFnr(fnr: String): String {
        return if (fnr.length == 11) {
            fnr.substring(0, 6) + "*****"
        } else {
            "***********"
        }
    }
}
