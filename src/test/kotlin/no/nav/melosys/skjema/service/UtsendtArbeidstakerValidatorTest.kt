package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.dto.Representasjonstype
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.opprettSoknadMedKontekstRequestMedDefaultVerdier
import no.nav.melosys.skjema.personDtoMedDefaultVerdier
import no.nav.melosys.skjema.simpleOrganisasjonDtoMedDefaultVerdier

class UtsendtArbeidstakerValidatorTest : FunSpec({

    val mockAltinnService = mockk<AltinnService>()
    val mockReprService = mockk<ReprService>()
    val mockPdlService = mockk<PdlService>()
    val mockEregService = mockk<EregService>()

    val validator = UtsendtArbeidstakerValidator(
        mockAltinnService,
        mockReprService,
        mockPdlService,
        mockEregService
    )

    val testArbeidsgiver = simpleOrganisasjonDtoMedDefaultVerdier(orgnr = "123456789")
    val testArbeidstaker = personDtoMedDefaultVerdier(fnr = "12345678910", etternavn = null)
    val testArbeidstakerMedEtternavn = personDtoMedDefaultVerdier(fnr = "12345678910", etternavn = "Testesen")
    val testRadgiverfirma = simpleOrganisasjonDtoMedDefaultVerdier(orgnr = "987654321", navn = "Rådgiver AS")

    context("DEG_SELV validering") {
        test("skal godkjenne gyldig DEG_SELV request") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            validator.validerOpprettelse(request)

            verify { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) }
        }

        test("skal feile når arbeidsgiver ikke finnes i EREG") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns false

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "finnes ikke"
        }

        test("skal feile når harFullmakt er true") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "harFullmakt kan ikke være true"
        }
    }

    context("ARBEIDSGIVER validering") {
        test("skal godkjenne gyldig ARBEIDSGIVER request med fullmakt") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns true

            validator.validerOpprettelse(request)

            verify { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) }
            verify { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) }
        }

        test("skal godkjenne gyldig ARBEIDSGIVER request uten fullmakt") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstakerMedEtternavn,
                harFullmakt = false
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every { mockPdlService.verifiserOgHentPerson(testArbeidstakerMedEtternavn.fnr, testArbeidstakerMedEtternavn.etternavn!!) } returns Pair(
                "Test Testesen",
                java.time.LocalDate.of(1990, 1, 1)
            )

            validator.validerOpprettelse(request)

            verify { mockPdlService.verifiserOgHentPerson(testArbeidstakerMedEtternavn.fnr, testArbeidstakerMedEtternavn.etternavn!!) }
        }

        test("skal feile når bruker ikke har Altinn-tilgang") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "ikke Altinn-tilgang"
        }

        test("skal feile når fullmakt mangler") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "ikke fullmakt"
        }

        test("skal feile når arbeidstaker ikke finnes i PDL (uten fullmakt)") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstakerMedEtternavn,
                harFullmakt = false
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every {
                mockPdlService.verifiserOgHentPerson(testArbeidstakerMedEtternavn.fnr, testArbeidstakerMedEtternavn.etternavn!!)
            } throws IllegalArgumentException("Person ikke funnet")

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "finnes ikke eller etternavn matcher ikke"
        }
    }

    context("RADGIVER validering") {
        test("skal godkjenne gyldig RADGIVER request med fullmakt") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = testRadgiverfirma,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockEregService.organisasjonsnummerEksisterer(testRadgiverfirma.orgnr) } returns true
            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns true

            validator.validerOpprettelse(request)

            verify { mockEregService.organisasjonsnummerEksisterer(testRadgiverfirma.orgnr) }
            verify { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) }
        }

        test("skal feile når rådgiverfirma mangler") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "Rådgiverfirma må oppgis"
        }

        test("skal feile når rådgiverfirma ikke finnes i EREG") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = testRadgiverfirma,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker
            )

            every { mockEregService.organisasjonsnummerEksisterer(testRadgiverfirma.orgnr) } returns false

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "Rådgiverfirma"
            exception.message shouldContain "finnes ikke"
        }
    }

    context("ANNEN_PERSON validering") {
        test("skal godkjenne gyldig ANNEN_PERSON request") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            validator.validerOpprettelse(request)

            verify { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) }
        }

        test("skal feile når bruker ikke har fullmakt") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "ikke fullmakt"
        }

        test("skal feile når harFullmakt er false") {
            val request = opprettSoknadMedKontekstRequestMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request)
            }

            exception.message shouldContain "harFullmakt må være true"
        }
    }
})
