package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.observability.MDCOperations
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.event.InnsendingOpprettetEvent
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.exception.SkjemaErIkkeRedigerbartException
import no.nav.melosys.skjema.extensions.tilSkjemadel
import no.nav.melosys.skjema.extensions.toUtsendtArbeidstakerDto
import no.nav.melosys.skjema.extensions.utsendtArbeidstakerMetadataOrThrow
import no.nav.melosys.skjema.extensions.utsendtArbeidstakerSkjemaDataOrEmpty
import no.nav.melosys.skjema.extensions.utsendtArbeidstakerSkjemaDataOrThrow
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.InnsendtSkjemaResponse
import no.nav.melosys.skjema.types.SkjemaInnsendtKvittering
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.*
import no.nav.melosys.skjema.validators.UtsendtArbeidstakerSkjemaDataValidator
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Service for håndtering av Utsendt Arbeidstaker søknader.
 * Inneholder all forretningslogikk for opprettelse, henting og tilgangskontroll.
 */
@Service
class UtsendtArbeidstakerService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val representasjonValidator: UtsendtArbeidstakerRepresentasjonValidator,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val eregService: EregService,
    private val skjemaKoblingService: UtsendtArbeidstakerSkjemaKoblingService,
    private val subjectHandler: SubjectHandler,
    private val innsendingService: InnsendingService,
    private val skjemaDataValidator: UtsendtArbeidstakerSkjemaDataValidator,
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
    fun opprettUtsendtArbeidstakerSoknad(request: OpprettUtsendtArbeidstakerSoknadRequest): OpprettUtsendtArbeidstakerSoknadResponse {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.info { "Oppretter Utsendt Arbeidstaker søknad for representasjonstype: ${request.representasjonstype}" }

        val arbeidstakerNavn = representasjonValidator.validerOpprettelse(request, innloggetBrukerFnr)

        val juridiskEnhetOrgnr = hentJuridiskEnhetOrgnr(request.arbeidsgiver.orgnr)

        val metadata = byggMetadata(request, innloggetBrukerFnr, juridiskEnhetOrgnr, arbeidstakerNavn)

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

        return OpprettUtsendtArbeidstakerSoknadResponse(
            id = savedSkjema.id ?: throw IllegalStateException("Skjema ID var null etter lagring"),
            status = savedSkjema.status
        )
    }

    /**
     * Henter et skjema for visning.
     *
     * For UTKAST: krever skrivetilgang og at innlogget bruker er den som starta utkastet.
     * For SENDT: lenient kontroll via [validerLesetilgangForSendtSkjema] — fullmektig som har mistet
     * fullmakten men fortsatt har Altinn-tilgang får se skjemaet med arbeidstakers data strippet.
     */
    fun hentSkjema(skjemaId: UUID): UtsendtArbeidstakerSkjemaDto {
        val skjema = findAktivByIdOrThrow(skjemaId)
        if (skjema.status != SkjemaStatus.SENDT) {
            return krevSkrivetilgang(skjema).toUtsendtArbeidstakerDto()
        }

        val dto = skjema.toUtsendtArbeidstakerDto()
        return if (validerLesetilgangForSendtSkjema(skjema) == false) {
            dto.copy(data = stripArbeidstakersData(dto.data))
        } else {
            dto
        }
    }

    /**
     * Soft-sletter et skjema med status UTKAST.
     *
     * Validerer skrivetilgang og at skjemaet er i utkast-status.
     * Setter status til SLETTET — skjemaet og tilhørende vedlegg beholdes i databasen.
     *
     * @param skjemaId ID til skjemaet som skal slettes
     * @throws NoSuchElementException hvis skjema ikke finnes
     * @throws AccessDeniedException hvis bruker ikke har skrivetilgang
     * @throws SkjemaErIkkeRedigerbartException hvis skjema allerede er sendt inn
     */
    @Transactional
    fun slettUtkast(skjemaId: UUID) {
        val skjema = hentSkjemaMedSkrivetilgang(skjemaId)

        if (skjema.status != SkjemaStatus.UTKAST) {
            throw SkjemaErIkkeRedigerbartException()
        }

        skjema.status = SkjemaStatus.SLETTET
        skjema.endretAv = subjectHandler.getUserID()
        skjemaRepository.save(skjema)

        log.info { "Utkast slettet: $skjemaId" }
    }


    fun saveArbeidsgiverensVirksomhetINorge(skjemaId: UUID, request: ArbeidsgiverensVirksomhetINorgeDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> dto.copy(arbeidsgiverensVirksomhetINorge = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(arbeidsgiversData = dto.arbeidsgiversData.copy(
                    arbeidsgiverensVirksomhetINorge = request
                ))
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> error("Kan ikke lagre arbeidsgiverens virksomhet på arbeidstakers skjemadel")
            }
        }
    }

    fun saveUtenlandsoppdraget(skjemaId: UUID, request: UtenlandsoppdragetDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> dto.copy(utenlandsoppdraget = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(arbeidsgiversData = dto.arbeidsgiversData.copy(
                    utenlandsoppdraget = request
                ))
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> error("Kan ikke lagre utenlandsoppdraget på arbeidstakers skjemadel")
            }
        }
    }

    fun saveArbeidstakerensLonn(skjemaId: UUID, request: ArbeidstakerensLonnDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> dto.copy(arbeidstakerensLonn = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(arbeidsgiversData = dto.arbeidsgiversData.copy(
                    arbeidstakerensLonn = request
                ))
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> error("Kan ikke lagre arbeidstakerens lønn på arbeidstakers skjemadel")
            }
        }
    }

    fun saveArbeidsstedIUtlandet(skjemaId: UUID, request: ArbeidsstedIUtlandetDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving arbeidssted i utlandet info for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> dto.copy(arbeidsstedIUtlandet = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(arbeidsgiversData = dto.arbeidsgiversData.copy(
                    arbeidsstedIUtlandet = request
                ))
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> error("Kan ikke lagre arbeidssted i utlandet på arbeidstakers skjemadel")
            }
        }
    }

    @Transactional
    fun sendInnSkjema(skjemaId: UUID, sprak: Språk = Språk.NORSK_BOKMAL): SkjemaInnsendtKvittering {
        log.info { "Submitting arbeidsgiver skjema: $skjemaId" }
        val skjema = hentSkjemaMedSkrivetilgang(skjemaId)

        if (skjema.status != SkjemaStatus.UTKAST) {
            throw SkjemaErIkkeRedigerbartException()
        }

        // Valider at skjemaet er komplett utfylt med gyldige data
        val skjemaData = skjema.utsendtArbeidstakerSkjemaDataOrThrow()
        skjemaDataValidator.validateUtsendtArbeidstakerSkjemaData(skjemaData)

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
            innsendtSprak = sprak,
            innsenderFnr = subjectHandler.getUserID()
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
            skjemaId = savedSkjema.id!!,
            referanseId = referanseId,
            status = savedSkjema.status
        )
    }

    fun genererInnsendtKvittering(skjemaId: UUID): SkjemaInnsendtKvittering {
        val skjema = hentSkjemaMedLesetilgang(skjemaId)

        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: throw NoSuchElementException("Innsending for skjema $skjemaId finnes ikke")

        return SkjemaInnsendtKvittering(
            skjemaId,
            innsending.referanseId,
            skjema.status
        )

    }

    fun getSkjemaMetadata(skjemaId: UUID): UtsendtArbeidstakerMetadata {
        val skjema = findAktivByIdOrThrow(skjemaId)
        if (skjema.status == SkjemaStatus.SENDT) {
            validerLesetilgangForSendtSkjema(skjema)
        } else {
            krevSkrivetilgang(skjema)
        }
        return skjema.utsendtArbeidstakerMetadataOrThrow()
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
        val skjema = findAktivByIdOrThrow(skjemaId)

        if (skjema.status != SkjemaStatus.SENDT) {
            throw IllegalStateException("Skjema $skjemaId er ikke innsendt (status: ${skjema.status})")
        }

        val fullmaktAktiv = validerLesetilgangForSendtSkjema(skjema)

        if (fullmaktAktiv == false) {
            log.warn { "Fullmakt tapt for innsendt skjema ${skjema.id}, arbeidstaker-data strippet" }
        }

        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: throw NoSuchElementException("Innsending for skjema $skjemaId finnes ikke")

        val visSprak = sprak ?: innsending.innsendtSprak
        val definisjon = skjemaDefinisjonService.hent(
            type = skjema.type,
            versjon = innsending.skjemaDefinisjonVersjon,
            språk = visSprak
        )

        val skjemaData = skjema.utsendtArbeidstakerSkjemaDataOrThrow()

        return InnsendtSkjemaResponse(
            skjemaId = skjema.id!!,
            referanseId = innsending.referanseId,
            innsendtDato = skjema.endretDato,
            innsendtSprak = innsending.innsendtSprak,
            skjemaDefinisjonVersjon = innsending.skjemaDefinisjonVersjon,
            skjemaData = if (fullmaktAktiv == false) stripArbeidstakersData(skjemaData) else skjemaData,
            definisjon = definisjon,
            fullmaktAktiv = fullmaktAktiv
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
    fun hentSkjemaMedLesetilgang(skjemaId: UUID): Skjema =
        krevLesetilgang(findAktivByIdOrThrow(skjemaId))

    private fun krevLesetilgang(skjema: Skjema): Skjema =
        skjema.takeIf { harInnloggetBrukerLesetilgangTilSkjema(it) }
            ?: throw AccessDeniedException("Innlogget bruker har ikke tilgang til skjema")

    private fun findAktivByIdOrThrow(skjemaId: UUID): Skjema =
        skjemaRepository.findAktivById(skjemaId)
            ?: throw NoSuchElementException("Skjema med id $skjemaId finnes ikke")

    fun saveUtsendingsperiodeOgLand(skjemaId: UUID, request: UtsendingsperiodeOgLandDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving utsendingsperiode og land info for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> dto.copy(utsendingsperiodeOgLand = request)
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> dto.copy(utsendingsperiodeOgLand = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(utsendingsperiodeOgLand = request)
            }
        }
    }

    fun saveArbeidssituasjon(skjemaId: UUID, request: ArbeidssituasjonDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving arbeidssituasjon info for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> dto.copy(arbeidssituasjon = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(arbeidstakersData = dto.arbeidstakersData.copy(
                    arbeidssituasjon = request
                ))
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> error("Kan ikke lagre arbeidssituasjon på arbeidsgivers skjemadel")
            }
        }
    }

    fun saveSkatteforholdOgInntekt(skjemaId: UUID, request: SkatteforholdOgInntektDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> dto.copy(skatteforholdOgInntekt = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(arbeidstakersData = dto.arbeidstakersData.copy(
                    skatteforholdOgInntekt = request
                ))
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> error("Kan ikke lagre skatteforhold og inntekt på arbeidsgivers skjemadel")
            }
        }
    }

    fun saveFamiliemedlemmer(skjemaId: UUID, request: FamiliemedlemmerDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving familiemedlemmer info for skjema: $skjemaId" }

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> dto.copy(familiemedlemmer = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(arbeidstakersData = dto.arbeidstakersData.copy(
                    familiemedlemmer = request
                ))
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> error("Kan ikke lagre familiemedlemmer på arbeidsgivers skjemadel")
            }
        }
    }

    fun saveTilleggsopplysninger(skjemaId: UUID, request: TilleggsopplysningerDto): UtsendtArbeidstakerSkjemaDto {
        log.info { "Saving tilleggsopplysninger for skjema: $skjemaId" }
        skjemaDataValidator.validate(request)

        return updateSkjemaData(skjemaId) { dto ->
            when (dto) {
                is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> dto.copy(tilleggsopplysninger = request)
                is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> dto.copy(tilleggsopplysninger = request)
                is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> dto.copy(tilleggsopplysninger = request)
            }
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
        request: OpprettUtsendtArbeidstakerSoknadRequest,
        innloggetBrukerFnr: String,
        juridiskEnhetOrgnr: String,
        arbeidstakerNavn: String
    ): UtsendtArbeidstakerMetadata {
        val skjemadel = request.representasjonstype.tilSkjemadel()

        return when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> DegSelvMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidstakerNavn = arbeidstakerNavn
            )
            Representasjonstype.ARBEIDSGIVER -> ArbeidsgiverMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidstakerNavn = arbeidstakerNavn
            )
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> ArbeidsgiverMedFullmaktMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                fullmektigFnr = innloggetBrukerFnr,
                arbeidstakerNavn = arbeidstakerNavn
            )
            Representasjonstype.RADGIVER -> {
                val radgiverfirmaInfo = request.radgiverfirma
                    ?: throw IllegalArgumentException("radgiverfirma er påkrevd for RADGIVER")
                RadgiverMetadata(
                    skjemadel = skjemadel,
                    arbeidsgiverNavn = request.arbeidsgiver.navn,
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                    arbeidstakerNavn = arbeidstakerNavn,
                    radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaInfo.orgnr, navn = radgiverfirmaInfo.navn)
                )
            }
            Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                val radgiverfirmaInfo = request.radgiverfirma
                    ?: throw IllegalArgumentException("radgiverfirma er påkrevd for RADGIVER_MED_FULLMAKT")
                RadgiverMedFullmaktMetadata(
                    skjemadel = skjemadel,
                    arbeidsgiverNavn = request.arbeidsgiver.navn,
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                    fullmektigFnr = innloggetBrukerFnr,
                    arbeidstakerNavn = arbeidstakerNavn,
                    radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaInfo.orgnr, navn = radgiverfirmaInfo.navn)
                )
            }
            Representasjonstype.ANNEN_PERSON -> AnnenPersonMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = request.arbeidsgiver.navn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                fullmektigFnr = innloggetBrukerFnr,
                arbeidstakerNavn = arbeidstakerNavn
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
     * Sjekker tilgang og om fullmakt er aktiv for innsendte skjemaer.
     *
     * Returverdien styrer om arbeidstakers data skal strippes hos kalleren:
     * - `null` = strippes ikke (ikke _MED_FULLMAKT, eller bruker er arbeidstakeren selv)
     * - `true` = strippes ikke (aktiv fullmakt)
     * - `false` = strippes (fullmakt tapt, men Altinn-fallback ga tilgang)
     *
     * Kaster AccessDeniedException hvis bruker ikke har tilgang i det hele tatt.
     * For ANNEN_PERSON uten fullmakt: kaster AccessDeniedException (ingen fallback).
     */
    private fun validerLesetilgangForSendtSkjema(skjema: Skjema): Boolean? {
        val currentUser = subjectHandler.getUserID()

        if (skjema.fnr == currentUser) {
            return null
        }

        val skjemaMetadata = skjema.utsendtArbeidstakerMetadataOrThrow()

        return when (skjemaMetadata.representasjonstype) {
            Representasjonstype.DEG_SELV ->
                throw AccessDeniedException("Innlogget bruker har ikke tilgang til skjema")

            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.RADGIVER -> {
                if (altinnService.harBrukerTilgang(skjema.orgnr)) null
                else throw AccessDeniedException("Innlogget bruker har ikke tilgang til skjema")
            }

            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
            Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                if (reprService.harLeserettigheterForMedlemskap(skjema.fnr)) {
                    true
                } else if (altinnService.harBrukerTilgang(skjema.orgnr)) {
                    false
                } else {
                    throw AccessDeniedException("Innlogget bruker har ikke tilgang til skjema")
                }
            }

            Representasjonstype.ANNEN_PERSON -> {
                val metadata = skjemaMetadata as AnnenPersonMetadata
                if (metadata.fullmektigFnr == currentUser &&
                    reprService.harLeserettigheterForMedlemskap(skjema.fnr)
                ) {
                    true
                } else {
                    throw AccessDeniedException("Innlogget bruker har ikke tilgang til skjema")
                }
            }
        }
    }

    private fun stripArbeidstakersData(skjemaData: UtsendtArbeidstakerSkjemaData): UtsendtArbeidstakerSkjemaData {
        return when (skjemaData) {
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto ->
                skjemaData.copy(arbeidstakersData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData())
            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto ->
                UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> skjemaData
        }
    }

    /**
     * Validerer at innlogget bruker har tilgang til skjemaet.
     *
     * For UTKAST: kun den som starta utkastet (opprettetAv-match) i tillegg til rollesjekk —
     * utkast er personlig og deles ikke mellom brukere med samme rolle.
     *
     * For andre statuser: arbeidstaker selv (fnr-match) eller rollesjekk
     * (fullmektig med aktiv fullmakt, eller Altinn-tilgang til arbeidsgiver).
     *
     * VIKTIG: Fullmakt verifiseres ALLTID mot repr-api for å sikre at den fortsatt er aktiv.
     */
    private fun harInnloggetBrukerLesetilgangTilSkjema(skjema: Skjema): Boolean {
        val currentUser = subjectHandler.getUserID()

        if (skjema.status == SkjemaStatus.UTKAST && skjema.opprettetAv != currentUser) {
            return false
        }

        if (skjema.fnr == currentUser) {
            return true
        }

        val skjemaMetadata = skjema.utsendtArbeidstakerMetadataOrThrow()

        return when(skjemaMetadata.representasjonstype){
            Representasjonstype.DEG_SELV -> false

            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.RADGIVER -> {
                altinnService.harBrukerTilgang(skjema.orgnr)
            }

            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> {
                val metadata = skjemaMetadata as ArbeidsgiverMedFullmaktMetadata
                metadata.fullmektigFnr == currentUser && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }

            Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                val metadata = skjemaMetadata as RadgiverMedFullmaktMetadata
                metadata.fullmektigFnr == currentUser && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }

            Representasjonstype.ANNEN_PERSON -> {
                val annenPersonMetadata = skjemaMetadata as AnnenPersonMetadata
                annenPersonMetadata.fullmektigFnr == currentUser && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }
        }
    }

    private fun updateSkjemaData(
        skjemaId: UUID,
        updateFunction: (UtsendtArbeidstakerSkjemaData) -> UtsendtArbeidstakerSkjemaData
    ): UtsendtArbeidstakerSkjemaDto {
        val skjema = hentSkjemaMedSkrivetilgang(skjemaId)
        val existing = skjema.utsendtArbeidstakerSkjemaDataOrEmpty()
        skjema.data = updateFunction(existing)
        return skjemaRepository.save(skjema).toUtsendtArbeidstakerDto()
    }

    /**
     * Henter skjema og verifiserer at innlogget bruker har skrivetilgang.
     *
     * Krever at brukeren har riktig rolle:
     * - DEG_SELV: Kun arbeidstaker selv (fnr-match)
     * - ARBEIDSGIVER/RADGIVER: Kun via Altinn-tilgang til organisasjonen
     * - *_MED_FULLMAKT/ANNEN_PERSON: Kun fullmektig med aktiv fullmakt
     *
     * I tillegg for UTKAST: kun den som starta utkastet kan redigere — utkast er personlig
     * og kan ikke overtas av andre brukere selv om de har samme rolle.
     */
    private fun hentSkjemaMedSkrivetilgang(skjemaId: UUID): Skjema =
        krevSkrivetilgang(findAktivByIdOrThrow(skjemaId))

    private fun krevSkrivetilgang(skjema: Skjema): Skjema {
        val currentUser = subjectHandler.getUserID()

        if (skjema.status == SkjemaStatus.UTKAST && skjema.opprettetAv != currentUser) {
            throw AccessDeniedException("Innlogget bruker har ikke skrivetilgang til skjema")
        }

        val skjemaMetadata = skjema.utsendtArbeidstakerMetadataOrThrow()
        val harRolle = when (skjemaMetadata.representasjonstype) {
            Representasjonstype.DEG_SELV -> skjema.fnr == currentUser

            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.RADGIVER -> altinnService.harBrukerTilgang(skjema.orgnr)

            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> {
                val metadata = skjemaMetadata as ArbeidsgiverMedFullmaktMetadata
                metadata.fullmektigFnr == currentUser && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }

            Representasjonstype.RADGIVER_MED_FULLMAKT -> {
                val metadata = skjemaMetadata as RadgiverMedFullmaktMetadata
                metadata.fullmektigFnr == currentUser && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }

            Representasjonstype.ANNEN_PERSON -> {
                val metadata = skjemaMetadata as AnnenPersonMetadata
                metadata.fullmektigFnr == currentUser && reprService.harSkriverettigheterForMedlemskap(skjema.fnr)
            }
        }

        if (!harRolle) {
            throw AccessDeniedException("Innlogget bruker har ikke skrivetilgang til skjema")
        }

        return skjema
    }



}
