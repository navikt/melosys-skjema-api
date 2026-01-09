package no.nav.melosys.skjema.service

import tools.jackson.databind.json.JsonMapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier
import no.nav.melosys.skjema.dto.*
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.radgiverfirmaInfoMedDefaultVerdier
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
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
        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.ARBEIDSGIVER)

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
        val metadataRiktigRadgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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
        val metadataFeilRadgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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
    @DisplayName("ANNEN_PERSON: Skal returnere tom liste når repr-api feiler")
    fun `skal returnere tom liste når repr-api feiler for ANNEN_PERSON`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr
        every { reprService.hentKanRepresentere() } throws RuntimeException("Repr-API nede")

        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ANNEN_PERSON
        )

        val response = service.hentInnsendteSoknader(request)

        response.totaltAntall shouldBe 0
        response.soknader.shouldBeEmpty()
    }

    // ========================================
    // PAGINERING
    // ========================================

    @Test
    @DisplayName("Paginering: Skal returnere riktig side med riktig antall")
    fun `skal returnere riktig side med riktig antall`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
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

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)

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
    @DisplayName("Edge case: Skal returnere harPdf = false")
    fun `skal returnere harPdf false`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV)
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

        response.soknader[0].harPdf shouldBe false // TODO: Skal endres når PDF implementeres
    }

    @Test
    @DisplayName("Edge case: Skal håndtere null verdier i metadata")
    fun `skal håndtere null verdier i metadata`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr

        val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV,
            arbeidsgiverNavn = null // Null arbeidsgiver navn
        )
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = userFnr,
                orgnr = null, // Null orgnr
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

        response.soknader shouldHaveSize 1
        response.soknader[0].arbeidsgiverNavn shouldBe null
        response.soknader[0].arbeidsgiverOrgnr shouldBe null
    }

    // Error handling tests
    // ========================================

    @Test
    @DisplayName("Error: Skal håndtere ReprService feil ved henting av fullmakter")
    fun `skal håndtere ReprService feil gracefully`() {
        val userFnr = korrektSyntetiskFnr
        every { subjectHandler.getUserID() } returns userFnr
        every { reprService.hentKanRepresentere() } throws RuntimeException("Repr service unavailable")

        // Skal returnere tom liste i stedet for å feile
        val request = HentInnsendteSoknaderRequest(
            side = 1,
            antall = 10,
            representasjonstype = Representasjonstype.ANNEN_PERSON
        )

        val response = service.hentInnsendteSoknader(request)

        response.shouldNotBeNull()
        response.totaltAntall shouldBe 0
        response.soknader.shouldBeEmpty()
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
}
