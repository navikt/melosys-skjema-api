package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.parseUtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.HentInnsendteSoknaderRequest
import no.nav.melosys.skjema.types.InnsendtSoknadOversiktDto
import no.nav.melosys.skjema.types.InnsendteSoknaderResponse
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.SorteringsFelt
import no.nav.melosys.skjema.types.Sorteringsretning
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger { }

/**
 * Service for henting av innsendte søknader for Utsendt Arbeidstaker.
 *
 * Håndterer:
 * - Databasepaginering med Spring Data Pageable
 * - Kontekstbasert filtrering basert på representasjonstype
 * - In-memory søk og sortering (pga. JSONB data-felt)
 */
@Service
class HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val jsonMapper: JsonMapper,
    private val subjectHandler: SubjectHandler
) {

    companion object {
        private val INNSENDT_STATUSES = listOf(SkjemaStatus.SENDT)
    }

    /**
     * Henter innsendte søknader basert på representasjonskontekst med paginering, søk og sortering.
     *
     * Filtrerer søknader med status SENDT basert på:
     * - DEG_SELV: fnr = innlogget bruker
     * - ARBEIDSGIVER: ALLE søknader for arbeidsgivere bruker har Altinn-tilgang til
     * - RADGIVER: ALLE søknader for det spesifikke rådgiverfirmaet
     * - ANNEN_PERSON: ALLE søknader for personer bruker har fullmakt for
     *
     * Paginering: Database-nivå med Spring Data Pageable
     * Søk: Database-nivå på fnr og orgnr
     * Sortering: Database-nivå for INNSENDT_DATO og STATUS (JSONB-felter støttes ikke ennå)
     *
     * TODO: Implementer søk på arbeidsgiver navn og arbeidstaker navn (JSONB felter)
     * TODO: Implementer sortering på ARBEIDSGIVER og ARBEIDSTAKER (JSONB felter)
     *
     * @param request Forespørsel med paginerings-, søk- og sorteringsparametere
     * @return Paginert liste med innsendte søknader
     */
    fun hentInnsendteSoknader(request: HentInnsendteSoknaderRequest): InnsendteSoknaderResponse {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.debug { "Henter innsendte søknader for representasjonstype: ${request.representasjonstype}" }

        // Bygg sortering
        val sort = byggSortering(request.sortering, request.retning)
        val pageable = PageRequest.of(request.side - 1, request.antall, sort)

        // Hent paginert resultat fra database
        val page = hentSkjemaerFraDatabase(request, innloggetBrukerFnr, pageable)

        // Konverter til DTOs //TODO: Burde kanskje hente en dataklasse fra databasen, istf å drive med mapping
        val soknader = page.content.map { konverterTilInnsendtSoknadDto(it) }

        log.debug { "Fant ${page.totalElements} innsendte søknader, returnerer side ${request.side} med ${soknader.size} resultater" }

        return InnsendteSoknaderResponse(
            soknader = soknader,
            totaltAntall = page.totalElements.toInt(),
            side = request.side,
            antallPerSide = request.antall
        )
    }

    /**
     * Bygger Spring Data Sort basert på sorteringsfelt og retning.
     *
     * Støtter kun INNSENDT_DATO (endretDato) og STATUS på database-nivå.
     * ARBEIDSGIVER og ARBEIDSTAKER ignoreres (krever JSONB-spørringer).
     *
     * Default: endretDato descending (nyeste først).
     */
    private fun byggSortering(sorteringsFelt: SorteringsFelt?, retning: Sorteringsretning?): Sort {
        // Default: nyeste først
        if (sorteringsFelt == null || retning == null) {
            return Sort.by(Sort.Direction.DESC, "endretDato")
        }

        val direction = when (retning) {
            Sorteringsretning.ASC -> Sort.Direction.ASC
            Sorteringsretning.DESC -> Sort.Direction.DESC
        }

        return when (sorteringsFelt) {
            SorteringsFelt.INNSENDT_DATO -> Sort.by(direction, "endretDato")
            SorteringsFelt.STATUS -> Sort.by(direction, "status")
            SorteringsFelt.ARBEIDSGIVER, SorteringsFelt.ARBEIDSTAKER -> {
                // JSONB-felter støttes ikke ennå, fall tilbake til default
                log.warn { "Sortering på $sorteringsFelt er ikke støttet ennå (JSONB-felt). Bruker default sortering." }
                Sort.by(Sort.Direction.DESC, "endretDato")
            }
        }
    }

    /**
     * Henter skjemaer fra database basert på representasjonstype med paginering og søk.
     */
    private fun hentSkjemaerFraDatabase(
        request: HentInnsendteSoknaderRequest,
        innloggetBrukerFnr: String,
        pageable: PageRequest
    ): Page<Skjema> {
        val searchTerm = request.sok?.takeIf { it.isNotBlank() }

        return when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> hentForDegSelv(innloggetBrukerFnr, pageable, searchTerm)
            Representasjonstype.ARBEIDSGIVER -> hentForArbeidsgiver(pageable, searchTerm)
            Representasjonstype.RADGIVER -> {
                // RADGIVER bruker native SQL, så vi må konvertere JPA field names til kolonne-navn
                val nativePageable = konverterTilNativePageable(pageable)
                hentForRadgiver(request.radgiverfirmaOrgnr, nativePageable, searchTerm)
            }
            Representasjonstype.ANNEN_PERSON -> hentForAnnenPerson(pageable, searchTerm)
        }
    }

    /**
     * Konverterer PageRequest med JPA field names til database: InnsendingRepository-kolonnenavn for native queries.
     */
    private fun konverterTilNativePageable(pageable: PageRequest): PageRequest {
        val nativeSort = Sort.by(
            pageable.sort.map { order ->
                val nativeProperty = when (order.property) {
                    "endretDato" -> "endret_dato"
                    "opprettetDato" -> "opprettet_dato"
                    else -> order.property
                }
                Sort.Order(order.direction, nativeProperty)
            }.toList()
        )
        return PageRequest.of(pageable.pageNumber, pageable.pageSize, nativeSort)
    }

    /**
     * Henter søknader for DEG_SELV - arbeidstaker selv.
     */
    private fun hentForDegSelv(innloggetBrukerFnr: String, pageable: PageRequest, searchTerm: String?): Page<Skjema> {
        return if (searchTerm.isNullOrBlank()) {
            skjemaRepository.findByFnrAndStatusIn(innloggetBrukerFnr, INNSENDT_STATUSES, pageable)
        } else {
            skjemaRepository.findByFnrAndStatusInWithSearch(innloggetBrukerFnr, INNSENDT_STATUSES, searchTerm, pageable)
        }
    }

    /**
     * Henter søknader for ARBEIDSGIVER - alle søknader for arbeidsgivere bruker har tilgang til.
     */
    private fun hentForArbeidsgiver(pageable: PageRequest, searchTerm: String?): Page<Skjema> {
        val tilganger = altinnService.hentBrukersTilganger()
        val orgnrs = tilganger.map { it.orgnr }

        return if (orgnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else if (searchTerm.isNullOrBlank()) {
            skjemaRepository.findByOrgnrInAndStatusIn(orgnrs, INNSENDT_STATUSES, pageable)
        } else {
            skjemaRepository.findByOrgnrInAndStatusInWithSearch(orgnrs, INNSENDT_STATUSES, searchTerm, pageable)
        }
    }

    /**
     * Henter søknader for RADGIVER - alle søknader for det spesifikke rådgiverfirmaet.
     */
    private fun hentForRadgiver(radgiverfirmaOrgnr: String?, pageable: PageRequest, searchTerm: String?): Page<Skjema> {
        requireNotNull(radgiverfirmaOrgnr) { "radgiverfirmaOrgnr er påkrevd for RADGIVER" }

        val tilganger = altinnService.hentBrukersTilganger()
        val orgnrs = tilganger.map { it.orgnr }

        return if (orgnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else {
            val statusStrings = INNSENDT_STATUSES.map { it.name }
            if (searchTerm.isNullOrBlank()) {
                skjemaRepository.findInnsendteForRadgiver(orgnrs, statusStrings, radgiverfirmaOrgnr, pageable)
            } else {
                skjemaRepository.findInnsendteForRadgiverWithSearch(orgnrs, statusStrings, radgiverfirmaOrgnr, searchTerm, pageable)
            }
        }
    }

    /**
     * Henter søknader for ANNEN_PERSON - alle søknader for personer bruker har fullmakt for.
     */
    private fun hentForAnnenPerson(pageable: PageRequest, searchTerm: String?): Page<Skjema> {
        val fullmakter = try {
            reprService.hentKanRepresentere()
        } catch (e: Exception) {
            log.warn(e) { "Feil ved henting av fullmakter for bruker" }
            emptyList()
        }

        val fnrs = fullmakter.map { it.fullmaktsgiver }

        return if (fnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else if (searchTerm.isNullOrBlank()) {
            skjemaRepository.findByFnrInAndStatusIn(fnrs, INNSENDT_STATUSES, pageable)
        } else {
            skjemaRepository.findByFnrInAndStatusInWithSearch(fnrs, INNSENDT_STATUSES, searchTerm, pageable)
        }
    }

    /**
     * Konverterer Skjema til InnsendtSoknadOversiktDto.
     * Maskerer fnr og henter nødvendige metadata-verdier.
     */
    private fun konverterTilInnsendtSoknadDto(skjema: Skjema): InnsendtSoknadOversiktDto {
        val metadata = jsonMapper.parseUtsendtArbeidstakerMetadata(skjema.metadata)
        val innsending = skjema.id?.let { innsendingRepository.findBySkjemaId(it) }

        return InnsendtSoknadOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            referanseId = innsending?.referanseId,
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = null, // TODO: Hent fra data-feltet hvis tilgjengelig
            arbeidstakerFnrMaskert =  maskerFnr(skjema.fnr),
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
