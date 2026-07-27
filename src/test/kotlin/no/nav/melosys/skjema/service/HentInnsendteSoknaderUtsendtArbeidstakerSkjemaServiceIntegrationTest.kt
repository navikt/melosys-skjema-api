package no.nav.melosys.skjema.service

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.radgiverfirmaInfoMedDefaultVerdier
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.HentInnsendteSoknaderRequest
import no.nav.melosys.skjema.types.MotpartStatus
import no.nav.melosys.skjema.types.felles.OrganisasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.json.JsonMapper

/**
 * Integrasjonstester for HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService.
 *
 * Tester hele flyten fra service til database, inkludert:
 * - Database-paginering
 * - Kontekstbasert filtrering for alle representasjonstyper
 * - In-memory søk og sortering
 * - Edge cases og grensetilfeller
 */
class HentInnsendteSoknaderUtsendtArbeidstakerSkjemaServiceIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var service: HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var koblingService: UtsendtArbeidstakerSkjemaKoblingService

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockkBean
    private lateinit var altinnService: AltinnService

    @MockkBean
    private lateinit var reprService: ReprService

    @MockkBean
    private lateinit var subjectHandler: SubjectHandler

    @BeforeEach
    fun setUp() {
        clearMocks(altinnService, reprService, subjectHandler)
        every { reprService.hentFullmaktsgiverFnr() } returns emptySet()
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
    }

    // ========================================
    // DEG_SELV - Arbeidstaker selv
    // ========================================

    @Test
    @DisplayName("DEG_SELV: Skal hente innsendte søknader for arbeidstaker selv")
    fun `skal hente innsendte søknader for DEG_SELV`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        // Opprett SENDT søknad for brukeren
        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
        val skjema = skjemaMedDefaultVerdier(
            fnr = userFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            metadata = metadata,
            opprettetAv = userFnr
        )
        skjemaRepository.save(skjema)

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.shouldNotBeNull()
        response.totaltAntall shouldBe 1
        response.soknader shouldHaveSize 1
        response.soknader[0].id shouldBe skjema.id
        response.side shouldBe 1
        response.antallPerSide shouldBe 10
    }

    @Test
    @DisplayName("DEG_SELV: Skal returnere tom liste når ingen innsendte søknader finnes")
    fun `skal returnere tom liste når ingen innsendte søknader finnes for DEG_SELV`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        // Opprett kun UTKAST (skal ikke vises)
        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.UTKAST,
                metadata = metadata,
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.shouldNotBeNull()
        response.totaltAntall shouldBe 0
        response.soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("DEG_SELV: Skal ikke inkludere UTKAST status")
    fun `skal ikke inkludere UTKAST status for DEG_SELV`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

        // Opprett SENDT - skal inkluderes
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = userFnr
            )
        )

        // Opprett UTKAST - skal IKKE inkluderes
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.UTKAST,
                metadata = metadata,
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 1
        response.soknader shouldHaveSize 1
    }

    @Test
    @DisplayName("DEG_SELV: Skal ikke returnere søknader fra andre brukere")
    fun `skal ikke returnere søknader fra andre brukere for DEG_SELV`() {
        val userFnr = korrektSyntetiskFnr
        val annenBrukerFnr = etAnnetKorrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

        // Opprett søknad for annen bruker
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = annenBrukerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = annenBrukerFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 0
        response.soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("DEG_SELV: Skal ikke returnere søknader med annen representasjonstype for samme fnr")
    fun `skal ikke returnere søknader med annen representasjonstype for DEG_SELV`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        // Opprett DEG_SELV søknad - skal returneres
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV),
                opprettetAv = userFnr
            )
        )

        // Opprett ARBEIDSGIVER søknad med samme fnr - skal IKKE returneres ved DEG_SELV-forespørsel
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.ARBEIDSGIVER),
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 1
        response.soknader shouldHaveSize 1
    }

    // ========================================
    // ARBEIDSGIVER
    // ========================================

    @Test
    @DisplayName("ARBEIDSGIVER: Skal hente ALLE søknader for arbeidsgivere med Altinn-tilgang")
    fun `skal hente alle søknader for arbeidsgivere med Altinn-tilgang`() {
        val userFnr = korrektSyntetiskFnr
        val orgnr1 = "111222333"
        val orgnr2 = "444555666"
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr1, "Bedrift A AS", "AS"),
            OrganisasjonDto(orgnr2, "Bedrift B AS", "AS")
        )

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER,
            arbeidsgiverNavn = "Bedrift A AS"
        )

        // Opprett søknad opprettet av ANNEN BRUKER for org med tilgang
        // VIKTIG: Skal returneres fordi vi henter ALLE for orgnr, ikke basert på opprettetAv
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr1,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = etAnnetKorrektSyntetiskFnr // Må være gyldig fnr
            )
        )

        // Opprett søknad for org uten tilgang (skal ikke returneres)
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = "999888777",
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 1
        response.soknader shouldHaveSize 1
        response.soknader[0].arbeidsgiverOrgnr shouldBe orgnr1
    }

    @Test
    @DisplayName("ARBEIDSGIVER: Skal returnere tom liste når ingen Altinn-tilganger")
    fun `skal returnere tom liste når ingen Altinn-tilganger for ARBEIDSGIVER`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns emptyList()

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 0
        response.soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("ARBEIDSGIVER: Skal hente søknader for flere organisasjoner")
    fun `skal hente søknader for flere organisasjoner for ARBEIDSGIVER`() {
        val userFnr = korrektSyntetiskFnr
        val orgnr1 = "111222333"
        val orgnr2 = "444555666"
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr1, "Bedrift A AS", "AS"),
            OrganisasjonDto(orgnr2, "Bedrift B AS", "AS")
        )

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.ARBEIDSGIVER)

        // Opprett søknader for begge orgnr
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr1,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = userFnr
            )
        )
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr2,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = korrektSyntetiskFnr // Må være gyldig fnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 2
        response.soknader shouldHaveSize 2
    }

    @Test
    @DisplayName("ARBEIDSGIVER: Skal ikke returnere søknader med annen representasjonstype for samme orgnr")
    fun `skal ikke returnere søknader med annen representasjonstype for ARBEIDSGIVER`() {
        val userFnr = korrektSyntetiskFnr
        val orgnr = "111222333"
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr, "Bedrift A AS", "AS")
        )

        // Opprett ARBEIDSGIVER søknad - skal returneres
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.ARBEIDSGIVER),
                opprettetAv = userFnr
            )
        )

        // Opprett RADGIVER søknad med samme orgnr - skal IKKE returneres ved ARBEIDSGIVER-forespørsel
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.RADGIVER,
                    radgiverfirma = radgiverfirmaInfoMedDefaultVerdier()
                ),
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 1
        response.soknader shouldHaveSize 1
    }

    @Test
    @DisplayName("ARBEIDSGIVER: Skal inkludere ARBEIDSGIVER_MED_FULLMAKT søknader ved ARBEIDSGIVER-forespørsel")
    fun `skal inkludere ARBEIDSGIVER_MED_FULLMAKT søknader ved ARBEIDSGIVER forespørsel`() {
        val userFnr = korrektSyntetiskFnr
        val orgnr = "111222333"
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr, "Bedrift A AS", "AS")
        )

        // Opprett ARBEIDSGIVER søknad
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.ARBEIDSGIVER),
                opprettetAv = userFnr
            )
        )

        // Opprett ARBEIDSGIVER_MED_FULLMAKT søknad - skal OGSÅ returneres
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT),
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 2
        response.soknader shouldHaveSize 2
    }

    // ========================================
    // RADGIVER
    // ========================================

    @Test
    @DisplayName("RADGIVER: Skal hente ALLE søknader for spesifikt rådgiverfirma")
    fun `skal hente alle søknader for spesifikt rådgiverfirma`() {
        val userFnr = korrektSyntetiskFnr
        val radgiverfirmaOrgnr = "987654321"
        val orgnr = "111222333"
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr, "Klient AS", "AS")
        )

        // Opprett metadata med rådgiverfirma
        val metadataRiktigRadgiver = utsendtArbeidstakerMetadataMedDefaultVerdier(
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr=radgiverfirmaOrgnr)
        )

        // Opprett søknad opprettet av ANNEN BRUKER - skal likevel returneres
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                metadata = metadataRiktigRadgiver,
                opprettetAv = korrektSyntetiskFnr // Må være gyldig fnr
            )
        )

        // Opprett søknad med annet rådgiverfirma (skal ikke returneres)
        val metadataFeilRadgiver = utsendtArbeidstakerMetadataMedDefaultVerdier(
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr="111111111")
        )

        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                metadata = metadataFeilRadgiver,
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirmaOrgnr = radgiverfirmaOrgnr
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 1
        response.soknader shouldHaveSize 1
    }

    @Test
    @DisplayName("RADGIVER: Skal feile når radgiverfirmaOrgnr mangler")
    fun `skal feile når radgiverfirmaOrgnr mangler for RADGIVER`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirmaOrgnr = null // Mangler!
        )

        try {
            service.hentInnsendteSoknader(request)
            throw AssertionError("Skulle kastet IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldBe "radgiverfirmaOrgnr er påkrevd for RADGIVER"
        }
    }

    // ========================================
    // ANNEN_PERSON - Fullmektig
    // ========================================

    @Test
    @DisplayName("ANNEN_PERSON: Skal hente ALLE søknader for personer med fullmakt")
    fun `skal hente alle søknader for personer med fullmakt`() {
        val userFnr = korrektSyntetiskFnr
        val fullmaktsgiver1 = etAnnetKorrektSyntetiskFnr
        val fullmaktsgiver2 = "10203040506"
        every { subjectHandler.getUserID() } returns userFnr
        every { reprService.hentKanRepresentere() } returns listOf(
            Fullmakt(
                fullmaktsgiver = fullmaktsgiver1,
                fullmektig = userFnr,
                leserettigheter = listOf("melosys"),
                skriverettigheter = listOf("melosys")
            ),
            Fullmakt(
                fullmaktsgiver = fullmaktsgiver2,
                fullmektig = userFnr,
                leserettigheter = listOf("melosys"),
                skriverettigheter = listOf("melosys")
            )
        )

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
            representasjonstype = Representasjonstype.ANNEN_PERSON,
            fullmektigFnr = userFnr
        )

        // Opprett søknad opprettet av ANNEN FULLMEKTIG - skal likevel returneres
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = fullmaktsgiver1,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = korrektSyntetiskFnr // Må være gyldig fnr
            )
        )

        // Opprett søknad for person uten fullmakt (skal ikke returneres)
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = "99999999999",
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ANNEN_PERSON
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 1
        response.soknader shouldHaveSize 1
        response.soknader[0].arbeidstakerFnrMaskert shouldBe "${fullmaktsgiver1.take(6)}*****"
    }

    @Test
    @DisplayName("ANNEN_PERSON: Skal la repr-api-feil propagere")
    fun `skal la repr-api-feil propagere for ANNEN_PERSON`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr
        every { reprService.hentKanRepresentere() } throws RuntimeException("Repr-API nede")

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ANNEN_PERSON
        )

        shouldThrow<RuntimeException> {
            service.hentInnsendteSoknader(request)
        }
    }

    // ========================================
    // PAGINERING
    // ========================================

    @Test
    @DisplayName("Paginering: Skal returnere riktig side med riktig antall")
    fun `skal returnere riktig side med riktig antall`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

        // Opprett 15 søknader
        repeat(15) {
            skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = userFnr,
                    status = SkjemaStatus.SENDT,
                    metadata = metadata
                )
            )
        }

        // Hent side 1 med 10 per side
        val request1 = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val response1 = service.hentInnsendteSoknader(request1)

        response1.totaltAntall shouldBe 15
        response1.soknader shouldHaveSize 10
        response1.side shouldBe 1
        response1.antallPerSide shouldBe 10

        // Hent side 2 med 10 per side
        val request2 = HentInnsendteSoknaderRequest(
            side = 2,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val response2 = service.hentInnsendteSoknader(request2)

        response2.totaltAntall shouldBe 15
        response2.soknader shouldHaveSize 5 // Kun 5 igjen på side 2
        response2.side shouldBe 2
    }

    @Test
    @DisplayName("Paginering: Skal håndtere tom side 2 når kun 5 resultater")
    fun `skal håndtere tom side 2 når kun 5 resultater`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

        // Opprett kun 5 søknader
        repeat(5) {
            skjemaRepository.save(
                skjemaMedDefaultVerdier(
                    fnr = userFnr,
                    status = SkjemaStatus.SENDT,
                    metadata = metadata
                )
            )
        }

        // Prøv å hente side 2
        val request = HentInnsendteSoknaderRequest(
            side = 2,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )
        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 5
        response.soknader.shouldBeEmpty()
        response.side shouldBe 2
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Edge case: Skal maskere fødselsnummer korrekt")
    fun `skal maskere fødselsnummer korrekt`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.soknader[0].arbeidstakerFnrMaskert shouldBe "${userFnr.take(6)}*****"
        response.soknader[0].arbeidstakerFnrMaskert!!.length shouldBe 11
    }

    @Test
    @DisplayName("Søk: Skal filtrere på orgnr for DEG_SELV")
    fun `skal filtrere på orgnr med søk`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

        // Opprett søknader med forskjellige orgnr
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = "111222333",
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = "444555666",
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = "777888999",
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )

        // Søk på orgnr som matcher kun 1 søknad
        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            sok = "444555", // Matcher kun "444555666"
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.soknader shouldHaveSize 1
        response.soknader[0].arbeidsgiverOrgnr shouldBe "444555666"
        response.totaltAntall shouldBe 1
    }

    @Test
    @DisplayName("Søk: Skal filtrere på referanse-ID for DEG_SELV")
    fun `skal filtrere på referanse-ID med søk`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

        // Opprett søknader med forskjellige orgnr
        val skjema1 = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = "111222333",
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )
        val skjema2 = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = "444555666",
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )

        // Opprett innsendinger med kjente referanse-IDer
        innsendingRepository.save(innsendingMedDefaultVerdier(skjema = skjema1, referanseId = "ABC123"))
        innsendingRepository.save(innsendingMedDefaultVerdier(skjema = skjema2, referanseId = "XYZ789"))

        // Søk på referanse-ID som matcher kun 1 søknad
        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            sok = "XYZ789",
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.soknader shouldHaveSize 1
        response.soknader[0].arbeidsgiverOrgnr shouldBe "444555666"
        response.soknader[0].referanseId shouldBe "XYZ789"
        response.totaltAntall shouldBe 1
    }

    @Test
    @DisplayName("Søk: Skal filtrere på delvis referanse-ID for DEG_SELV")
    fun `skal filtrere på delvis referanse-ID med søk`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

        val skjema1 = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = "111222333",
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )
        val skjema2 = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = "444555666",
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )
        )

        innsendingRepository.save(innsendingMedDefaultVerdier(skjema = skjema1, referanseId = "ABC123"))
        innsendingRepository.save(innsendingMedDefaultVerdier(skjema = skjema2, referanseId = "XYZ789"))

        // Søk på delvis referanse-ID
        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            sok = "xyz", // Case-insensitive delvis match
            representasjonstype = Representasjonstype.DEG_SELV
        )

        val response = service.hentInnsendteSoknader(request)

        response.soknader shouldHaveSize 1
        response.soknader[0].referanseId shouldBe "XYZ789"
        response.totaltAntall shouldBe 1
    }

    // Error handling tests
    // ========================================

    @Test
    @DisplayName("Error: Skal la ReprService-feil propagere ved henting av fullmakter")
    fun `skal la ReprService-feil propagere`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr
        every { reprService.hentKanRepresentere() } throws RuntimeException("Repr service unavailable")

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ANNEN_PERSON
        )

        shouldThrow<RuntimeException> {
            service.hentInnsendteSoknader(request)
        }
    }

    @Test
    @DisplayName("Error: Skal håndtere tom orgnr liste fra Altinn for ARBEIDSGIVER")
    fun `skal håndtere tom orgnr liste fra Altinn for ARBEIDSGIVER`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns emptyList()

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 0
        response.soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("Error: Skal kaste feil når radgiverfirmaOrgnr mangler for RADGIVER")
    fun `skal kaste feil når radgiverfirmaOrgnr mangler for RADGIVER`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.RADGIVER,
            radgiverfirmaOrgnr = null // Mangler!
        )

        try {
            service.hentInnsendteSoknader(request)
            throw AssertionError("Skulle kastet IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldBe "radgiverfirmaOrgnr er påkrevd for RADGIVER"
        }
    }

    // ========================================
    // Saksnummer, saksstatus, skjemadel og motpart-status
    // ========================================

    private fun standardRequestDegSelv() = HentInnsendteSoknaderRequest(
        side = 1,
        antall = 10,
        representasjonstype = Representasjonstype.DEG_SELV
    )

    @Test
    @DisplayName("Saksinfo: Skal populere saksnummer og saksstatus fra innsendingen")
    fun `skal populere saksnummer og saksstatus fra innsendingen`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(skjema = skjema, saksnummer = "MEL-123456", saksstatus = Saksstatus.MOTTATT)
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].saksnummer shouldBe "MEL-123456"
        response.soknader[0].saksstatus shouldBe Saksstatus.MOTTATT
    }

    @Test
    @DisplayName("Saksinfo: saksnummer og saksstatus skal være null når innsendingen mangler dem")
    fun `saksnummer og saksstatus skal være null når innsendingen mangler dem`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(skjema = skjema, saksnummer = null, saksstatus = null)
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].saksnummer shouldBe null
        response.soknader[0].saksstatus shouldBe null
    }

    @Test
    @DisplayName("Saksinfo: Skal populere skjemadel fra metadata")
    fun `skal populere skjemadel fra metadata`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                )
            )
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
    }

    @Test
    @DisplayName("MotpartStatus: VENTER når skjemadel har motpart uten kobling")
    fun `motpartStatus skal være VENTER uten kobling`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    kobletSkjemaId = null
                )
            )
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].motpartStatus shouldBe MotpartStatus.VENTER
    }

    @Test
    @DisplayName("MotpartStatus: VENTER når koblet skjema kun er utkast")
    fun `motpartStatus skal være VENTER når koblet skjema kun er utkast`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        // Motpartens del (AG-del) er kun UTKAST
        val motpartSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.UTKAST,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                ),
                opprettetAv = etAnnetKorrektSyntetiskFnr
            )
        )

        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    kobletSkjemaId = motpartSkjema.id
                )
            )
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].motpartStatus shouldBe MotpartStatus.VENTER
    }

    @Test
    @DisplayName("MotpartStatus: HAR_SENDT når koblet skjema er sendt inn")
    fun `motpartStatus skal være HAR_SENDT når koblet skjema er sendt inn`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        // Motpartens del (AG-del) er SENDT
        val motpartSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                ),
                opprettetAv = etAnnetKorrektSyntetiskFnr
            )
        )

        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    kobletSkjemaId = motpartSkjema.id
                )
            )
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].motpartStatus shouldBe MotpartStatus.HAR_SENDT
    }

    @Test
    @DisplayName("MotpartStatus: IKKE_RELEVANT for kombinert del")
    fun `motpartStatus skal være IKKE_RELEVANT for kombinert del`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                )
            )
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].motpartStatus shouldBe MotpartStatus.IKKE_RELEVANT
    }

    @Test
    @DisplayName("Saksinfo: AVSLUTTET sak uten kobling returnerer både saksstatus og motpartStatus (avsluttet-vinner håndteres i visning)")
    fun `avsluttet sak uten kobling skal returnere både saksstatus og motpartStatus`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        // Motpart sendte via annen kanal (ingen kobling), saken er avsluttet i melosys-api.
        // Backend returnerer VENTER + AVSLUTTET; frontend lar AVSLUTTET vinne i visningen.
        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                )
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(skjema = skjema, saksnummer = "MEL-654321", saksstatus = Saksstatus.AVSLUTTET)
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].saksstatus shouldBe Saksstatus.AVSLUTTET
        response.soknader[0].motpartStatus shouldBe MotpartStatus.VENTER
    }

    @Test
    @DisplayName("Saksinfo: AVSLUTTET sak med sendt kobling returnerer HAR_SENDT og AVSLUTTET")
    fun `avsluttet sak med sendt kobling skal returnere HAR_SENDT og AVSLUTTET`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val motpartSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                ),
                opprettetAv = etAnnetKorrektSyntetiskFnr
            )
        )

        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    kobletSkjemaId = motpartSkjema.id
                )
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(skjema = skjema, saksnummer = "MEL-654321", saksstatus = Saksstatus.AVSLUTTET)
        )

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 1
        response.soknader[0].saksstatus shouldBe Saksstatus.AVSLUTTET
        response.soknader[0].motpartStatus shouldBe MotpartStatus.HAR_SENDT
    }

    // ========================================
    // MotpartStatus ved resubmisjon — erstatter-kjeden følges
    // (produkteier-beslutning 2026-07-27, se utledMotpartStatus)
    // ========================================

    /**
     * Sender inn et skjema slik produksjonsflyten gjør det: lagres med status SENDT og kobles
     * via [UtsendtArbeidstakerSkjemaKoblingService.finnOgKobl] — ingen håndsatt kobling-metadata.
     */
    private fun sendInnOgKobl(representasjonstype: Representasjonstype, skjemadel: Skjemadel): Skjema {
        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = representasjonstype,
                    skjemadel = skjemadel
                ),
                data = when (skjemadel) {
                    Skjemadel.ARBEIDSTAKERS_DEL -> arbeidstakersSkjemaDataDtoMedDefaultVerdier()
                    else -> arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
                        utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier()
                    )
                }
            )
        )
        koblingService.finnOgKobl(skjema)
        return skjema
    }

    @Test
    @DisplayName("MotpartStatus: HAR_SENDT beholdes når motparten sender ny versjon av sin del")
    fun `motpartStatus skal forbli HAR_SENDT når motparten sender ny versjon`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(korrektSyntetiskOrgnr, "Test Arbeidsgiver AS", "AS")
        )

        // 1) Arbeidstakeren (A) sender sin del
        sendInnOgKobl(Representasjonstype.DEG_SELV, Skjemadel.ARBEIDSTAKERS_DEL)

        // 2) Motparten (B) sender arbeidsgivers del v1 → motpart-kobles til A
        sendInnOgKobl(Representasjonstype.ARBEIDSGIVER, Skjemadel.ARBEIDSGIVERS_DEL)
        service.hentInnsendteSoknader(standardRequestDegSelv()).soknader.single().motpartStatus shouldBe MotpartStatus.HAR_SENDT

        // 3) Motparten (B) sender NY VERSJON (v2) — erstatter v1 og overtar koblingen til A,
        //    og koblingsservicen NULLER kobletSkjemaId på v1
        sendInnOgKobl(Representasjonstype.ARBEIDSGIVER, Skjemadel.ARBEIDSGIVERS_DEL)

        // A skal FORTSATT vise HAR_SENDT — motparten har faktisk sendt (v2)
        service.hentInnsendteSoknader(standardRequestDegSelv()).soknader.single().motpartStatus shouldBe MotpartStatus.HAR_SENDT

        // Begge arbeidsgiver-radene viser HAR_SENDT: motparten (A) har sendt, selv om v1
        // mistet koblingen sin da v2 overtok den — erstatter-kjeden følges
        val arbeidsgiverRespons = service.hentInnsendteSoknader(
            HentInnsendteSoknaderRequest(side = 1, antall = 10, representasjonstype = Representasjonstype.ARBEIDSGIVER)
        )
        arbeidsgiverRespons.soknader shouldHaveSize 2
        arbeidsgiverRespons.soknader.forEach { it.motpartStatus shouldBe MotpartStatus.HAR_SENDT }
    }

    @Test
    @DisplayName("MotpartStatus: Sirkulær erstatter-referanse terminerer og gir korrekt status")
    fun `sirkulær erstatter-referanse skal ikke gi evig løkke`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        // Håndsatt korrupt data: to arbeidstaker-versjoner som erstatter hverandre sirkulært.
        // v2 holder koblingen til en SENDT arbeidsgiver-del; v1 har mistet sin.
        val motpartSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                ),
                opprettetAv = etAnnetKorrektSyntetiskFnr
            )
        )
        val v2 = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    kobletSkjemaId = motpartSkjema.id
                )
            )
        )
        val v1 = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    erstatterSkjemaId = v2.id
                )
            )
        )
        // Lukk sirkelen: v2 erstatter v1 som erstatter v2
        v2.metadata = (v2.metadata as UtsendtArbeidstakerMetadata).medErstatterSkjemaId(v1.id)
        skjemaRepository.save(v2)

        val response = service.hentInnsendteSoknader(standardRequestDegSelv())

        response.soknader shouldHaveSize 2
        response.soknader.forEach { it.motpartStatus shouldBe MotpartStatus.HAR_SENDT }
    }

    // ========================================
    // arbeidstakerNavn — lagret i metadata ved opprettelse
    // ========================================

    @Test
    @DisplayName("ARBEIDSGIVER: arbeidstakerNavn skal leses fra metadata")
    fun `arbeidstakerNavn skal leses fra metadata`() {
        val userFnr = korrektSyntetiskFnr
        val arbeidstakerFnr = etAnnetKorrektSyntetiskFnr
        val orgnr = "111222333"
        every { subjectHandler.getUserID() } returns userFnr
        every { altinnService.hentBrukersTilganger() } returns listOf(
            OrganisasjonDto(orgnr, "Bedrift A AS", "AS")
        )

        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = arbeidstakerFnr,
                orgnr = orgnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    arbeidsgiverNavn = "Bedrift A AS",
                    arbeidstakerNavn = "Kurt Sand"
                ),
                opprettetAv = userFnr
            )
        )

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )

        val response = service.hentInnsendteSoknader(request)

        response.soknader shouldHaveSize 1
        response.soknader[0].arbeidstakerNavn shouldBe "Kurt Sand"
    }
}
