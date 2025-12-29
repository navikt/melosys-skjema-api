package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.*
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDto
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.event.InnsendingOpprettetEvent
import no.nav.melosys.skjema.exception.AccessDeniedException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional

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
    private val subjectHandler: SubjectHandler,
    private val innsendingStatusService: InnsendingStatusService,
    private val eventPublisher: ApplicationEventPublisher,
    private val referanseIdGenerator: ReferanseIdGenerator
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
     * Henter alle skjemaer hvor innlogget bruker har en eller annen form for tilgang.
     *
     * Inkluderer skjemaer hvor bruker er:
     * - Arbeidstaker selv
     * - Fullmektig for arbeidstaker (med aktiv fullmakt)
     * - Arbeidsgiver/rådgiver (via Altinn-tilgang)
     */
    fun listAlleSkjemaerForBruker(): List<ArbeidstakersSkjemaDto> {
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
            .map { convertToArbeidstakersSkjemaDto(it) }

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
                    val skjemaMetadata = parseMetadata(skjema)
                    skjemaMetadata.representasjonstype == Representasjonstype.DEG_SELV
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
                    // Sjekk at representasjonstype er ARBEIDSGIVER
                    val skjemaMetadata = parseMetadata(skjema)

                    skjemaMetadata.representasjonstype == Representasjonstype.ARBEIDSGIVER &&
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
                    // Sjekk at skjemaet har metadata med riktig rådgiverfirma og representasjonstype
                    val skjemaMetadata = parseMetadata(skjema)

                    skjemaMetadata.representasjonstype == Representasjonstype.RADGIVER &&
                            skjemaMetadata.radgiverfirma?.orgnr == radgiverfirmaOrgnr
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

                val personerMedFullmaktFnr = fullmakter.map { it.fullmaktsgiver }.toSet()

                // Hent alle utkast opprettet av innlogget bruker og filtrer på fullmakt
                skjemaRepository.findByOpprettetAvAndStatus(innloggetBrukerFnr, SkjemaStatus.UTKAST)
                    .filter { skjema ->
                        val skjemaMetadata = parseMetadata(skjema)
                        // Sjekk at representasjonstype er ANNEN_PERSON og at arbeidstaker er i fullmaktslisten
                        skjemaMetadata.representasjonstype == Representasjonstype.ANNEN_PERSON &&
                            skjema.fnr != null && personerMedFullmaktFnr.contains(skjema.fnr)
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

    fun getSkjemaArbeidsgiversDel(skjemaId: UUID): ArbeidsgiversSkjemaDto {
        return convertToArbeidsgiversSkjemaDto(getSkjemaMedTilgangsstyring(skjemaId))
    }

    fun getSkjemaArbeidstakersDel(skjemaId: UUID): ArbeidstakersSkjemaDto {
        return convertToArbeidstakersSkjemaDto(getSkjemaMedTilgangsstyring(skjemaId))
    }



    fun saveVirksomhetInfo(skjemaId: UUID, request: ArbeidsgiverensVirksomhetINorgeDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidsgiverensVirksomhetINorge = request)
        }
    }

    fun saveUtenlandsoppdragInfo(skjemaId: UUID, request: UtenlandsoppdragetDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
    }

    fun saveArbeidstakerLonnInfo(skjemaId: UUID, request: ArbeidstakerensLonnDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidstakerensLonn = request)
        }
    }

    fun saveArbeidsstedIUtlandetInfo(skjemaId: UUID, request: ArbeidsstedIUtlandetDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving arbeidssted i utlandet info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidsstedIUtlandet = request)
        }
    }

    fun saveTilleggsopplysningerInfoAsArbeidsgiver(skjemaId: UUID, request: TilleggsopplysningerDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
    }

    @Transactional
    fun sendInnSkjema(skjemaId: UUID): SubmitSkjemaResponse {
        log.info { "Submitting arbeidsgiver skjema: $skjemaId" }
        val currentUser = subjectHandler.getUserID()

        val skjema = getSkjemaMedTilgangsstyring(skjemaId)

        // 1. Generer referanseId
        val referanseId = referanseIdGenerator.generer()

        // 2. Sett skjema-status til SENDT
        skjema.status = SkjemaStatus.SENDT
        skjema.endretAv = currentUser

        // 3. Lagre skjema
        val savedSkjema = skjemaRepository.save(skjema)

        // 4. Opprett innsending-rad for prosesseringsstatus med referanseId
        innsendingStatusService.opprettInnsending(savedSkjema, referanseId)

        // 5. Publiser event - async prosessering starter ETTER at transaksjonen er committed
        eventPublisher.publishEvent(InnsendingOpprettetEvent(savedSkjema.id!!))

        // 6. Returner kvittering med referanseId
        return SubmitSkjemaResponse(
            skjemaId = savedSkjema.id,
            referanseId = referanseId,
            status = savedSkjema.status
        )
    }

    fun getRepresentasjonstype(skjemaId: UUID): Representasjonstype {
        val skjema = getSkjemaMedTilgangsstyring(skjemaId)

        return parseMetadata(skjema).representasjonstype
    }

    fun saveUtenlandsoppdragetInfoAsArbeidstaker(skjemaId: UUID, request: UtenlandsoppdragetArbeidstakersDelDto): ArbeidstakersSkjemaDto {
        log.info { "Saving utenlandsoppdraget info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
    }

    fun saveArbeidssituasjonInfo(skjemaId: UUID, request: ArbeidssituasjonDto): ArbeidstakersSkjemaDto {
        log.info { "Saving arbeidssituasjon info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidssituasjon = request)
        }
    }

    fun saveSkatteforholdOgInntektInfo(skjemaId: UUID, request: SkatteforholdOgInntektDto): ArbeidstakersSkjemaDto {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(skatteforholdOgInntekt = request)
        }
    }

    fun saveFamiliemedlemmerInfo(skjemaId: UUID, request: FamiliemedlemmerDto): ArbeidstakersSkjemaDto {
        log.info { "Saving familiemedlemmer info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(familiemedlemmer = request)
        }
    }

    fun saveTilleggsopplysningerInfo(skjemaId: UUID, request: TilleggsopplysningerDto): ArbeidstakersSkjemaDto {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
    }

    /**
     * Parser metadata-feltet til en typesafe UtsendtArbeidstakerMetadata.
     * @throws IllegalStateException hvis metadata er null
     */
    private fun parseMetadata(skjema: Skjema): UtsendtArbeidstakerMetadata {
        return objectMapper.treeToValue(
            skjema.metadata ?: error("Metadata mangler for skjema ${skjema.id}"),
            UtsendtArbeidstakerMetadata::class.java
        )
    }

    /**
     * Konverterer Skjema til UtkastOversiktDto.
     * Maskerer fnr og henter nødvendige metadata-verdier.
     */
    private fun konverterTilUtkastDto(skjema: Skjema): UtkastOversiktDto {
        val skjemaMetadata = parseMetadata(skjema)

        return UtkastOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            arbeidsgiverNavn = skjemaMetadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = null, // TODO: Hent fra data-feltet hvis tilgjengelig
            arbeidstakerFnrMaskert = skjema.fnr?.let { maskerFnr(it) },
            opprettetDato = skjema.opprettetDato,
            sistEndretDato = skjema.endretDato,
            status = skjema.status
        )
    }

    fun getSkjemaMedTilgangsstyring(skjemaId: UUID): Skjema {
        val skjema = skjemaRepository.findByIdOrNull(skjemaId)
            ?: throw NoSuchElementException("Skjema with id $skjemaId not found")

        if (skjema.fnr == subjectHandler.getUserID()) {
            return skjema
        }

        val skjemaMetadata = parseMetadata(skjema)

        return when(skjemaMetadata.representasjonstype){
            Representasjonstype.DEG_SELV -> skjema.also {
                if (it.fnr != subjectHandler.getUserID()) {
                    throw AccessDeniedException("Bruker har ikke tilgang")
                }
            }

            Representasjonstype.ARBEIDSGIVER, Representasjonstype.RADGIVER -> skjema.also {
                it.orgnr?.takeIf { orgnr -> altinnService.harBrukerTilgang(orgnr) }
                    ?: throw AccessDeniedException("Bruker har ikke tilgang")
            }

            Representasjonstype.ANNEN_PERSON -> skjema.also {
                skjemaMetadata.fullmektigFnr?.takeIf {
                    it == subjectHandler.getUserID() && reprService.harSkriverettigheterForMedlemskap(skjema.fnr ?: error("Denne skal ikke kunnevære null lenger"))
                }
                    ?: throw AccessDeniedException("Bruker har ikke tilgang")
            }
        }
    }

    private fun updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(
        skjemaId: UUID,
        updateFunction: (ArbeidsgiversSkjemaDataDto) -> ArbeidsgiversSkjemaDataDto
    ): ArbeidsgiversSkjemaDto {
        val skjema = getSkjemaMedTilgangsstyring(skjemaId)

        // Read existing ArbeidsgiversSkjemaDto or create empty one
        val existingDto = convertToArbeidsgiversSkjemaDataDto(skjema.data)

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Convert back to JSON and save
        skjema.data = objectMapper.valueToTree(updatedDto)
        return saveAndConvertToArbeidsgiversSkjemaDto(skjema)
    }

    private fun updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(
        skjemaId: UUID,
        updateFunction: (ArbeidstakersSkjemaDataDto) -> ArbeidstakersSkjemaDataDto
    ): ArbeidstakersSkjemaDto {
        val skjema = getSkjemaMedTilgangsstyring(skjemaId)

        // Read existing ArbeidstakersSkjemaDataDto or create empty one
        val existingDto = convertToArbeidstakersSkjemaDataDto(skjema.data)

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Convert back to JSON and save
        skjema.data = objectMapper.valueToTree(updatedDto)
        return saveAndConvertToArbeidstakersSkjemaDto(skjema)
    }

    private fun saveAndConvertToArbeidsgiversSkjemaDto(skjema: Skjema): ArbeidsgiversSkjemaDto {
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToArbeidsgiversSkjemaDto(savedSkjema)
    }

    private fun saveAndConvertToArbeidstakersSkjemaDto(skjema: Skjema): ArbeidstakersSkjemaDto {
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToArbeidstakersSkjemaDto(savedSkjema)
    }

    private fun convertToArbeidsgiversSkjemaDataDto(data: JsonNode?): ArbeidsgiversSkjemaDataDto {
        return convertDataToDto(data, ArbeidsgiversSkjemaDataDto())
    }

    private fun convertToArbeidstakersSkjemaDataDto(data: JsonNode?): ArbeidstakersSkjemaDataDto {
        return convertDataToDto(data, ArbeidstakersSkjemaDataDto())
    }

    private inline fun <reified T> convertDataToDto(data: JsonNode?, defaultValue: T): T {
        return if (data == null) {
            defaultValue
        } else {
            objectMapper.treeToValue(data, T::class.java)
        }
    }

    private fun convertToArbeidsgiversSkjemaDto(skjema: Skjema): ArbeidsgiversSkjemaDto {
        val data = convertToArbeidsgiversSkjemaDataDto(skjema.data)

        return ArbeidsgiversSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            orgnr = skjema.orgnr ?: error("Skjema orgnr is null"),
            status = skjema.status,
            data = data
        )
    }

    private fun convertToArbeidstakersSkjemaDto(skjema: Skjema): ArbeidstakersSkjemaDto {
        val data = convertToArbeidstakersSkjemaDataDto(skjema.data)

        return ArbeidstakersSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            fnr = skjema.fnr ?: error("Skjema fnr is null"),
            status = skjema.status,
            data = data
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
