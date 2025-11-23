package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.*
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.entity.UtsendtArbeidstakerSkjema
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger { }

/**
 * Service for håndtering av Utsendt Arbeidstaker søknader.
 * Inneholder all forretningslogikk for opprettelse, henting og tilgangskontroll.
 */
@Service
class UtsendtArbeidstakerService(
    private val skjemaRepository: SkjemaRepository,
    private val validator: UtsendtArbeidstakerValidator,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val objectMapper: ObjectMapper,
    private val subjectHandler: SubjectHandler
) {

    /**
     * Oppretter en ny Utsendt Arbeidstaker søknad med forhåndsvalgt kontekst.
     *
     * Validerer all input basert på representasjonstype før opprettelse.
     *
     * @param request Forespørsel med representasjonskontekst
     * @return Response med skjema-ID og status
     * @throws IllegalArgumentException hvis validering feiler
     */
    fun opprettMedKontekst(request: OpprettSoknadMedKontekstRequest): OpprettSoknadMedKontekstResponse {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.info { "Oppretter Utsendt Arbeidstaker søknad for representasjonstype: ${request.representasjonstype}" }

        // Valider forespørsel
        validator.validerOpprettelse(request, innloggetBrukerFnr)

        // Bygg metadata med korrekt fullmektig-logikk
        val metadata = byggMetadata(request, innloggetBrukerFnr)
        val metadataJson = objectMapper.valueToTree<JsonNode>(metadata)

        // Opprett skjema med riktig fnr og orgnr basert på representasjonstype
        val skjema = when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> {
                // Arbeidstaker fyller ut for seg selv
                Skjema(
                    status = SkjemaStatus.UTKAST,
                    fnr = innloggetBrukerFnr,
                    orgnr = request.arbeidsgiver?.orgnr,
                    metadata = metadataJson,
                    opprettetAv = innloggetBrukerFnr,
                    endretAv = innloggetBrukerFnr
                )
            }

            Representasjonstype.ARBEIDSGIVER, Representasjonstype.RADGIVER -> {
                // Arbeidsgiver eller rådgiver fyller ut på vegne av arbeidstaker
                Skjema(
                    status = SkjemaStatus.UTKAST,
                    orgnr = request.arbeidsgiver?.orgnr,
                    fnr = request.arbeidstaker?.fnr,
                    metadata = metadataJson,
                    opprettetAv = innloggetBrukerFnr,
                    endretAv = innloggetBrukerFnr
                )
            }

            Representasjonstype.ANNEN_PERSON -> {
                // Fullmektig fyller ut på vegne av arbeidstaker
                Skjema(
                    status = SkjemaStatus.UTKAST,
                    fnr = request.arbeidstaker?.fnr,
                    orgnr = request.arbeidsgiver?.orgnr,
                    metadata = metadataJson,
                    opprettetAv = innloggetBrukerFnr,
                    endretAv = innloggetBrukerFnr
                )
            }
        }

        val savedSkjema = skjemaRepository.save(skjema)
        log.info { "Opprettet Utsendt Arbeidstaker søknad med id: ${savedSkjema.id}, representasjonstype: ${request.representasjonstype}" }

        return OpprettSoknadMedKontekstResponse(
            id = savedSkjema.id ?: throw IllegalStateException("Skjema ID var null etter lagring"),
            status = savedSkjema.status
        )
    }

    /**
     * Henter et Utsendt Arbeidstaker skjema med tilgangskontroll.
     *
     * Validerer at innlogget bruker har tilgang til skjemaet basert på:
     * - Om bruker er arbeidstaker
     * - Om bruker er fullmektig (og har aktiv fullmakt via repr-api)
     * - Om bruker har Altinn-tilgang til arbeidsgiver
     *
     * @param skjemaId ID til skjemaet
     * @return UtsendtArbeidstakerSkjema med type-safe metadata
     * @throws NoSuchElementException hvis skjema ikke finnes
     * @throws AccessDeniedException hvis tilgang nektes
     */
    fun hentSkjema(skjemaId: UUID): UtsendtArbeidstakerSkjema {
        val currentUser = subjectHandler.getUserID()
        log.debug { "Henter Utsendt Arbeidstaker skjema: $skjemaId" }

        val skjema = skjemaRepository.findByIdOrNull(skjemaId)
            ?: throw NoSuchElementException("Skjema med id $skjemaId finnes ikke")

        // Valider tilgang
        validerTilgangTilSkjema(skjema, currentUser)

        return UtsendtArbeidstakerSkjema(skjema, objectMapper)
    }

    /**
     * Validerer at innlogget bruker har tilgang til skjemaet.
     *
     * Sjekker følgende i prioritert rekkefølge:
     * 1. Er bruker arbeidstaker? (fnr match)
     * 2. Er bruker fullmektig? (fullmektigFnr match + aktiv fullmakt via repr-api)
     * 3. Har bruker Altinn-tilgang til arbeidsgiver? (orgnr match)
     *
     * VIKTIG: Fullmakt verifiseres ALLTID mot repr-api for å sikre at den fortsatt er aktiv.
     *
     * @param skjema Skjemaet som skal sjekkes
     * @param currentUser Innlogget bruker
     * @throws IllegalArgumentException hvis bruker ikke har tilgang
     */
    private fun validerTilgangTilSkjema(skjema: Skjema, currentUser: String) {
        val utsendtSkjema = UtsendtArbeidstakerSkjema(skjema, objectMapper)
        val metadata = utsendtSkjema.metadata

        val harTilgang = when {
            // 1. Arbeidstaker selv
            skjema.fnr == currentUser -> {
                log.debug { "Tilgang gitt: Bruker er arbeidstaker" }
                true
            }

            // 2. Fullmektig - VIKTIG: Alltid verifiser via repr-api!
            metadata.fullmektigFnr != null &&
                    metadata.fullmektigFnr == currentUser &&
                    skjema.fnr != null -> {
                // Sjekk at fullmakten fortsatt er aktiv
                val harAktivFullmakt = try {
                    reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
                } catch (e: Exception) {
                    log.warn(e) { "Feil ved sjekk av fullmakt for skjema $skjema.id" }
                    false
                }

                if (harAktivFullmakt) {
                    log.debug { "Tilgang gitt: Bruker er fullmektig med aktiv fullmakt" }
                } else {
                    log.warn { "Fullmakt er ikke lenger aktiv for skjema ${skjema.id}" }
                }

                harAktivFullmakt
            }

            // 3. Arbeidsgiver/rådgiver med Altinn-tilgang
            skjema.orgnr != null && altinnService.harBrukerTilgang(skjema.orgnr) -> {
                log.debug { "Tilgang gitt: Bruker har Altinn-tilgang til arbeidsgiver" }
                true
            }

            else -> {
                log.warn { "Tilgang nektet til skjema ${skjema.id} for bruker" }
                false
            }
        }

        if (!harTilgang) {
            throw IllegalArgumentException("Bruker har ikke tilgang til skjema ${skjema.id}")
        }
    }

    /**
     * Henter alle skjemaer hvor innlogget bruker har en eller annen form for tilgang.
     *
     * Inkluderer skjemaer hvor bruker er:
     * - Arbeidstaker selv
     * - Fullmektig for arbeidstaker (med aktiv fullmakt)
     * - Arbeidsgiver/rådgiver (via Altinn-tilgang)
     */
    fun listAlleSkjemaerForBruker(): List<UtsendtArbeidstakerSkjema> {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.debug { "Lister alle skjemaer for bruker" }

        // 1. Skjemaer hvor bruker er arbeidstaker
        val somArbeidstaker = skjemaRepository.findByFnr(innloggetBrukerFnr)

        // 2. Skjemaer hvor bruker er fullmektig (må verifisere aktiv fullmakt)
        val somFullmektig = skjemaRepository.findByFullmektigFnr(innloggetBrukerFnr)
            .filter { skjema ->
                skjema.fnr != null && try {
                    reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
                } catch (e: Exception) {
                    log.warn(e) { "Feil ved sjekk av fullmakt for skjema ${skjema.id}" }
                    false
                }
            }

        // 3. Skjemaer hvor bruker har Altinn-tilgang til arbeidsgiver
        val tilganger = altinnService.hentBrukersTilganger()
        val somArbeidsgiver = tilganger.flatMap { org ->
            skjemaRepository.findByOrgnr(org.orgnr)
        }

        // Kombiner og fjern duplikater
        val alleSkjemaer = (somArbeidstaker + somFullmektig + somArbeidsgiver)
            .distinctBy { it.id }
            .map { UtsendtArbeidstakerSkjema(it, objectMapper) }

        log.debug { "Fant ${alleSkjemaer.size} skjemaer for bruker (arbeidstaker: ${somArbeidstaker.size}, fullmektig: ${somFullmektig.size}, arbeidsgiver: ${somArbeidsgiver.size})" }

        return alleSkjemaer
    }

    /**
     * Bygger metadata-objekt med korrekt fullmektig-logikk.
     *
     * fullmektigFnr representerer HVEM som kan fylle ut arbeidstaker-delen på vegne av arbeidstaker.
     * Dette feltet settes kun når det faktisk ER en fullmektig som kan representere arbeidstaker:
     *
     * - DEG_SELV: Ingen fullmektig (null)
     *   → Arbeidstaker er innlogget bruker og fyller selv, trenger ikke fullmakt
     *
     * - ANNEN_PERSON: Innlogget bruker er fullmektig (innloggetBrukerFnr)
     *   → Advokat/fullmektig representerer arbeidstaker, validert via repr-api
     *
     * - ARBEIDSGIVER/RADGIVER med harFullmakt=true: Innlogget bruker er fullmektig (innloggetBrukerFnr)
     *   → HR-person/konsulent har fått fullmakt fra arbeidstaker (validert via repr-api)
     *   → Kan fylle både arbeidsgiver-del OG arbeidstaker-del
     *
     * - ARBEIDSGIVER/RADGIVER med harFullmakt=false: Ingen fullmektig (null)
     *   → Arbeidsgiver/rådgiver fyller kun sin egen del
     *   → Arbeidstaker må selv fylle sin del (validert at person finnes i PDL)
     *
     * Merk: Validering har allerede bekreftet at fullmakt eksisterer når harFullmakt=true
     */
    private fun byggMetadata(
        request: OpprettSoknadMedKontekstRequest,
        innloggetBrukerFnr: String
    ): UtsendtArbeidstakerMetadata {
        val fullmektigFnr = when {
            request.representasjonstype == Representasjonstype.DEG_SELV -> null // Ingen fullmektig
            request.representasjonstype == Representasjonstype.ANNEN_PERSON -> innloggetBrukerFnr // Fullmektig er innlogget bruker
            request.harFullmakt -> innloggetBrukerFnr // Arbeidsgiver/rådgiver MED fullmakt (validert)
            else -> null // Arbeidsgiver/rådgiver UTEN fullmakt (arbeidstaker fyller selv)
        }

        return UtsendtArbeidstakerMetadata(
            representasjonstype = request.representasjonstype,
            harFullmakt = request.harFullmakt,
            radgiverfirma = request.radgiverfirma?.let {
                RadgiverfirmaInfo(orgnr = it.orgnr, navn = it.navn)
            },
            arbeidsgiverNavn = request.arbeidsgiver?.navn,
            fullmektigFnr = fullmektigFnr
        )
    }

    /**
     * Henter utkast basert på representasjonskontekst.
     *
     * Filtrerer søknader med status UTKAST basert på:
     * - DEG_SELV: fnr = innlogget bruker
     * - ARBEIDSGIVER: opprettetAv = innlogget bruker OG orgnr i Altinn-tilganger
     * - RADGIVER: opprettetAv = innlogget bruker OG orgnr i Altinn-tilganger
     * - ANNEN_PERSON: opprettetAv = innlogget bruker OG fnr i fullmaktsliste
     *
     * @param request Kun representasjonstype - filtreringen gjøres basert på brukerens tilganger
     * @return Liste med utkast
     */
    fun hentUtkast(request: HentUtkastRequest): UtkastListeResponse {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.debug { "Henter utkast for representasjonstype: ${request.representasjonstype}" }

        val utkastSkjemaer = when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> {
                // Arbeidstaker fyller ut for seg selv
                skjemaRepository.findByFnrAndStatus(
                    innloggetBrukerFnr,
                    SkjemaStatus.UTKAST
                ).filter { skjema ->
                    // Sikre at representasjonstype i metadata er DEG_SELV
                    val utsendtSkjema = UtsendtArbeidstakerSkjema(skjema, objectMapper)
                    utsendtSkjema.metadata.representasjonstype == Representasjonstype.DEG_SELV
                }
            }

            Representasjonstype.ARBEIDSGIVER -> {
                // Arbeidsgiver - søknader for alle arbeidsgivere bruker har tilgang til
                val tilganger = altinnService.hentBrukersTilganger()
                val tilgangOrgnr = tilganger.map { it.orgnr }.toSet()

                // Hent alle utkast opprettet av bruker og filtrer på tilganger
                skjemaRepository.findByOpprettetAvAndStatus(
                    innloggetBrukerFnr,
                    SkjemaStatus.UTKAST
                ).filter { skjema ->
                    skjema.orgnr != null && tilgangOrgnr.contains(skjema.orgnr)
                }
            }

            Representasjonstype.RADGIVER -> {
                // Rådgiver - kun utkast for det spesifikke rådgiverfirmaet
                val radgiverfirmaOrgnr = request.radgiverfirmaOrgnr
                    ?: throw IllegalArgumentException("radgiverfirmaOrgnr er påkrevd for RADGIVER")

                // Hent utkast opprettet av innlogget bruker som tilhører rådgiverfirmaet
                skjemaRepository.findByOpprettetAvAndStatus(
                    innloggetBrukerFnr,
                    SkjemaStatus.UTKAST
                ).filter { skjema ->
                    // Sjekk at skjemaet har metadata med riktig rådgiverfirma
                    val utsendtSkjema = UtsendtArbeidstakerSkjema(skjema, objectMapper)
                    utsendtSkjema.metadata.radgiverfirma?.orgnr == radgiverfirmaOrgnr
                }
            }

            Representasjonstype.ANNEN_PERSON -> {
                // Fullmektig på vegne av alle personer bruker har fullmakt for
                // Hent alle personer innlogget bruker har fullmakt for
                val fullmakter = try {
                    reprService.hentKanRepresentere()
                } catch (e: Exception) {
                    log.warn(e) { "Feil ved henting av fullmakter for bruker" }
                    emptyList()
                }

                val personerMedFullmakt = fullmakter.map { it.fullmaktsgiver }.toSet()

                // Hent alle utkast opprettet av innlogget bruker og filtrer på fullmakt
                skjemaRepository.findByOpprettetAvAndStatus(innloggetBrukerFnr, SkjemaStatus.UTKAST)
                    .filter { skjema ->
                    skjema.fnr != null && personerMedFullmakt.contains(skjema.fnr)
                }
            }
        }

        // Konverter til DTO
        val utkastDtos = utkastSkjemaer.map { skjema ->
            konverterTilUtkastDto(skjema)
        }

        log.debug { "Fant ${utkastDtos.size} utkast for representasjonstype ${request.representasjonstype}" }

        return UtkastListeResponse(
            utkast = utkastDtos,
            antall = utkastDtos.size
        )
    }

    /**
     * Konverterer Skjema til UtkastOversiktDto.
     * Maskerer fnr og henter nødvendige metadata-verdier.
     */
    private fun konverterTilUtkastDto(skjema: Skjema): UtkastOversiktDto {
        val utsendtSkjema = UtsendtArbeidstakerSkjema(skjema, objectMapper)
        val metadata = utsendtSkjema.metadata

        return UtkastOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = null, // TODO: Hent fra data-feltet hvis tilgjengelig
            arbeidstakerFnrMaskert = skjema.fnr?.let { maskerFnr(it) },
            opprettetDato = skjema.opprettetDato,
            sistEndretDato = skjema.endretDato,
            status = skjema.status
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
