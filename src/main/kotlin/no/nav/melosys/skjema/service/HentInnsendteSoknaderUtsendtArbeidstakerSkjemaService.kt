package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.UtsendtArbeidstakerSkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.HentInnsendteSoknaderRequest
import no.nav.melosys.skjema.types.InnsendtSoknadOversiktDto
import no.nav.melosys.skjema.types.InnsendteSoknaderResponse
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.SorteringsFelt
import no.nav.melosys.skjema.types.Sorteringsretning
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

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
    private val utsendtArbeidstakerSkjemaRepository: UtsendtArbeidstakerSkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val subjectHandler: SubjectHandler
) {

    companion object {
        private val INNSENDT_STATUS = SkjemaStatus.SENDT.name
        private val ARBEIDSGIVER_TYPER = listOf(Representasjonstype.ARBEIDSGIVER.name, Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT.name)
        private val RADGIVER_TYPER = listOf(Representasjonstype.RADGIVER.name, Representasjonstype.RADGIVER_MED_FULLMAKT.name)
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
     * Bygger Spring Data Sort med native kolonnenavn for bruk i native SQL queries.
     *
     * Støtter kun INNSENDT_DATO (endret_dato) og STATUS på database-nivå.
     * ARBEIDSGIVER og ARBEIDSTAKER ignoreres (krever JSONB-spørringer).
     *
     * Default: endret_dato descending (nyeste først).
     */
    private fun byggSortering(sorteringsFelt: SorteringsFelt?, retning: Sorteringsretning?): Sort {
        // Default: nyeste først
        if (sorteringsFelt == null || retning == null) {
            return Sort.by(Sort.Direction.DESC, "endret_dato")
        }

        val direction = when (retning) {
            Sorteringsretning.ASC -> Sort.Direction.ASC
            Sorteringsretning.DESC -> Sort.Direction.DESC
        }

        return when (sorteringsFelt) {
            SorteringsFelt.INNSENDT_DATO -> Sort.by(direction, "endret_dato")
            SorteringsFelt.STATUS -> Sort.by(direction, "status")
            SorteringsFelt.ARBEIDSGIVER, SorteringsFelt.ARBEIDSTAKER -> {
                log.warn { "Sortering på $sorteringsFelt er ikke støttet ennå (JSONB-felt). Bruker default sortering." }
                Sort.by(Sort.Direction.DESC, "endret_dato")
            }
        }
    }

    /**
     * Henter skjemaer fra database basert på representasjonstype med paginering og søk.
     *
     * Alle queries er native SQL (pga. JSONB-filtrering på representasjonstype).
     */
    private fun hentSkjemaerFraDatabase(
        request: HentInnsendteSoknaderRequest,
        innloggetBrukerFnr: String,
        pageable: PageRequest
    ): Page<Skjema> {
        val searchTerm = request.sok?.takeIf { it.isNotBlank() }

        return when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> hentForDegSelv(innloggetBrukerFnr, pageable, searchTerm)
            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> hentForArbeidsgiver(pageable, searchTerm)

            Representasjonstype.RADGIVER,
            Representasjonstype.RADGIVER_MED_FULLMAKT -> hentForRadgiver(request.radgiverfirmaOrgnr, pageable, searchTerm)

            Representasjonstype.ANNEN_PERSON -> hentForAnnenPerson(pageable, searchTerm)
        }
    }

    private fun hentForDegSelv(innloggetBrukerFnr: String, pageable: PageRequest, searchTerm: String?): Page<Skjema> {
        return if (searchTerm.isNullOrBlank()) {
            utsendtArbeidstakerSkjemaRepository.findByFnrAndStatusAndRepresentasjonstype(
                innloggetBrukerFnr,
                INNSENDT_STATUS,
                Representasjonstype.DEG_SELV.name,
                pageable
            )
        } else {
            utsendtArbeidstakerSkjemaRepository.findByFnrAndStatusAndRepresentasjonstypeWithSearch(
                innloggetBrukerFnr,
                INNSENDT_STATUS,
                Representasjonstype.DEG_SELV.name,
                searchTerm,
                pageable
            )
        }
    }

    private fun hentForArbeidsgiver(pageable: PageRequest, searchTerm: String?): Page<Skjema> {
        val tilganger = altinnService.hentBrukersTilganger()
        val orgnrs = tilganger.map { it.orgnr }

        return if (orgnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else if (searchTerm.isNullOrBlank()) {
            utsendtArbeidstakerSkjemaRepository.findByOrgnrInAndStatusAndRepresentasjonstyper(orgnrs, INNSENDT_STATUS, ARBEIDSGIVER_TYPER, pageable)
        } else {
            utsendtArbeidstakerSkjemaRepository.findByOrgnrInAndStatusAndRepresentasjonstyperWithSearch(orgnrs, INNSENDT_STATUS, ARBEIDSGIVER_TYPER, searchTerm, pageable)
        }
    }

    private fun hentForRadgiver(radgiverfirmaOrgnr: String?, pageable: PageRequest, searchTerm: String?): Page<Skjema> {
        requireNotNull(radgiverfirmaOrgnr) { "radgiverfirmaOrgnr er påkrevd for RADGIVER" }

        val tilganger = altinnService.hentBrukersTilganger()
        val orgnrs = tilganger.map { it.orgnr }

        return if (orgnrs.isEmpty()) {
            PageImpl(emptyList(), pageable, 0)
        } else if (searchTerm.isNullOrBlank()) {
            utsendtArbeidstakerSkjemaRepository.findInnsendteForRadgiver(orgnrs, INNSENDT_STATUS, RADGIVER_TYPER, radgiverfirmaOrgnr, pageable)
        } else {
            utsendtArbeidstakerSkjemaRepository.findInnsendteForRadgiverWithSearch(orgnrs, INNSENDT_STATUS, RADGIVER_TYPER, radgiverfirmaOrgnr, searchTerm, pageable)
        }
    }

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
            utsendtArbeidstakerSkjemaRepository.findByFnrInAndStatusAndRepresentasjonstype(fnrs, INNSENDT_STATUS, Representasjonstype.ANNEN_PERSON.name, pageable)
        } else {
            utsendtArbeidstakerSkjemaRepository.findByFnrInAndStatusAndRepresentasjonstypeWithSearch(fnrs, INNSENDT_STATUS, Representasjonstype.ANNEN_PERSON.name, searchTerm, pageable)
        }
    }

    /**
     * Konverterer Skjema til InnsendtSoknadOversiktDto.
     * Maskerer fnr og henter nødvendige metadata-verdier.
     */
    private fun konverterTilInnsendtSoknadDto(skjema: Skjema): InnsendtSoknadOversiktDto {
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        val innsending = skjema.id?.let { innsendingRepository.findBySkjemaId(it) }

        return InnsendtSoknadOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            referanseId = innsending?.referanseId,
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = null, // TODO: Hent fra data-feltet hvis tilgjengelig
            arbeidstakerFnrMaskert = maskerFnr(skjema.fnr),
            arbeidstakerFodselsdato = hentFodselsdatoFraFnr(skjema.fnr),
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

/**
 * Utleder fødselsdato fra fødselsnummer som LocalDate.
 * Håndterer D-nummer (dag + 40), H-nummer (måned + 40) og FH-nummer (måned + 80).
 * Bestemmer århundre basert på individnummer (siffer 7-9) iht. Skatteetatens regler.
 *
 * @param fnr Fødselsnummer (11 siffer)
 * @return Fødselsdato som LocalDate
 * @throws IllegalArgumentException hvis fnr er ugyldig
 */
fun hentFodselsdatoFraFnr(fnr: String): LocalDate {
    require(fnr.length == 11) { "Fødselsnummer må være 11 siffer, var ${fnr.length}" }

    val dag = fnr.substring(0, 2).toInt()
    val maaned = fnr.substring(2, 4).toInt()
    val toSifferAar = fnr.substring(4, 6).toInt()
    val individnummer = fnr.substring(6, 9).toInt()

    // D-nummer: dag har 40 lagt til
    val justerDag = if (dag > 40) dag - 40 else dag
    // FH-nummer: måned har 80 lagt til, H-nummer: måned har 40 lagt til
    val justerMaaned = when {
        maaned > 80 -> maaned - 80
        maaned > 40 -> maaned - 40
        else -> maaned
    }

    val aarhundre = when {
        individnummer <= 499 -> 19
        individnummer <= 749 -> if (toSifferAar <= 39) 20 else 18
        individnummer <= 899 -> {
            require(toSifferAar <= 39) { "Ugyldig kombinasjon: individnummer $individnummer med år $toSifferAar" }
            20
        }

        else -> if (toSifferAar <= 39) 20 else 19
    }

    val fullAar = aarhundre * 100 + toSifferAar
    return LocalDate.of(fullAar, justerMaaned, justerDag)
}
