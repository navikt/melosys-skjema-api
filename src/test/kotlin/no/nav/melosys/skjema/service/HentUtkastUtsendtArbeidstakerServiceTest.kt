package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.radgiverfirmaInfoMedDefaultVerdier
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.simpleOrganisasjonDtoMedDefaultVerdier
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.HentUtkastRequest
import no.nav.melosys.skjema.types.felles.OrganisasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier

class HentUtkastUtsendtArbeidstakerServiceTest : FunSpec({

    val mockSkjemaRepository = mockk<SkjemaRepository>()
    val mockAltinnService = mockk<AltinnService>()
    val mockReprService = mockk<ReprService>() {
        every { hentFullmaktsgiverFnr() } returns emptySet()
    }
    val mockSubjectHandler = mockk<SubjectHandler>()

    val service = HentUtkastUtsendtArbeidstakerService(
        mockSkjemaRepository,
        mockAltinnService,
        mockReprService,
        mockSubjectHandler
    )

    val testArbeidsgiver = simpleOrganisasjonDtoMedDefaultVerdier(orgnr = "123456789")

    context("hentUtkast") {
        test("skal hente utkast for DEG_SELV") {
            val currentUser = "12345678910"
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()

            val metadata1 = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,

            )
            val metadata2 = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,

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
            every { mockSkjemaRepository.findByFnrAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2)

            val request = HentUtkastRequest(
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

            val metadata1 = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiverNavn = "Bedrift A AS"
            )
            val metadata2 = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
                OrganisasjonDto("111222333", "Bedrift A AS", "AS"),
                OrganisasjonDto("444555666", "Bedrift B AS", "AS")
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockAltinnService.hentBrukersTilganger() } returns altinnTilganger
            every { mockSkjemaRepository.findByOpprettetAvAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2, utkast3)

            val request = HentUtkastRequest(
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

            val metadata1 = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
            val metadata2 = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
            every { mockSkjemaRepository.findByOpprettetAvAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2)

            val request = HentUtkastRequest(
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

            val request = HentUtkastRequest(
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

            val metadata1 = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,

                fullmektigFnr = currentUser
            )
            val metadata2 = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,

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

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockReprService.hentFullmaktsgiverFnr() } returns setOf(person1Fnr, person2Fnr)
            every { mockSkjemaRepository.findByOpprettetAvAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkast1, utkast2, utkast3)

            val request = HentUtkastRequest(
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
            every { mockSkjemaRepository.findByFnrAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns emptyList()

            val request = HentUtkastRequest(
                representasjonstype = Representasjonstype.DEG_SELV
            )

            val response = service.hentUtkast(request)

            response.antall shouldBe 0
            response.utkast.size shouldBe 0
        }

        test("skal maskere fnr i utkast-oversikten") {
            val currentUser = "12345678910"
            val skjemaId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,

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
            every { mockSkjemaRepository.findByFnrAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkast)

            val request = HentUtkastRequest(
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
            val metadataDegSelv = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
            val metadataArbeidsgiver = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
            every { mockSkjemaRepository.findByFnrAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkastDegSelv, utkastArbeidsgiver)

            val request = HentUtkastRequest(
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
            val metadataArbeidsgiver = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
            val metadataDegSelv = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
                OrganisasjonDto("111222333", "Bedrift A AS", "AS")
            )

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockAltinnService.hentBrukersTilganger() } returns altinnTilganger
            every { mockSkjemaRepository.findByOpprettetAvAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkastArbeidsgiver, utkastDegSelv)

            val request = HentUtkastRequest(
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
            val metadataRadgiver = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
            val metadataArbeidsgiver = utsendtArbeidstakerMetadataMedDefaultVerdier(
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
            every { mockSkjemaRepository.findByOpprettetAvAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkastRadgiver, utkastArbeidsgiver)

            val request = HentUtkastRequest(
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
            val metadataAnnenPerson = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,

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
            val metadataDegSelv = utsendtArbeidstakerMetadataMedDefaultVerdier(
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

            every { mockSubjectHandler.getUserID() } returns currentUser
            every { mockReprService.hentFullmaktsgiverFnr() } returns setOf(person1Fnr)
            every { mockSkjemaRepository.findByOpprettetAvAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns listOf(utkastAnnenPerson, utkastDegSelv)

            val request = HentUtkastRequest(
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
            // ReprService.hentFullmaktsgiverFnr() returnerer emptySet() ved feil (intern try/catch)
            every { mockReprService.hentFullmaktsgiverFnr() } returns emptySet()
            every { mockSkjemaRepository.findByOpprettetAvAndTypeAndStatus(currentUser, any(), SkjemaStatus.UTKAST) } returns emptyList()

            val request = HentUtkastRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON
            )

            // Skal returnere tom liste når ingen fullmakter finnes
            val response = service.hentUtkast(request)

            response.antall shouldBe 0
            response.utkast.size shouldBe 0
        }
    }
})
