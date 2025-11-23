package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.createDefaultMetadata
import no.nav.melosys.skjema.dto.OpprettSoknadMedKontekstRequest
import no.nav.melosys.skjema.dto.PersonDto
import no.nav.melosys.skjema.dto.Representasjonstype
import no.nav.melosys.skjema.dto.SimpleOrganisasjonDto
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class UtsendtArbeidstakerServiceTest : FunSpec({

    val mockRepository = mockk<SkjemaRepository>()
    val mockValidator = mockk<UtsendtArbeidstakerValidator>(relaxed = true)
    val mockAltinnService = mockk<AltinnService>()
    val mockReprService = mockk<ReprService>()
    val mockSubjectHandler = mockk<SubjectHandler>()
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val service = UtsendtArbeidstakerService(
        mockRepository,
        mockValidator,
        mockAltinnService,
        mockReprService,
        objectMapper,
        mockSubjectHandler
    )

    val testArbeidsgiver = SimpleOrganisasjonDto(
        orgnr = "123456789",
        navn = "Test AS"
    )

    val testArbeidstaker = PersonDto(
        fnr = "12345678910",
        etternavn = "Testesen"
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

            val savedSkjema = Skjema(
                id = UUID.randomUUID(),
                status = SkjemaStatus.UTKAST,
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = objectMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.save(any()) } returns savedSkjema

            val response = service.opprettMedKontekst(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request, currentUser) }
            verify { mockRepository.save(any()) }
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

            val savedSkjema = Skjema(
                id = UUID.randomUUID(),
                status = SkjemaStatus.UTKAST,
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = objectMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.save(any()) } returns savedSkjema

            val response = service.opprettMedKontekst(request)

            response.id shouldNotBe null
            response.status shouldBe SkjemaStatus.UTKAST

            verify { mockValidator.validerOpprettelse(request, currentUser) }
            verify { mockRepository.save(any()) }
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

            val savedSkjema = Skjema(
                id = UUID.randomUUID(),
                status = SkjemaStatus.UTKAST,
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = objectMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.save(any()) } returns savedSkjema

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

            val savedSkjema = Skjema(
                id = UUID.randomUUID(),
                status = SkjemaStatus.UTKAST,
                fnr = testArbeidstaker.fnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = objectMapper.createObjectNode(),
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.save(any()) } returns savedSkjema

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

    context("hentSkjema - tilgangskontroll") {
        test("skal godkjenne tilgang for arbeidstaker selv") {
            val currentUser = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = createDefaultMetadata(
                representasjonstype = no.nav.melosys.skjema.dto.Representasjonstype.DEG_SELV,
                harFullmakt = false
            )

            val skjema = Skjema(
                id = skjemaId,
                status = SkjemaStatus.UTKAST,
                fnr = currentUser,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.findByIdOrNull(skjemaId) } returns skjema

            val result = service.hentSkjema(skjemaId)

            result.fnr shouldBe currentUser
        }

        test("skal godkjenne tilgang for fullmektig med aktiv fullmakt") {
            val currentUser = "99999999999"
            val arbeidstakerFnr = "12345678910"
            val skjemaId = UUID.randomUUID()

            // Metadata med fullmektigFnr
            val metadata = createDefaultMetadata(
                representasjonstype = no.nav.melosys.skjema.dto.Representasjonstype.ANNEN_PERSON,
                harFullmakt = true,
                fullmektigFnr = currentUser
            )

            val skjema = Skjema(
                id = skjemaId,
                status = SkjemaStatus.UTKAST,
                fnr = arbeidstakerFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) } returns true

            val result = service.hentSkjema(skjemaId)

            result.fnr shouldBe arbeidstakerFnr
            verify { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) }
        }

        test("skal nekte tilgang for fullmektig uten aktiv fullmakt") {
            val currentUser = "99999999999"
            val arbeidstakerFnr = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = objectMapper.createObjectNode()
            metadata.put("representasjonstype", "ANNEN_PERSON")
            metadata.put("harFullmakt", true)
            metadata.put("fullmektigFnr", currentUser)

            val skjema = Skjema(
                id = skjemaId,
                status = SkjemaStatus.UTKAST,
                fnr = arbeidstakerFnr,
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockReprService.harSkriverettigheterForMedlemskap(arbeidstakerFnr) } returns false

            val exception = shouldThrow<IllegalArgumentException> {
                service.hentSkjema(skjemaId)
            }

            exception.message shouldContain "ikke tilgang"
        }

        test("skal godkjenne tilgang for bruker med Altinn-tilgang") {
            val currentUser = "99999999999"
            val skjemaId = UUID.randomUUID()

            val metadata = objectMapper.createObjectNode()
            metadata.put("representasjonstype", "ARBEIDSGIVER")

            val skjema = Skjema(
                id = skjemaId,
                status = SkjemaStatus.UTKAST,
                fnr = "12345678910",
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true

            val result = service.hentSkjema(skjemaId)

            result.orgnr shouldBe testArbeidsgiver.orgnr
            verify { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) }
        }

        test("skal nekte tilgang for bruker uten noen tilgang") {
            val currentUser = "99999999999"
            val skjemaId = UUID.randomUUID()

            val metadata = objectMapper.createObjectNode()
            metadata.put("representasjonstype", "ARBEIDSGIVER")

            val skjema = Skjema(
                id = skjemaId,
                status = SkjemaStatus.UTKAST,
                fnr = "12345678910",
                orgnr = testArbeidsgiver.orgnr,
                metadata = metadata,
                opprettetAv = currentUser,
                endretAv = currentUser
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.findByIdOrNull(skjemaId) } returns skjema
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns false

            val exception = shouldThrow<IllegalArgumentException> {
                service.hentSkjema(skjemaId)
            }

            exception.message shouldContain "ikke tilgang"
        }

        test("skal feile når skjema ikke finnes") {
            val currentUser = "99999999999"
            val skjemaId = UUID.randomUUID()

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockRepository.findByIdOrNull(skjemaId) } returns null

            val exception = shouldThrow<IllegalArgumentException> {
                service.hentSkjema(skjemaId)
            }

            exception.message shouldContain "finnes ikke"
        }
    }
})
