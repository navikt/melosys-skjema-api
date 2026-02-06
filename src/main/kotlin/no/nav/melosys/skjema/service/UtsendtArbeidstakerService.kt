package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.config.observability.MDCOperations
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.event.InnsendingOpprettetEvent
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.exception.SkjemaAlleredeSendtException
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.HentUtkastRequest
import no.nav.melosys.skjema.types.InnsendtSkjemaResponse
import no.nav.melosys.skjema.types.OpprettSoknadMedKontekstRequest
import no.nav.melosys.skjema.types.OpprettSoknadMedKontekstResponse
import no.nav.melosys.skjema.types.AnnenPersonMetadata
import no.nav.melosys.skjema.types.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.RadgiverMetadata
import no.nav.melosys.skjema.types.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.RadgiverfirmaInfo
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.SkjemaInnsendtKvittering
import no.nav.melosys.skjema.types.UtkastListeResponse
import no.nav.melosys.skjema.types.UtkastOversiktDto
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Service for håndtering av Utsendt Arbeidstaker søknader.
 * Inneholder all forretningslogikk for opprettelse, henting og tilgangskontroll.
 */
@Service
class UtsendtArbeidstakerService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val validator: UtsendtArbeidstakerValidator,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val eregService: EregService,
    private val skjemaKoblingService: SkjemaKoblingService,
    private val subjectHandler: SubjectHandler,
    private val innsendingService: InnsendingService,
    private val eventPublisher: ApplicationEventPublisher,
    private val referanseIdGenerator: ReferanseIdGenerator,
    private val skjemaDefinisjonService: SkjemaDefinisjonService
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
        validator.validerOpprettelse(request)

        // Hent juridisk enhet fra Enhetsregisteret for kobling av separate søknader
        val juridiskEnhetOrgnr = hentJuridiskEnhetOrgnr(request.arbeidsgiver.orgnr)

        // Bygg metadata med korrekt fullmektig-logikk og juridisk enhet
        val metadata = byggMetadata(request, innloggetBrukerFnr, juridiskEnhetOrgnr)

        // Opprett skjema med riktig fnr og orgnr basert på representasjonstype
        val skjema = when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> {
                // Arbeidstaker fyller ut for seg selv
                Skjema(
                    status = SkjemaStatus.UTKAST,
                    fnr = innloggetBrukerFnr,
                    orgnr = request.arbeidsgiver.orgnr,
                    metadata = metadata,
                    opprettetAv = innloggetBrukerFnr,
                    endretAv = innloggetBrukerFnr
                )
            }

            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
            Representasjonstype.RADGIVER,
            Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                // Arbeidsgiver eller rådgiver fyller ut på vegne av arbeidstaker
                Skjema(
                    status = SkjemaStatus.UTKAST,
                    orgnr = request.arbeidsgiver.orgnr,
                    fnr = request.arbeidstaker.fnr,
                    metadata = metadata,
                    opprettetAv = innloggetBrukerFnr,
                    endretAv = innloggetBrukerFnr
                )
            }

            Representasjonstype.ANNEN_PERSON -> {
                // Fullmektig fyller ut på vegne av arbeidstaker
                Skjema(
                    status = SkjemaStatus.UTKAST,
                    fnr = request.arbeidstaker.fnr,
                    orgnr = request.arbeidsgiver.orgnr,
                    metadata = metadata,
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
    fun listAlleSkjemaerForBruker(): List<UtsendtArbeidstakerSkjemaDto> {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.debug { "Lister alle skjemaer for bruker" }

        // 1. Skjemaer hvor bruker er arbeidstaker
        val somArbeidstaker = skjemaRepository.findByFnr(innloggetBrukerFnr)

        // 2. Skjemaer hvor bruker er fullmektig (må verifisere aktiv fullmakt)
        val somFullmektig = skjemaRepository.findByFullmektigFnr(innloggetBrukerFnr)
            .filter { skjema ->
                try {
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
            .map { convertToSkjemaDto(it, it.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto ?: UtsendtArbeidstakerArbeidstakersSkjemaDataDto()) }

        log.debug { "Fant ${alleSkjemaer.size} skjemaer for bruker (arbeidstaker: ${somArbeidstaker.size}, fullmektig: ${somFullmektig.size}, arbeidsgiver: ${somArbeidsgiver.size})" }

        return alleSkjemaer
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
                    val skjemaMetadata = skjema.metadata as UtsendtArbeidstakerMetadata
                    skjemaMetadata.representasjonstype == Representasjonstype.DEG_SELV
                }
            }

            Representasjonstype.ARBEIDSGIVER, Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> {
                // Arbeidsgiver - søknader for alle arbeidsgivere bruker har tilgang til
                val tilganger = altinnService.hentBrukersTilganger()
                val tilgangOrgnr = tilganger.map { it.orgnr }.toSet()

                // Hent alle utkast opprettet av bruker og filtrer på tilganger
                skjemaRepository.findByOpprettetAvAndStatus(
                    innloggetBrukerFnr,
                    SkjemaStatus.UTKAST
                ).filter { skjema ->
                    // Sjekk at representasjonstype er ARBEIDSGIVER eller ARBEIDSGIVER_MED_FULLMAKT
                    val skjemaMetadata = skjema.metadata as UtsendtArbeidstakerMetadata

                    skjemaMetadata.representasjonstype == request.representasjonstype && tilgangOrgnr.contains(skjema.orgnr)
                }
            }

            Representasjonstype.RADGIVER, Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                // Rådgiver - kun utkast for det spesifikke rådgiverfirmaet
                val radgiverfirmaOrgnr = request.radgiverfirmaOrgnr
                    ?: throw IllegalArgumentException("radgiverfirmaOrgnr er påkrevd for RADGIVER")

                // Hent utkast opprettet av innlogget bruker som tilhører rådgiverfirmaet
                skjemaRepository.findByOpprettetAvAndStatus(
                    innloggetBrukerFnr,
                    SkjemaStatus.UTKAST
                ).filter { skjema ->
                    // Sjekk at skjemaet har metadata med riktig rådgiverfirma og representasjonstype
                    val skjemaMetadata = skjema.metadata as? UtsendtArbeidstakerMetadata
                        ?: return@filter false

                    skjemaMetadata.representasjonstype == request.representasjonstype &&
                        when (skjemaMetadata) {
                            is RadgiverMetadata -> skjemaMetadata.radgiverfirma.orgnr == radgiverfirmaOrgnr
                            is RadgiverMedFullmaktMetadata -> skjemaMetadata.radgiverfirma.orgnr == radgiverfirmaOrgnr
                            else -> false
                        }
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
                        val skjemaMetadata = skjema.metadata as UtsendtArbeidstakerMetadata
                        // Sjekk at representasjonstype er ANNEN_PERSON og at arbeidstaker er i fullmaktslisten
                        skjemaMetadata.representasjonstype == Representasjonstype.ANNEN_PERSON && personerMedFullmaktFnr.contains(skjema.fnr)
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

    fun getSkjemaArbeidsgiversDel(skjemaId: UUID): UtsendtArbeidstakerSkjemaDto {
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)
        val data = skjema.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ?: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        return convertToSkjemaDto(skjema, data)
    }

    fun getSkjemaArbeidstakersDel(skjemaId: UUID): UtsendtArbeidstakerSkjemaDto {
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)
        val data = skjema.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto ?: UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
        return convertToSkjemaDto(skjema, data)
    }



    fun saveVirksomhetInfo(skjemaId: UUID, request: ArbeidsgiverensVirksomhetINorgeDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidsgiverensVirksomhetINorge = request)
        }
    }

    fun saveUtenlandsoppdragInfo(skjemaId: UUID, request: UtenlandsoppdragetDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
    }

    fun saveArbeidstakerLonnInfo(skjemaId: UUID, request: ArbeidstakerensLonnDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidstakerensLonn = request)
        }
    }

    fun saveArbeidsstedIUtlandetInfo(skjemaId: UUID, request: ArbeidsstedIUtlandetDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving arbeidssted i utlandet info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidsstedIUtlandet = request)
        }
    }

    fun saveTilleggsopplysningerInfoAsArbeidsgiver(skjemaId: UUID, request: TilleggsopplysningerDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
    }

    @Transactional
    fun sendInnSkjema(skjemaId: UUID, sprak: Språk = Språk.NORSK_BOKMAL): SkjemaInnsendtKvittering {
        log.info { "Submitting arbeidsgiver skjema: $skjemaId" }
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)

        if (skjema.status != SkjemaStatus.UTKAST) {
            throw SkjemaAlleredeSendtException()
        }

        // TODO: Her må det valideres at skjemaet er komplett utfyllt med gyldige data

        // 1. Generer referanseId og hent aktiv versjon
        val referanseId = referanseIdGenerator.generer()
        val aktivVersjon = skjemaDefinisjonService.hentAktivVersjon(skjema.type)

        // 2. Sett skjema-status til SENDT
        skjema.status = SkjemaStatus.SENDT
        skjema.endretAv = subjectHandler.getUserID()

        // 3. Lagre skjema
        val savedSkjema = skjemaRepository.save(skjema)

        // 4. Koble med erstatter (forrige versjon) og/eller motpart
        val koblingsResultat = skjemaKoblingService.finnOgKobl(savedSkjema)
        if (koblingsResultat.erstatterSkjemaId != null) {
            log.info { "Skjema $skjemaId erstatter ${koblingsResultat.erstatterSkjemaId}" }
        }
        if (koblingsResultat.kobletSkjemaId != null) {
            log.info { "Skjema $skjemaId koblet med motpart ${koblingsResultat.kobletSkjemaId}" }
        }

        // 5. Opprett innsending-rad med versjon og språk
        innsendingService.opprettInnsending(
            skjema = savedSkjema,
            referanseId = referanseId,
            skjemaDefinisjonVersjon = aktivVersjon,
            innsendtSprak = sprak
        )

        // 6. Publiser event - async prosessering starter ETTER at transaksjonen er committed
        eventPublisher.publishEvent(
            InnsendingOpprettetEvent(
                skjemaId = savedSkjema.id!!,
                correlationId = MDCOperations.getCorrelationId()
            )
        )

        log.info { "Skjema $skjemaId sendt inn med versjon=$aktivVersjon, språk=${sprak.kode}, referanseId=$referanseId" }

        // 7. Returner kvittering med referanseId
        return SkjemaInnsendtKvittering(
            skjemaId = savedSkjema.id,
            referanseId = referanseId,
            status = savedSkjema.status
        )
    }

    fun genererInnsendtKvittering(skjemaId: UUID): SkjemaInnsendtKvittering {
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)

        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: throw NoSuchElementException("Innsending for skjema $skjemaId finnes ikke")

        return SkjemaInnsendtKvittering(
            skjemaId,
            innsending.referanseId,
            skjema.status
        )

    }

    fun getSkjemaMetadata(skjemaId: UUID): UtsendtArbeidstakerMetadata{
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)

        return skjema.metadata as UtsendtArbeidstakerMetadata
    }

    /**
     * Henter en innsendt søknad med skjemadefinisjon for visning.
     *
     * @param skjemaId ID til skjemaet
     * @param sprak Ønsket språk for definisjonen (valgfritt - bruker innsendtSpråk som default)
     * @return Innsendt søknad med data og definisjon
     * @throws IllegalStateException hvis skjema ikke er innsendt
     */
    fun hentInnsendtSkjema(skjemaId: UUID, sprak: Språk?): InnsendtSkjemaResponse {
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)

        if (skjema.status != SkjemaStatus.SENDT) {
            throw IllegalStateException("Skjema $skjemaId er ikke innsendt (status: ${skjema.status})")
        }

        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: throw NoSuchElementException("Innsending for skjema $skjemaId finnes ikke")

        // Bruk ønsket språk, eller fall tilbake til innsendtSpråk fra innsending
        val visSprak = sprak ?: innsending.innsendtSprak

        // Hent definisjon for riktig versjon
        val definisjon = skjemaDefinisjonService.hent(
            type = skjema.type,
            versjon = innsending.skjemaDefinisjonVersjon,
            språk = visSprak
        )

        // Parse skjemadata
        val arbeidstakerData = skjema.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto
        val arbeidsgiverData = skjema.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto

        return InnsendtSkjemaResponse(
            skjemaId = skjema.id!!,
            referanseId = innsending.referanseId,
            innsendtDato = skjema.endretDato,
            innsendtSprak = innsending.innsendtSprak,
            skjemaDefinisjonVersjon = innsending.skjemaDefinisjonVersjon,
            arbeidstakerData = arbeidstakerData,
            arbeidsgiverData = arbeidsgiverData,
            definisjon = definisjon
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
    fun hentSkjemaMedTilgangsstyring(skjemaId: UUID): Skjema {
        val skjema = skjemaRepository.findByIdOrNull(skjemaId)
            ?: throw NoSuchElementException("Skjema with id $skjemaId not found")

        return skjema.takeIf { harInnloggetBrukerTilgangTilSkjema(it) }
            ?: throw AccessDeniedException("Innlogget bruker har ikke tilgang til skjema")
    }

    fun saveUtenlandsoppdragetInfoAsArbeidstaker(skjemaId: UUID, request: UtenlandsoppdragetArbeidstakersDelDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving utenlandsoppdraget info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
    }

    fun saveArbeidssituasjonInfo(skjemaId: UUID, request: ArbeidssituasjonDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving arbeidssituasjon info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidssituasjon = request)
        }
    }

    fun saveSkatteforholdOgInntektInfo(skjemaId: UUID, request: SkatteforholdOgInntektDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(skatteforholdOgInntekt = request)
        }
    }

    fun saveFamiliemedlemmerInfo(skjemaId: UUID, request: FamiliemedlemmerDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving familiemedlemmer info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(familiemedlemmer = request)
        }
    }

    fun saveTilleggsopplysningerInfo(skjemaId: UUID, request: TilleggsopplysningerDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToSkjemaDto(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
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
     * @param request Opprettelsesforespørselen
     * @param innloggetBrukerFnr FNR til innlogget bruker
     * @param juridiskEnhetOrgnr Orgnr til juridisk enhet (fra EREG) - brukes for kobling av separate søknader
     */
    private fun byggMetadata(
        request: OpprettSoknadMedKontekstRequest,
        innloggetBrukerFnr: String,
        juridiskEnhetOrgnr: String
    ): UtsendtArbeidstakerMetadata {
        return when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> DegSelvMetadata(
                skjemadel = request.skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )
            Representasjonstype.ARBEIDSGIVER -> ArbeidsgiverMetadata(
                skjemadel = request.skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> ArbeidsgiverMedFullmaktMetadata(
                skjemadel = request.skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                fullmektigFnr = innloggetBrukerFnr
            )
            Representasjonstype.RADGIVER -> {
                val radgiverfirmaInfo = request.radgiverfirma
                    ?: throw IllegalArgumentException("radgiverfirma er påkrevd for RADGIVER")
                RadgiverMetadata(
                    skjemadel = request.skjemadel,
                    arbeidsgiverNavn = request.arbeidsgiver.navn,
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                    radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaInfo.orgnr, navn = radgiverfirmaInfo.navn)
                )
            }
            Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                val radgiverfirmaInfo = request.radgiverfirma
                    ?: throw IllegalArgumentException("radgiverfirma er påkrevd for RADGIVER_MED_FULLMAKT")
                RadgiverMedFullmaktMetadata(
                    skjemadel = request.skjemadel,
                    arbeidsgiverNavn = request.arbeidsgiver.navn,
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                    fullmektigFnr = innloggetBrukerFnr,
                    radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaInfo.orgnr, navn = radgiverfirmaInfo.navn)
                )
            }
            Representasjonstype.ANNEN_PERSON -> AnnenPersonMetadata(
                skjemadel = request.skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                fullmektigFnr = innloggetBrukerFnr
            )
        }
    }

    /**
     * Henter juridisk enhet orgnr fra Enhetsregisteret.
     * Brukes for kobling av separate søknader (arbeidsgiver-del og arbeidstaker-del).
     *
     * @param orgnr Organisasjonsnummer (kan være underenhet)
     * @return Orgnr til juridisk enhet
     * @throws IllegalStateException hvis juridisk enhet ikke kan hentes
     */
    private fun hentJuridiskEnhetOrgnr(orgnr: String): String {
        val organisasjonMedJuridiskEnhet = eregService.hentOrganisasjonMedJuridiskEnhet(orgnr)
        return organisasjonMedJuridiskEnhet.juridiskEnhet.orgnr.also {
            log.info { "Hentet juridisk enhet ${it.take(3)}*** for org ${orgnr.take(3)}***" }
        }
    }

    /**
     * Konverterer Skjema til UtkastOversiktDto.
     * Maskerer fnr og henter nødvendige metadata-verdier.
     */
    private fun konverterTilUtkastDto(skjema: Skjema): UtkastOversiktDto {
        val skjemaMetadata = skjema.metadata as UtsendtArbeidstakerMetadata

        return UtkastOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            arbeidsgiverNavn = skjemaMetadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = null, // TODO: Hent fra data-feltet hvis tilgjengelig
            arbeidstakerFnrMaskert = maskerFnr(skjema.fnr),
            opprettetDato = skjema.opprettetDato,
            sistEndretDato = skjema.endretDato,
            status = skjema.status
        )
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
     * @throws IllegalArgumentException hvis bruker ikke har tilgang
     */
    private fun harInnloggetBrukerTilgangTilSkjema(skjema: Skjema): Boolean {
        if (skjema.fnr == subjectHandler.getUserID()) {
            return true
        }

        val skjemaMetadata = skjema.metadata as UtsendtArbeidstakerMetadata

        return when(skjemaMetadata.representasjonstype){
            Representasjonstype.DEG_SELV -> false

            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.RADGIVER -> {
                altinnService.harBrukerTilgang(skjema.orgnr)
            }

            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> {
                val metadata = skjemaMetadata as ArbeidsgiverMedFullmaktMetadata
                metadata.fullmektigFnr == subjectHandler.getUserID() && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }

            Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                val metadata = skjemaMetadata as RadgiverMedFullmaktMetadata
                metadata.fullmektigFnr == subjectHandler.getUserID() && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }

            Representasjonstype.ANNEN_PERSON -> {
                val annenPersonMetadata = skjemaMetadata as AnnenPersonMetadata
                annenPersonMetadata.fullmektigFnr == subjectHandler.getUserID() && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }
        }
    }

    private fun updateArbeidsgiverSkjemaDataAndConvertToSkjemaDto(
        skjemaId: UUID,
        updateFunction: (UtsendtArbeidstakerArbeidsgiversSkjemaDataDto) -> UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
    ): UtsendtArbeidstakerSkjemaDto {
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)

        // Read existing ArbeidsgiversSkjemaDataDto or create empty one
        val existingDto = skjema.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ?: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Save and convert
        skjema.data = updatedDto
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToSkjemaDto(savedSkjema, updatedDto)
    }

    private fun updateArbeidstakerSkjemaDataAndConvertToSkjemaDto(
        skjemaId: UUID,
        updateFunction: (UtsendtArbeidstakerArbeidstakersSkjemaDataDto) -> UtsendtArbeidstakerArbeidstakersSkjemaDataDto
    ): UtsendtArbeidstakerSkjemaDto {
        val skjema = hentSkjemaMedTilgangsstyring(skjemaId)

        // Read existing ArbeidstakersSkjemaDataDto or create empty one
        val existingDto = skjema.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto ?: UtsendtArbeidstakerArbeidstakersSkjemaDataDto()

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Save and convert
        skjema.data = updatedDto
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToSkjemaDto(savedSkjema, updatedDto)
    }

    private fun convertToSkjemaDto(skjema: Skjema, data: UtsendtArbeidstakerSkjemaData): UtsendtArbeidstakerSkjemaDto {
        return UtsendtArbeidstakerSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            status = skjema.status,
            type = skjema.type,
            fnr = skjema.fnr,
            orgnr = skjema.orgnr,
            metadata = skjema.metadata as UtsendtArbeidstakerMetadata,
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
