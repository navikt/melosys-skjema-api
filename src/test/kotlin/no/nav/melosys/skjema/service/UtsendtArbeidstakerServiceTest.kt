package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.exception.SkjemaAlleredeSendtException
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.types.felles.OrganisasjonMedJuridiskEnhetDto
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier
import no.nav.melosys.skjema.personDtoMedDefaultVerdier
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.simpleOrganisasjonDtoMedDefaultVerdier
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import no.nav.melosys.skjema.validators.UtsendtArbeidstakerSkjemaDataValidator
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull

class UtsendtArbeidstakerServiceTest : FunSpec({

    val mockSkjemaRepository = mockk<SkjemaRepository>()
    val mockInnsendingRepository = mockk<InnsendingRepository>()
    val mockValidator = mockk<UtsendtArbeidstakerRepresentasjonValidator>(relaxed = true)
    val mockAltinnService = mockk<AltinnService>()
    val mockReprService = mockk<ReprService>() {
        every { hentFullmaktsgiverFnr() } returns emptySet()
    }
    val mockEregService = mockk<EregService>()
    val mockUtsendtArbeidstakerSkjemaKoblingService = mockk<UtsendtArbeidstakerSkjemaKoblingService>()
    val mockSubjectHandler = mockk<SubjectHandler>()
    val innsendingService = mockk<InnsendingService>()
    val mockSkjemaDataValidator = mockk<UtsendtArbeidstakerSkjemaDataValidator>(relaxed = true)
    val eventPublisher = mockk<ApplicationEventPublisher>()
    val referanseIdGenerator = mockk<ReferanseIdGenerator>()
    val mockSkjemaDefinisjonService = mockk<SkjemaDefinisjonService>()

    val service = UtsendtArbeidstakerService(
        mockSkjemaRepository,
        mockInnsendingRepository,
        mockValidator,
        mockAltinnService,
        mockReprService,
        mockEregService,
        mockUtsendtArbeidstakerSkjemaKoblingService,
        mockSubjectHandler,
        innsendingService,
        mockSkjemaDataValidator,
        eventPublisher,
        referanseIdGenerator,
        mockSkjemaDefinisjonService
    )

    val testArbeidsgiver = simpleOrganisasjonDtoMedDefaultVerdier(orgnr = "123456789")
    val testArbeidstaker = personDtoMedDefaultVerdier(fnr = "12345678910")
    val testRadgiverfirma = simpleOrganisasjonDtoMedDefaultVerdier(orgnr = "987654321", navn = "Rådgiver AS")

    beforeTest {
        every { mockSkjemaDefinisjonService.hentAktivVersjon(SkjemaType.UTSENDT_ARBEIDSTAKER) } returns "1"
        // Default: EregService returnerer juridisk enhet
        every { mockEregService.hentOrganisasjonMedJuridiskEnhet(any()) } returns OrganisasjonMedJuridiskEnhetDto(
            organisasjon = simpleOrganisasjonDtoMedDefaultVerdier(),
            juridiskEnhet = simpleOrganisasjonDtoMedDefaultVerdier(orgnr = "999888777", navn = "Juridisk Enhet AS")
        )
        // Default: Ingen kobling
        every { mockUtsendtArbeidstakerSkjemaKoblingService.finnOgKobl(any()) } returns KoblingsResultat(kobletSkjemaId = null, erstatterSkjemaId = null)
    }

    context("opprettUtsendtArbeidstakerSoknad") {
        test("skal opprette skjema for DEG_SELV") {
            val currentUser = "12345678910"
            val request = opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.DEG_SELV),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettUtsendtArbeidstakerSoknad(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request) }
            verify { mockSkjemaRepository.save(any()) }
        }

        test("skal opprette skjema for ARBEIDSGIVER med fullmakt") {
            val currentUser = "99999999999"
            val request = opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettUtsendtArbeidstakerSoknad(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request) }
            verify { mockSkjemaRepository.save(any()) }
        }

        test("skal opprette skjema for RADGIVER") {
            val currentUser = "99999999999"
            val request = opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT,
                radgiverfirma = testRadgiverfirma,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettUtsendtArbeidstakerSoknad(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request) }
        }

        test("skal opprette skjema for ANNEN_PERSON") {
            val currentUser = "99999999999"
            val request = opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            val savedSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(representasjonstype = Representasjonstype.ANNEN_PERSON),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.save(any()) } returns savedSkjema

            val response = service.opprettUtsendtArbeidstakerSoknad(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request) }
        }

        test("skal feile når validering feiler") {
            val currentUser = "99999999999"
            val request = opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockValidator.validerOpprettelse(request) } throws IllegalArgumentException("Validering feilet")

            val exception = shouldThrow<IllegalArgumentException> {
                service.opprettUtsendtArbeidstakerSoknad(request)
            }

            exception.message shouldContain "Validering feilet"
        }
    }

    context("hentSkjemaMedLesetilgang - tilgangskontroll") {
        test("skal godkjenne tilgang for arbeidstaker selv") {
            val currentUser = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                
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

            val result = service.hentSkjemaMedLesetilgang(skjemaId)

            result.fnr shouldBe currentUser
        }

        test("skal godkjenne tilgang for fullmektig med aktiv fullmakt") {
            val currentUser = "99999999999"
            val arbeidstakerFnr = "12345678910"
            val skjemaId = UUID.randomUUID()

            // Metadata med fullmektigFnr
            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(

                representasjonstype = Representasjonstype.ANNEN_PERSON,
                
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

            val result = service.hentSkjemaMedLesetilgang(skjemaId)

            result.fnr shouldBe arbeidstakerFnr
            verify { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) }
        }

        test("skal nekte tilgang for fullmektig uten aktiv fullmakt") {
            val currentUser = "99999999999"
            val arbeidstakerFnr = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                
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
            every { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                service.hentSkjemaMedLesetilgang(skjemaId)
            }

            exception.message shouldContain "ikke tilgang"
        }

        test("skal godkjenne tilgang for bruker med Altinn-tilgang") {
            val currentUser = korrektSyntetiskFnr
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
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

            val result = service.hentSkjemaMedLesetilgang(skjemaId)

            result.orgnr shouldBe testArbeidsgiver.orgnr
            verify { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) }
        }

        test("skal nekte tilgang for bruker uten noen tilgang") {
            val currentUser = korrektSyntetiskFnr
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
                service.hentSkjemaMedLesetilgang(skjemaId)
            }

            exception.message shouldContain "ikke tilgang"
        }

        test("skal feile når skjema ikke finnes") {
            val currentUser = "99999999999"
            val skjemaId = UUID.randomUUID()

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockSkjemaRepository.findByIdOrNull(skjemaId) } returns null

            shouldThrow<NoSuchElementException> {
                service.hentSkjemaMedLesetilgang(skjemaId)
            }
        }
    }

    context("sendInnSkjema") {
        test("skal kaste SkjemaAlleredeSendtException når skjema allerede er sendt") {
            val alleredeSendtSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                status = SkjemaStatus.SENDT,
                fnr = korrektSyntetiskFnr
            )

            every { mockSubjectHandler.getUserID() } returns alleredeSendtSkjema.fnr
            every { mockSkjemaRepository.findByIdOrNull(alleredeSendtSkjema.id!!) } returns alleredeSendtSkjema

            shouldThrow<SkjemaAlleredeSendtException> {
                service.sendInnSkjema(alleredeSendtSkjema.id!!)
            }
        }
    }
})
