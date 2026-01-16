package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.dto.OpprettSoknadMedKontekstRequest
import no.nav.melosys.skjema.dto.PersonDto
import no.nav.melosys.skjema.dto.Representasjonstype
import no.nav.melosys.skjema.dto.SimpleOrganisasjonDto
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.integrasjon.repr.ReprService

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

    val testArbeidsgiver = SimpleOrganisasjonDto(
        orgnr = "123456789",
        navn = "Test AS"
    )

    val testArbeidstaker = PersonDto(
        fnr = "12345678910"
    )

    val testArbeidstakerMedNavn = PersonDto(
        fnr = "12345678910",
        navn = "Test Testesen"
    )

    val testRadgiverfirma = SimpleOrganisasjonDto(
        orgnr = "987654321",
        navn = "Rådgiver AS"
    )

    context("DEG_SELV validering") {
        test("skal godkjenne gyldig DEG_SELV request") {
            val currentUser = "12345678910"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.DEG_SELV,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            validator.validerOpprettelse(request, currentUser)

            verify { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) }
        }

        test("skal feile når arbeidsgiver mangler") {
            val currentUser = "12345678910"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.DEG_SELV,
                radgiverfirma = null,
                arbeidsgiver = null,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "Arbeidsgiver må oppgis"
        }

        test("skal feile når arbeidsgiver ikke finnes i EREG") {
            val currentUser = "12345678910"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.DEG_SELV,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns false

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "finnes ikke"
        }

        test("skal feile når harFullmakt er true") {
            val currentUser = "12345678910"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.DEG_SELV,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "harFullmakt kan ikke være true"
        }
    }

    context("ARBEIDSGIVER validering") {
        test("skal godkjenne gyldig ARBEIDSGIVER request med fullmakt") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns true

            validator.validerOpprettelse(request, currentUser)

            verify { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) }
            verify { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) }
        }

        test("skal godkjenne gyldig ARBEIDSGIVER request uten fullmakt") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstakerMedNavn,
                harFullmakt = false
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every { mockPdlService.verifiserOgHentPerson(testArbeidstakerMedNavn.fnr, testArbeidstakerMedNavn.navn!!) } returns Pair(
                "Test Testesen",
                java.time.LocalDate.of(1990, 1, 1)
            )

            validator.validerOpprettelse(request, currentUser)

            verify { mockPdlService.verifiserOgHentPerson(testArbeidstakerMedNavn.fnr, testArbeidstakerMedNavn.navn!!) }
        }

        test("skal feile når bruker ikke har Altinn-tilgang") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "ikke Altinn-tilgang"
        }

        test("skal feile når fullmakt mangler") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "ikke fullmakt"
        }

        test("skal feile når arbeidstaker ikke finnes i PDL (uten fullmakt)") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstakerMedNavn,
                harFullmakt = false
            )

            every { mockAltinnService.harBrukerTilgang(testArbeidsgiver.orgnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true
            every {
                mockPdlService.verifiserOgHentPerson(testArbeidstakerMedNavn.fnr, testArbeidstakerMedNavn.navn!!)
            } throws IllegalArgumentException("Person ikke funnet")

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "finnes ikke eller navn matcher ikke"
        }
    }

    context("RADGIVER validering") {
        test("skal godkjenne gyldig RADGIVER request med fullmakt") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
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

            validator.validerOpprettelse(request, currentUser)

            verify { mockEregService.organisasjonsnummerEksisterer(testRadgiverfirma.orgnr) }
            verify { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) }
        }

        test("skal feile når rådgiverfirma mangler") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "Rådgiverfirma må oppgis"
        }

        test("skal feile når rådgiverfirma ikke finnes i EREG") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.RADGIVER,
                radgiverfirma = testRadgiverfirma,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            every { mockEregService.organisasjonsnummerEksisterer(testRadgiverfirma.orgnr) } returns false

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "Rådgiverfirma"
            exception.message shouldContain "finnes ikke"
        }
    }

    context("ANNEN_PERSON validering") {
        test("skal godkjenne gyldig ANNEN_PERSON request") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            validator.validerOpprettelse(request, currentUser)

            verify { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) }
        }

        test("skal feile når arbeidstaker mangler") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = null,
                harFullmakt = true
            )

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "Arbeidstaker må oppgis"
        }

        test("skal feile når bruker ikke har fullmakt") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = true
            )

            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns false

            val exception = shouldThrow<AccessDeniedException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "ikke fullmakt"
        }

        test("skal feile når harFullmakt er false") {
            val currentUser = "99999999999"
            val request = OpprettSoknadMedKontekstRequest(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                radgiverfirma = null,
                arbeidsgiver = testArbeidsgiver,
                arbeidstaker = testArbeidstaker,
                harFullmakt = false
            )

            every { mockReprService.harSkriverettigheterForMedlemskap(testArbeidstaker.fnr) } returns true
            every { mockEregService.organisasjonsnummerEksisterer(testArbeidsgiver.orgnr) } returns true

            val exception = shouldThrow<IllegalArgumentException> {
                validator.validerOpprettelse(request, currentUser)
            }

            exception.message shouldContain "harFullmakt må være true"
        }
    }
})
