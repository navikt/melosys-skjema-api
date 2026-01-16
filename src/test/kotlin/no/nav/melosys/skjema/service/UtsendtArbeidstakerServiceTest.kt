package no.nav.melosys.skjema.service

import tools.jackson.databind.json.JsonMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier
import no.nav.melosys.skjema.dto.OpprettSoknadMedKontekstRequest
import no.nav.melosys.skjema.dto.PersonDto
import no.nav.melosys.skjema.dto.Representasjonstype
import no.nav.melosys.skjema.dto.SimpleOrganisasjonDto
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.data.repository.findByIdOrNull
import org.springframework.context.ApplicationEventPublisher
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.exception.SkjemaAlleredeSendtException
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.radgiverfirmaInfoMedDefaultVerdier
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier

class UtsendtArbeidstakerServiceTest : FunSpec({

    val mockSkjemaRepository = mockk<SkjemaRepository>()
    val mockInnsendingRepository = mockk<InnsendingRepository>()
    val mockValidator = mockk<UtsendtArbeidstakerValidator>(relaxed = true)
    val mockAltinnService = mockk<AltinnService>()
    val mockReprService = mockk<ReprService>()
    val mockSubjectHandler = mockk<SubjectHandler>()
    val jsonMapper: JsonMapper = JsonMapper.builder().build()
    val innsendingStatusService = mockk<InnsendingStatusService>()
    val eventPublisher = mockk<ApplicationEventPublisher>()
    val referanseIdGenerator = mockk<ReferanseIdGenerator>()

    val service = UtsendtArbeidstakerService(
        mockSkjemaRepository,
        mockInnsendingRepository,
        mockValidator,
        mockAltinnService,
        mockReprService,
        jsonMapper,
        mockSubjectHandler,
        innsendingStatusService,
        eventPublisher,
        referanseIdGenerator
    )

    val testArbeidsgiver = SimpleOrganisasjonDto(
        orgnr = "123456789",
        navn = "Test AS"
    )

    val testArbeidstaker = PersonDto(
        fnr = "12345678910",
        navn = "Test Testesen"
    )

    val testRadgiverfirma = SimpleOrganisasjonDto(
        orgnr = "987654321",
        navn = "Rådgiver AS"
    )

    context("opprettMedKontekst") {
        test("skal opprette skjema for DEG_SELV") {
            val currentUser = "12345678910"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.DEG_SELV,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = jsonMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettMedKontekst(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request, currentUser) }
            verify { mockSkjemaRepository.save(any()) }
        }

        test("skal opprette skjema for ARBEIDSGIVER med fullmakt") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = jsonMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettMedKontekst(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request, currentUser) }
            verify { mockSkjemaRepository.save(any()) }
        }

        test("skal opprette skjema for RADGIVER") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = testRadgiverfirma,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = jsonMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettMedKontekst(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request, currentUser) }
        }

        test("skal opprette skjema for ANNEN_PERSON") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = jsonMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettMedKontekst(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request, currentUser) }
        }

        test("skal feile når validering feiler") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.DEG_SELV,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockValidator.validerOpprettelse(request, currentUser) } throws IllegalArgumentException("Validering feilet")

            val exception = shouldThrow<IllegalArgumentException> {
                service.opprettMedKontekst(request)
            }

            exception.message shouldContain "Validering feilet"
        }
    }

    context("hentSkjemaMedTilgangsstyring - tilgangskontroll") {
        test("skal godkjenne tilgang for arbeidstaker selv") {
            val currentUser = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                harFullmakt = false
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByIdOrNull(skjemaId) } returns skjema

            val result = service.hentSkjemaMedTilgangsstyring(skjemaId)

            result.fnr shouldBe currentUser
        }

        test("skal godkjenne tilgang for fullmektig med aktiv fullmakt") {
            val currentUser = "99999999999"
            val arbeidstakerFnr = "12345678910"
            val skjemaId = UUID.randomUUID()

            // Metadata med fullmektigFnr
            val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                harFullmakt = true,
                fullmektigFnr = currentUser
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) } returns true

            val result = service.hentSkjemaMedTilgangsstyring(skjemaId)

            result.fnr shouldBe arbeidstakerFnr
            verify { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) }
        }

        test("skal nekte tilgang for fullmektig uten aktiv fullmakt") {
            val currentUser = "99999999999"
            val arbeidstakerFnr = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = jsonMapper.createObjectNode()
            metadata.put("representasjonstype", "ANNEN_PERSON")
            metadata.put("harFullmakt", true)
            metadata.put("fullmektigFnr", currentUser)

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                service.hentSkjemaMedTilgangsstyring(skjemaId)
            }

            exception.message shouldContain "ikke tilgang"
        }

        test("skal godkjenne tilgang for bruker med Altinn-tilgang") {
            val currentUser = korrektSyntetiskFnr
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true

            val result = service.hentSkjemaMedTilgangsstyring(skjemaId)

            result.orgnr shouldBe testArbeidsgiver.orgnr
            verify { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) }
        }

        test("skal nekte tilgang for bruker uten noen tilgang") {
            val currentUser = korrektSyntetiskFnr
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = etAnnetKorrektSyntetiskFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                service.hentSkjemaMedTilgangsstyring(skjemaId)
            }

            exception.message shouldContain "ikke tilgang"
        }

        test("skal feile når skjema ikke finnes") {
            val currentUser = "99999999999"
            val skjemaId = UUID.randomUUID()

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByIdOrNull(skjemaId) } returns null

            shouldThrow<NoSuchElementException> {
                service.hentSkjemaMedTilgangsstyring(skjemaId)
            }
        }
    }

    context("hentUtkast") {
        test("skal hente utkast for DEG_SELV") {
            val currentUser = "12345678910"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            val metadata1 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                harFullmakt = false
            )
            val metadata2 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                harFullmakt = false
            )

            val utkast1 = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata1,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            val utkast2 = skjemaMedDefaultVerdier(
                id = skjemaId2,
                fnr = currentUser,
                orgnr = "999888777",
                metadata = metadata2,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByFnrAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.DEG_SELV
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 2
            response.utkast.size shouldBe 2
            response.utkast[0].id shouldBe skjemaId1
            response.utkast[1].id shouldBe skjemaId2
        }

        test("skal hente utkast for ARBEIDSGIVER basert på Altinn-tilganger") {
            val currentUser = "99999999999"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            val metadata1 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiverNavn = "Bedrift A AS"
            )
            val metadata2 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiverNavn = "Bedrift B AS"
            )

            // Utkast for to forskjellige bedrifter
            val utkast1 = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = "12345678910",
                orgnr = "111222333",
                metadata = metadata1,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            val utkast2 = skjemaMedDefaultVerdier(
                id = skjemaId2,
                fnr = "10987654321",
                orgnr = "444555666",
                metadata = metadata2,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            // Utkast for bedrift uten Altinn-tilgang (skal ikke vises)
            val utkast3 = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = "11111111111",
                orgnr = "777888999",
                metadata = metadata2,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            val altinnTilganger = listOf(
                no.nav.melosys.skjema.dto.OrganisasjonDto("111222333", "Bedrift A AS", "AS"),
                no.nav.melosys.skjema.dto.OrganisasjonDto("444555666", "Bedrift B AS", "AS")
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockAltinnService.hentBrukersTilganger() } returns altinnTilganger
            every { mockSkjemaRepository.findByOpprettetAvAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2, utkast3)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 2
            response.utkast.size shouldBe 2
            response.utkast.map { it.id } shouldBe listOf(skjemaId1, skjemaId2)
        }

        test("skal hente utkast for RADGIVER basert på spesifikt rådgiverfirma") {
            val currentUser = "99999999999"
            val radgiverfirmaOrgnr = "987654321"
            val skjemaId1 = UUID.randomUUID()

            val metadata1 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = radgiverfirmaOrgnr)
            )

            val utkast1 = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = "12345678910",
                orgnr = "111222333",
                metadata = metadata1,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            // Utkast med annet rådgiverfirma (skal ikke vises)
            val metadata2 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = "111111111")
            )

            val utkast2 = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = "10987654321",
                orgnr = "444555666",
                metadata = metadata2,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByOpprettetAvAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirmaOrgnr = radgiverfirmaOrgnr
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 1
            response.utkast.size shouldBe 1
            response.utkast[0].id shouldBe skjemaId1
        }

        test("skal feile når radgiverfirmaOrgnr mangler for RADGIVER") {
            val currentUser = "99999999999"

            every { mockSubjectHandler.getUserID() } returns currentUser

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirmaOrgnr = null
            )

            val exception = shouldThrow<IllegalArgumentException> {
                service.hentUtkast(request)
            }

            exception.message shouldContain "radgiverfirmaOrgnr"
        }

        test("skal hente utkast for ANNEN_PERSON basert på fullmakter") {
            val currentUser = "99999999999"
            val person1Fnr = "12345678910"
            val person2Fnr = "10987654321"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            val metadata1 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                harFullmakt = true,
                fullmektigFnr = currentUser
            )
            val metadata2 = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                harFullmakt = true,
                fullmektigFnr = currentUser
            )

            val utkast1 = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = person1Fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata1,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            val utkast2 = skjemaMedDefaultVerdier(
                id = skjemaId2,
                fnr = person2Fnr,
                orgnr = "999888777",
                metadata = metadata2,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            // Utkast for person uten fullmakt (skal ikke vises)
            val utkast3 = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = "11111111111",
                orgnr = "777888999",
                metadata = metadata1,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            val fullmakter = listOf(
                no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt(
                    fullmaktsgiver = person1Fnr,
                    fullmektig = currentUser,
                    leserettigheter = listOf("MELOSYS"),
                    skriverettigheter = listOf("MELOSYS")
                ),
                no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt(
                    fullmaktsgiver = person2Fnr,
                    fullmektig = currentUser,
                    leserettigheter = listOf("MELOSYS"),
                    skriverettigheter = listOf("MELOSYS")
                )
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockReprService.hentKanRepresentere() } returns fullmakter
            every { mockSkjemaRepository.findByOpprettetAvAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2, utkast3)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 2
            response.utkast.size shouldBe 2
            // Verifiser at kun utkast for personer med fullmakt returneres
            response.utkast.any { it.id == skjemaId1 } shouldBe true
            response.utkast.any { it.id == skjemaId2 } shouldBe true
        }

        test("skal returnere tom liste når ingen utkast finnes") {
            val currentUser = "12345678910"

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByFnrAndStatus(currentUser, SkjemaStatus.UTKAST) } returns emptyList()

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.DEG_SELV
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 0
            response.utkast.size shouldBe 0
        }

        test("skal maskere fnr i utkast-oversikten") {
            val currentUser = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                harFullmakt = false
            )

            val utkast = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByFnrAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkast)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.DEG_SELV
            )

            val response = service.hentUtkast(request)

            response.utkast[0].arbeidstakerFnrMaskert shouldBe "123456*****"
        }

        test("skal kun returnere utkast med riktig representasjonstype for DEG_SELV") {
            val currentUser = "12345678910"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            // Utkast med DEG_SELV (skal returneres)
            val metadataDegSelv = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV
            )
            val utkastDegSelv = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadataDegSelv,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            // Utkast med ARBEIDSGIVER (skal ikke returneres)
            val metadataArbeidsgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER
            )
            val utkastArbeidsgiver = skjemaMedDefaultVerdier(
                id = skjemaId2,
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadataArbeidsgiver,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByFnrAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkastDegSelv, utkastArbeidsgiver)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.DEG_SELV
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 1
            response.utkast.size shouldBe 1
            response.utkast[0].id shouldBe skjemaId1
        }

        test("skal kun returnere utkast med riktig representasjonstype for ARBEIDSGIVER") {
            val currentUser = "99999999999"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            // Utkast med ARBEIDSGIVER (skal returneres)
            val metadataArbeidsgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER
            )
            val utkastArbeidsgiver = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = "12345678910",
                orgnr = "111222333",
                metadata = metadataArbeidsgiver,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            // Utkast med DEG_SELV (skal ikke returneres)
            val metadataDegSelv = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV
            )
            val utkastDegSelv = skjemaMedDefaultVerdier(
                id = skjemaId2,
                fnr = currentUser,
                orgnr = "111222333",
                metadata = metadataDegSelv,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            val altinnTilganger = listOf(
                no.nav.melosys.skjema.dto.OrganisasjonDto("111222333", "Bedrift A AS", "AS")
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockAltinnService.hentBrukersTilganger() } returns altinnTilganger
            every { mockSkjemaRepository.findByOpprettetAvAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkastArbeidsgiver, utkastDegSelv)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 1
            response.utkast.size shouldBe 1
            response.utkast[0].id shouldBe skjemaId1
        }

        test("skal kun returnere utkast med riktig representasjonstype for RADGIVER") {
            val currentUser = "99999999999"
            val radgiverfirmaOrgnr = "987654321"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            // Utkast med RADGIVER (skal returneres)
            val metadataRadgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = radgiverfirmaOrgnr)
            )
            val utkastRadgiver = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = "12345678910",
                orgnr = "111222333",
                metadata = metadataRadgiver,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            // Utkast med ARBEIDSGIVER (skal ikke returneres)
            val metadataArbeidsgiver = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                radgiverfirma = radgiverfirmaInfoMedDefaultVerdier(orgnr = radgiverfirmaOrgnr)
            )
            val utkastArbeidsgiver = skjemaMedDefaultVerdier(
                id = skjemaId2,
                fnr = "10987654321",
                orgnr = "444555666",
                metadata = metadataArbeidsgiver,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByOpprettetAvAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkastRadgiver, utkastArbeidsgiver)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirmaOrgnr = radgiverfirmaOrgnr
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 1
            response.utkast.size shouldBe 1
            response.utkast[0].id shouldBe skjemaId1
        }

        test("skal kun returnere utkast med riktig representasjonstype for ANNEN_PERSON") {
            val currentUser = "99999999999"
            val person1Fnr = "12345678910"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            // Utkast med ANNEN_PERSON (skal returneres)
            val metadataAnnenPerson = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                harFullmakt = true,
                fullmektigFnr = currentUser
            )
            val utkastAnnenPerson = skjemaMedDefaultVerdier(
                id = skjemaId1,
                fnr = person1Fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadataAnnenPerson,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            // Utkast med DEG_SELV (skal ikke returneres)
            val metadataDegSelv = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV
            )
            val utkastDegSelv = skjemaMedDefaultVerdier(
                id = skjemaId2,
                fnr = person1Fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadataDegSelv,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            val fullmakter = listOf(
                no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt(
                    fullmaktsgiver = person1Fnr,
                    fullmektig = currentUser,
                    leserettigheter = listOf("MELOSYS"),
                    skriverettigheter = listOf("MELOSYS")
                )
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockReprService.hentKanRepresentere() } returns fullmakter
            every { mockSkjemaRepository.findByOpprettetAvAndStatus(currentUser, SkjemaStatus.UTKAST) } returns listOf(utkastAnnenPerson, utkastDegSelv)

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 1
            response.utkast.size shouldBe 1
            response.utkast[0].id shouldBe skjemaId1
        }

        test("skal håndtere feil ved henting av fullmakter for ANNEN_PERSON") {
            val currentUser = "99999999999"

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockReprService.hentKanRepresentere() } throws RuntimeException("Feil fra repr-api")
            every { mockSkjemaRepository.findByOpprettetAvAndStatus(currentUser, SkjemaStatus.UTKAST) } returns emptyList()

            val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON
            )

            // Skal ikke kaste exception, men returnere tom liste
            val response = service.hentUtkast(request)

            response.antall shouldBe 0
            response.utkast.size shouldBe 0
        }
    }

    context("sendInnSkjema") {
        test("skal kaste SkjemaAlleredeSendtException når skjema allerede er sendt") {
            val alleredeSendtSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                status = SkjemaStatus.SENDT,
                fnr = korrektSyntetiskFnr
            )

            every { mockSubjectHandler.getUserID() } returns alleredeSendtSkjema.fnr!!
            every { mockSkjemaRepository.findByIdOrNull(alleredeSendtSkjema.id!!) } returns alleredeSendtSkjema

            shouldThrow<SkjemaAlleredeSendtException> {
                service.sendInnSkjema(alleredeSendtSkjema.id!!)
            }
        }
    }
})
