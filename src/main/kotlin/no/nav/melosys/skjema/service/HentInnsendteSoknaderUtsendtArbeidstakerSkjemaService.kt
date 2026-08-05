package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.UtsendtArbeidstakerSkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.HentInnsendteSoknaderRequest
import no.nav.melosys.skjema.types.InnsendtSoknadOversiktDto
import no.nav.melosys.skjema.types.InnsendteSoknaderResponse
import no.nav.melosys.skjema.types.MotpartStatus
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
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

        val personerMedAktivFullmakt = when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> emptySet()
            else -> reprService.hentFullmaktsgiverFnr()
        }

        val sendteVersjonerPerFnr = hentSendteVersjonerPerFnr(page.content)

        val soknader = page.content.map { konverterTilInnsendtSoknadDto(it, personerMedAktivFullmakt, sendteVersjonerPerFnr) }

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
        val fullmakter = reprService.hentKanRepresentere()

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
     * Batch-henter metadata for alle SENDT-e utsendt arbeidstaker-skjemaer som tilhører personene
     * på siden, gruppert per fnr.
     *
     * Ett spørringskall per side (fnr IN) i stedet for ett oppslag per rad eller per ledd i
     * erstatter-kjeden — unngår N+1 selv om sidestørrelsen er lav (typisk 5 rader).
     * Motpart-koblinger og erstatter-referanser settes kun mellom skjemaer med samme fnr
     * (se [UtsendtArbeidstakerSkjemaKoblingService]), så settet dekker hele kjeden for hver rad.
     */
    private fun hentSendteVersjonerPerFnr(skjemaer: List<Skjema>): Map<String, SendteVersjoner> {
        val fnrs = skjemaer
            .filter { (it.metadata as? UtsendtArbeidstakerMetadata)?.skjemadel != Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }
            .map { it.fnr }
            .distinct()

        if (fnrs.isEmpty()) {
            return emptyMap()
        }

        return utsendtArbeidstakerSkjemaRepository.findByFnrInAndStatus(fnrs, INNSENDT_STATUS)
            .groupBy { it.fnr }
            .mapValues { (_, sendte) -> SendteVersjoner(sendte) }
    }

    /**
     * Utleder motpart-status for en skjemadel:
     * - Kombinert del (ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL) har aldri motpart → IKKE_RELEVANT
     * - Motpartens del finnes i noen SENDT versjon → HAR_SENDT
     * - Ellers → VENTER (inkluderer koblet skjema som kun er utkast, og motpart via annen kanal)
     *
     * Resubmisjonstilfellet (produkteier-beslutning 2026-07-27): når en part sender NY VERSJON av
     * sin del, flytter [UtsendtArbeidstakerSkjemaKoblingService] koblingen til den nye versjonen og
     * NULLER `kobletSkjemaId` på den gamle. Et rent `kobletSkjemaId`-oppslag ville derfor vist
     * VENTER for rader hvis kobling er flyttet — selv om motparten faktisk har sendt. Derfor følges
     * erstatter-kjeden for radens egen del (transitivt, begge retninger), og raden viser HAR_SENDT
     * når noen versjon i kjeden er koblet til en motpart-del som finnes i en SENDT versjon — den
     * koblede selv eller en versjon i dens erstatter-kjede (nyere som har overtatt koblingen).
     *
     * Traverseringen skjer i minne over batch-oppslaget fra [hentSendteVersjonerPerFnr] og er
     * sirkel-sikker via besøkt-sett (samme mønster som M2MSkjemaService.hentTidligereInnsendteSkjema).
     */
    private fun utledMotpartStatus(
        skjema: Skjema,
        metadata: UtsendtArbeidstakerMetadata,
        sendteVersjonerPerFnr: Map<String, SendteVersjoner>
    ): MotpartStatus {
        val motpartsDel = when (metadata.skjemadel) {
            Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> return MotpartStatus.IKKE_RELEVANT
            Skjemadel.ARBEIDSTAKERS_DEL -> Skjemadel.ARBEIDSGIVERS_DEL
            Skjemadel.ARBEIDSGIVERS_DEL -> Skjemadel.ARBEIDSTAKERS_DEL
        }
        val sendte = sendteVersjonerPerFnr[skjema.fnr] ?: return MotpartStatus.VENTER

        // Motpart-kandidater: radens egen kobling + koblingene til alle versjoner i radens erstatter-kjede
        // (koblingen kan være flyttet til en nyere versjon av radens egen del).
        val motpartKandidater = buildSet {
            metadata.kobletSkjemaId?.let(::add)
            finnErstatterKjede(skjema.id, metadata, sendte).forEach { versjonId ->
                sendte.metadataPerId[versjonId]?.kobletSkjemaId?.let(::add)
            }
        }

        val motpartHarSendt = motpartKandidater.any { kandidatId ->
            finnErstatterKjede(kandidatId, sendte.metadataPerId[kandidatId], sendte)
                .any { sendte.metadataPerId[it]?.skjemadel == motpartsDel }
        }
        return if (motpartHarSendt) MotpartStatus.HAR_SENDT else MotpartStatus.VENTER
    }

    /**
     * Finner alle versjoner i erstatter-kjeden til [startId] (inkludert startId selv) blant
     * personens SENDT-e skjemaer — transitivt i begge retninger: eldre versjoner via
     * `erstatterSkjemaId` og nyere versjoner via den reverserte relasjonen.
     *
     * Sirkel-sikker: besøkt-settet garanterer terminering selv ved sirkulære erstatter-referanser.
     */
    private fun finnErstatterKjede(
        startId: UUID?,
        startMetadata: UtsendtArbeidstakerMetadata?,
        sendte: SendteVersjoner
    ): Set<UUID> {
        startId ?: return emptySet()
        val besokt = mutableSetOf(startId)
        val ko = ArrayDeque(listOf(startId))
        while (ko.isNotEmpty()) {
            val id = ko.removeFirst()
            val metadata = if (id == startId) startMetadata else sendte.metadataPerId[id]
            val naboer = listOfNotNull(metadata?.erstatterSkjemaId) + sendte.erstattetAvPerId[id].orEmpty()
            naboer.forEach { if (besokt.add(it)) ko.add(it) }
        }
        return besokt
    }

    /**
     * Konverterer Skjema til InnsendtSoknadOversiktDto.
     * Maskerer fnr og henter nødvendige metadata-verdier.
     */
    private fun konverterTilInnsendtSoknadDto(
        skjema: Skjema,
        personerMedAktivFullmakt: Set<String>,
        sendteVersjonerPerFnr: Map<String, SendteVersjoner>
    ): InnsendtSoknadOversiktDto {
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        val innsending = skjema.id?.let { innsendingRepository.findBySkjemaId(it) }

        return InnsendtSoknadOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            referanseId = innsending?.referanseId,
            saksnummer = innsending?.saksnummer,
            saksstatus = innsending?.saksstatus,
            motpartStatus = utledMotpartStatus(skjema, metadata, sendteVersjonerPerFnr),
            skjemadel = metadata.skjemadel,
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = metadata.arbeidstakerNavn,
            arbeidstakerFnrMaskert = maskerFnr(skjema.fnr),
            arbeidstakerFodselsdato = hentFodselsdatoFraFnr(skjema.fnr),
            innsendtDato = skjema.endretDato,
            status = skjema.status,
            fullmaktAktiv = erFullmaktAktiv(skjema.id, metadata, skjema.fnr, personerMedAktivFullmakt)
        )
    }

    private fun erFullmaktAktiv(
        skjemaId: UUID?,
        metadata: UtsendtArbeidstakerMetadata,
        arbeidstakerFnr: String,
        personerMedAktivFullmakt: Set<String>
    ): Boolean? {
        return when (metadata.representasjonstype) {
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
            Representasjonstype.RADGIVER_MED_FULLMAKT,
            Representasjonstype.ANNEN_PERSON -> {
                val aktiv = personerMedAktivFullmakt.contains(arbeidstakerFnr)
                if (!aktiv) {
                    log.warn { "Fullmakt tapt for skjema $skjemaId, representasjonstype: ${metadata.representasjonstype}" }
                }
                aktiv
            }
            else -> null
        }
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
 * Oppslagsstruktur over én persons SENDT-e utsendt arbeidstaker-skjemaer:
 * metadata per skjema-id og den reverserte erstatter-relasjonen
 * (hvilke skjemaer som oppgir å erstatte en gitt id).
 */
private class SendteVersjoner(sendte: List<Skjema>) {
    val metadataPerId: Map<UUID, UtsendtArbeidstakerMetadata> = sendte
        .mapNotNull { skjema ->
            val id = skjema.id ?: return@mapNotNull null
            val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@mapNotNull null
            id to metadata
        }
        .toMap()

    val erstattetAvPerId: Map<UUID, List<UUID>> = metadataPerId.entries
        .mapNotNull { (id, metadata) -> metadata.erstatterSkjemaId?.let { it to id } }
        .groupBy({ it.first }, { it.second })
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
