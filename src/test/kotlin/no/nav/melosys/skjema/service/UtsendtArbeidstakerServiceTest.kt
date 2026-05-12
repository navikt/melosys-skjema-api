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
import no.nav.melosys.skjema.exception.SkjemaErIkkeRedigerbartException
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
import no.nav.melosys.skjema.arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import no.nav.melosys.skjema.validators.UtsendtArbeidstakerSkjemaDataValidator
import org.springframework.context.ApplicationEventPublisher

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

            verify { mockValidator.validerOpprettelse(request, any()) }
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

            verify { mockValidator.validerOpprettelse(request, any()) }
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

            verify { mockValidator.validerOpprettelse(request, any()) }
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

            verify { mockValidator.validerOpprettelse(request, any()) }
        }

        test("skal feile når validering feiler") {
            val currentUser = "99999999999"
            val request = opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockValidator.validerOpprettelse(request, any()) } throws IllegalArgumentException("Validering feilet")

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
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns skjema

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
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns skjema
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
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns skjema
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
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns skjema
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
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns skjema
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
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns null

            shouldThrow<NoSuchElementException> {
                service.hentSkjemaMedLesetilgang(skjemaId)
            }
        }
    }

    context("sendInnSkjema") {
        test("skal kaste SkjemaErIkkeRedigerbartException når skjema allerede er sendt") {
            val alleredeSendtSkjema = skjemaMedDefaultVerdier(
                id = UUID.randomUUID(),
                status = SkjemaStatus.SENDT,
                fnr = korrektSyntetiskFnr
            )

            every { mockSubjectHandler.getUserID() } returns alleredeSendtSkjema.fnr
            every { mockSkjemaRepository.findAktivById(alleredeSendtSkjema.id!!) } returns alleredeSendtSkjema

            shouldThrow<SkjemaErIkkeRedigerbartException> {
                service.sendInnSkjema(alleredeSendtSkjema.id!!)
            }
        }
    }

    context("MELOSYS-8065: SENDT RADGIVER_MED_FULLMAKT-skjema med tapt fullmakt") {
        val fullmektigFnr = "99999999999"
        val arbeidstakerFnr = "12345678910"

        fun radgiverMedFullmaktSendtSkjema(skjemaId: UUID, medData: Boolean = true) = skjemaMedDefaultVerdier(
            id = skjemaId,
            status = SkjemaStatus.SENDT,
            fnr = arbeidstakerFnr,
            orgnr = testArbeidsgiver.orgnr,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT,
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                fullmektigFnr = fullmektigFnr
            ),
            data = if (medData) arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier() else null
        )

        test("hentSkjema: tapt fullmakt + Altinn-tilgang → returnerer skjema med strippet AT-data") {
            val skjemaId = UUID.randomUUID()
            every { mockSubjectHandler.getUserID() } returns fullmektigFnr
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns radgiverMedFullmaktSendtSkjema(skjemaId)
            every { mockReprService.harLeserettigheterForMedlemskap(arbeidstakerFnr) } returns false
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true

            val data = service.hentSkjema(skjemaId).data as UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto

            data.arbeidsgiversData.arbeidsgiverensVirksomhetINorge shouldNotBe null
            data.arbeidstakersData.arbeidssituasjon shouldBe null
            data.arbeidstakersData.skatteforholdOgInntekt shouldBe null
            data.arbeidstakersData.familiemedlemmer shouldBe null
        }

        test("hentSkjema: aktiv fullmakt → returnerer full data") {
            val skjemaId = UUID.randomUUID()
            every { mockSubjectHandler.getUserID() } returns fullmektigFnr
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns radgiverMedFullmaktSendtSkjema(skjemaId)
            every { mockReprService.harLeserettigheterForMedlemskap(arbeidstakerFnr) } returns true

            val data = service.hentSkjema(skjemaId).data as UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto

            data.arbeidstakersData.arbeidssituasjon shouldNotBe null
        }

        test("hentSkjema: tapt fullmakt + ingen Altinn-tilgang → AccessDeniedException") {
            val skjemaId = UUID.randomUUID()
            every { mockSubjectHandler.getUserID() } returns fullmektigFnr
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns radgiverMedFullmaktSendtSkjema(skjemaId, medData = false)
            every { mockReprService.harLeserettigheterForMedlemskap(arbeidstakerFnr) } returns false
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns false

            shouldThrow<AccessDeniedException> { service.hentSkjema(skjemaId) }
        }

        test("getSkjemaMetadata: tapt fullmakt + Altinn-tilgang → returnerer metadata") {
            val skjemaId = UUID.randomUUID()
            every { mockSubjectHandler.getUserID() } returns fullmektigFnr
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns radgiverMedFullmaktSendtSkjema(skjemaId, medData = false)
            every { mockReprService.harLeserettigheterForMedlemskap(arbeidstakerFnr) } returns false
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true

            service.getSkjemaMetadata(skjemaId).representasjonstype shouldBe Representasjonstype.RADGIVER_MED_FULLMAKT
        }

        test("getSkjemaMetadata: tapt fullmakt + ingen Altinn-tilgang → AccessDeniedException") {
            val skjemaId = UUID.randomUUID()
            every { mockSubjectHandler.getUserID() } returns fullmektigFnr
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns radgiverMedFullmaktSendtSkjema(skjemaId, medData = false)
            every { mockReprService.harLeserettigheterForMedlemskap(arbeidstakerFnr) } returns false
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns false

            shouldThrow<AccessDeniedException> { service.getSkjemaMetadata(skjemaId) }
        }

        test("hentSkjema: ARBEIDSGIVER (uten _MED_FULLMAKT) med Altinn-tilgang skal returnere full data uten stripping") {
            val skjemaId = UUID.randomUUID()
            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                status = SkjemaStatus.SENDT,
                fnr = arbeidstakerFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                ),
                data = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()
            )

            every { mockSubjectHandler.getUserID() } returns korrektSyntetiskFnr
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns skjema
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true

            val data = service.hentSkjema(skjemaId).data as UtsendtArbeidstakerArbeidsgiversSkjemaDataDto

            data.arbeidsgiverensVirksomhetINorge shouldNotBe null
        }

        test("hentSkjema: UTKAST med _MED_FULLMAKT uten fullmakt skal kaste AccessDenied (streng kontroll bevart for utkast)") {
            val skjemaId = UUID.randomUUID()
            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                status = SkjemaStatus.UTKAST,
                fnr = arbeidstakerFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT,
                    fullmektigFnr = fullmektigFnr
                ),
                opprettetAv = fullmektigFnr,
                endretAv = fullmektigFnr
            )

            every { mockSubjectHandler.getUserID() } returns fullmektigFnr
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns skjema
            every { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) } returns false

            shouldThrow<AccessDeniedException> { service.hentSkjema(skjemaId) }
        }
    }

    context("UTKAST: kun den som starta utkastet har tilgang (personlig eierskap)") {
        val hrPersonA = "11111111111"
        val hrPersonB = "22222222222"
        val arbeidstakerFnr = "12345678910"

        fun arbeidsgiverUtkastStartetAv(creatorFnr: String, skjemaId: UUID) = skjemaMedDefaultVerdier(
            id = skjemaId,
            status = SkjemaStatus.UTKAST,
            fnr = arbeidstakerFnr,
            orgnr = testArbeidsgiver.orgnr,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            ),
            opprettetAv = creatorFnr,
            endretAv = creatorFnr
        )

        test("hentSkjema: opprinnelig creator med Altinn-tilgang får tilgang til eget utkast") {
            val skjemaId = UUID.randomUUID()
            every { mockSubjectHandler.getUserID() } returns hrPersonA
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns arbeidsgiverUtkastStartetAv(hrPersonA, skjemaId)
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true

            service.hentSkjema(skjemaId).id shouldBe skjemaId
        }

        test("hentSkjema: annen kollega med Altinn-tilgang får IKKE lese andres utkast") {
            val skjemaId = UUID.randomUUID()
            every { mockSubjectHandler.getUserID() } returns hrPersonB
            every { mockSkjemaRepository.findAktivById(skjemaId) } returns arbeidsgiverUtkastStartetAv(hrPersonA, skjemaId)
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true

            shouldThrow<AccessDeniedException> { service.hentSkjema(skjemaId) }
        }
    }
})
