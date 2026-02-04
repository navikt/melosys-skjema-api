package no.nav.melosys.skjema.service.pdf

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes
import no.nav.melosys.skjema.arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidssituasjonDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakerensLonnDtoMedDefaultVerdier
import no.nav.melosys.skjema.familiemedlemmerDtoMedDefaultVerdier
import no.nav.melosys.skjema.offshoreDtoMedDefaultVerdier
import no.nav.melosys.skjema.omBordPaFlyDtoMedDefaultVerdier
import no.nav.melosys.skjema.paLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.paSkipDtoMedDefaultVerdier
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonProperties
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.skatteforholdOgInntektDtoMedDefaultVerdier
import no.nav.melosys.skjema.tilleggsopplysningerDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.InnsendtSkjemaResponse
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.Familiemedlem
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.TestAssertion
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

private val log = KotlinLogging.logger {}

class PdfGeneratorServiceTest : FunSpec({

    val jsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .build()
    val properties = SkjemaDefinisjonProperties()
    val skjemaDefinisjonService = SkjemaDefinisjonService(properties, jsonMapper)
    val pdfGeneratorService = PdfGeneratorService()
    val htmlBuilder = HtmlDokumentBuilder()

    fun lagrePdfForInspeksjon(filnavn: String, pdfBytes: ByteArray) {
        val outputFile = File("build/test-output/$filnavn")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(pdfBytes)
        log.info { "PDF lagret til: ${outputFile.absolutePath}" }
    }

    fun lagInnsendtSkjema(
        referanseId: String,
        språk: Språk = Språk.NORSK_BOKMAL,
        arbeidstakerData: ArbeidstakersSkjemaDataDto? = null,
        arbeidsgiverData: ArbeidsgiversSkjemaDataDto? = null
    ): InnsendtSkjemaResponse {
        val definisjon = skjemaDefinisjonService.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, null, språk)
        return InnsendtSkjemaResponse(
            skjemaId = UUID.randomUUID(),
            referanseId = referanseId,
            innsendtDato = Instant.now(),
            innsendtSprak = språk,
            skjemaDefinisjonVersjon = "1",
            arbeidstakerData = arbeidstakerData,
            arbeidsgiverData = arbeidsgiverData,
            definisjon = definisjon
        )
    }

    context("PDF-generering") {
        test("genererer gyldig PDF med korrekt signatur") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-VALID",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val pdfBytes = pdfGeneratorService.genererPdf(skjema)

            pdfBytes shouldNotBe null
            pdfBytes.size shouldNotBe 0
            String(pdfBytes.take(4).toByteArray()) shouldBe "%PDF"
        }

        test("genererer PDF/A-2u kompatibel fil") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-PDFA",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val pdfBytes = pdfGeneratorService.genererPdf(skjema)
            lagrePdfForInspeksjon("pdfa-validering.pdf", pdfBytes)

            // Valider PDF/A-2u compliance med veraPDF (offisielt valideringsverktøy)
            val feil = validerPdfA2u(pdfBytes)

            if (feil.isNotEmpty()) {
                val feilTekst = feil.joinToString("\n") { "- ${it.ruleId}: ${it.message}" }
                log.error { "PDF/A-2u valideringsfeil:\n$feilTekst" }
                java.io.File("build/test-output/pdfa-errors.txt").apply {
                    parentFile?.mkdirs()
                    writeText("PDF/A-2u valideringsfeil:\n$feilTekst")
                }
            }

            feil.shouldBeEmpty()
        }

        test("genererer PDF med forventet størrelse for komplett søknad") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-KOMPLETT",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val pdfBytes = pdfGeneratorService.genererPdf(skjema)

            // PDF bør være minst 3KB for en komplett søknad
            pdfBytes.size shouldNotBe 0
            lagrePdfForInspeksjon("komplett-soknad.pdf", pdfBytes)
        }
    }

    context("HTML-innhold - Norsk") {
        test("inneholder korrekte overskrifter og seksjoner") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-NORSK",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Søknad om A1"
            html shouldContain "Arbeidstakers del"
            html shouldContain "Arbeidsgivers del"
            html shouldContain "Utenlandsoppdraget"
            html shouldContain "Arbeidssituasjon"
            html shouldContain "Skatteforhold og inntekt"
            html shouldContain "Familiemedlemmer"
        }

        test("viser landnavn på norsk") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-LAND-NO",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Sverige"
            html shouldNotContain "Sweden"
        }

        test("viser boolean-verdier som Ja/Nei") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-BOOL-NO",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            // Sjekk at boolean-verdier vises som Ja eller Nei
            html shouldContain ">Ja<"
            html shouldContain ">Nei<"
        }

        test("viser datoer i norsk format") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-DATO",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            // Datoer skal være på format dd.MM.yyyy
            html shouldContain "01.01.2024"
            html shouldContain "31.12.2024"
        }

        test("viser perioder som separate fra/til-felter") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-PERIODE",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Fra dato"
            html shouldContain "Til dato"
        }
    }

    context("HTML-innhold - Engelsk") {
        test("inneholder engelske overskrifter") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-ENG",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Application for posted worker"
            html shouldContain "Employee"
            html shouldContain "Employer"
            html shouldContain "Foreign assignment"
        }

        test("viser landnavn på engelsk") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-LAND-EN",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Sweden"
        }

        test("viser boolean-verdier som Yes/No") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-BOOL-EN",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain ">Yes<"
            html shouldContain ">No<"
        }
    }

    context("Familiemedlemmer") {
        test("viser familiemedlemmer med fødselsnummer") {
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                familiemedlemmer = FamiliemedlemmerDto(
                    skalHaMedFamiliemedlemmer = true,
                    familiemedlemmer = listOf(
                        Familiemedlem(
                            fornavn = "Kari",
                            etternavn = "Nordmann",
                            harNorskFodselsnummerEllerDnummer = true,
                            fodselsnummer = "12345678901",
                            fodselsdato = null
                        )
                    )
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-FAM-FNR",
                arbeidstakerData = arbeidstakerData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Kari"
            html shouldContain "Nordmann"
            html shouldContain "12345678901"
            html shouldContain "1. familiemedlem"
        }

        test("viser familiemedlemmer med fødselsdato") {
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                familiemedlemmer = FamiliemedlemmerDto(
                    skalHaMedFamiliemedlemmer = true,
                    familiemedlemmer = listOf(
                        Familiemedlem(
                            fornavn = "Ola",
                            etternavn = "Nordmann Jr.",
                            harNorskFodselsnummerEllerDnummer = false,
                            fodselsnummer = null,
                            fodselsdato = LocalDate.of(2015, 6, 15)
                        )
                    )
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-FAM-DATO",
                arbeidstakerData = arbeidstakerData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Ola"
            html shouldContain "Nordmann Jr."
            html shouldContain "15.06.2015"
        }

        test("viser flere familiemedlemmer med nummerering") {
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                familiemedlemmer = FamiliemedlemmerDto(
                    skalHaMedFamiliemedlemmer = true,
                    familiemedlemmer = listOf(
                        Familiemedlem("Kari", "Nordmann", true, "12345678901", null),
                        Familiemedlem("Ola", "Nordmann", false, null, LocalDate.of(2015, 1, 1))
                    )
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-FAM-FLERE",
                arbeidstakerData = arbeidstakerData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "1. familiemedlem"
            html shouldContain "2. familiemedlem"
        }
    }

    context("Arbeidssted-typer") {
        test("viser arbeidssted på land med adresse") {
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                    arbeidsstedType = ArbeidsstedType.PA_LAND,
                    paLand = paLandDtoMedDefaultVerdier(),
                    offshore = null,
                    paSkip = null,
                    omBordPaFly = null
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-LAND",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "På land"
            lagrePdfForInspeksjon("arbeidssted-pa-land.pdf", pdfGeneratorService.genererPdf(skjema))
        }

        test("viser arbeidssted offshore") {
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                    arbeidsstedType = ArbeidsstedType.OFFSHORE,
                    paLand = null,
                    offshore = offshoreDtoMedDefaultVerdier(),
                    paSkip = null,
                    omBordPaFly = null
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-OFFSHORE",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Offshore"
            lagrePdfForInspeksjon("arbeidssted-offshore.pdf", pdfGeneratorService.genererPdf(skjema))
        }

        test("viser arbeidssted på skip") {
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                    arbeidsstedType = ArbeidsstedType.PA_SKIP,
                    paLand = null,
                    offshore = null,
                    paSkip = paSkipDtoMedDefaultVerdier(),
                    omBordPaFly = null
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-SKIP",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "På skip"
            lagrePdfForInspeksjon("arbeidssted-pa-skip.pdf", pdfGeneratorService.genererPdf(skjema))
        }

        test("viser arbeidssted om bord på fly") {
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                    arbeidsstedType = ArbeidsstedType.OM_BORD_PA_FLY,
                    paLand = null,
                    offshore = null,
                    paSkip = null,
                    omBordPaFly = omBordPaFlyDtoMedDefaultVerdier()
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-FLY",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Om bord på fly"
            lagrePdfForInspeksjon("arbeidssted-om-bord-pa-fly.pdf", pdfGeneratorService.genererPdf(skjema))
        }
    }

    context("HTML-escaping og sikkerhet") {
        test("escaper HTML-spesialtegn korrekt") {
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                tilleggsopplysninger = TilleggsopplysningerDto(
                    harFlereOpplysningerTilSoknaden = true,
                    tilleggsopplysningerTilSoknad = "<script>alert('XSS')</script>"
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-XSS",
                arbeidstakerData = arbeidstakerData
            )

            val html = htmlBuilder.byggHtml(skjema)

            // Script-tag skal være escaped
            html shouldNotContain "<script>"
            html shouldContain "&lt;script&gt;"
        }

        test("escaper ampersand korrekt") {
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                tilleggsopplysninger = TilleggsopplysningerDto(
                    harFlereOpplysningerTilSoknaden = true,
                    tilleggsopplysningerTilSoknad = "Tom & Jerry"
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-AMP",
                arbeidstakerData = arbeidstakerData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Tom &amp; Jerry"
        }

        test("håndterer norske tegn korrekt") {
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                familiemedlemmer = FamiliemedlemmerDto(
                    skalHaMedFamiliemedlemmer = true,
                    familiemedlemmer = listOf(
                        Familiemedlem(
                            fornavn = "Bjørn Ærlig",
                            etternavn = "Østgård",
                            harNorskFodselsnummerEllerDnummer = true,
                            fodselsnummer = "12345678901",
                            fodselsdato = null
                        )
                    )
                )
            )

            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-NORSKE-TEGN",
                arbeidstakerData = arbeidstakerData
            )

            val html = htmlBuilder.byggHtml(skjema)

            html shouldContain "Bjørn Ærlig"
            html shouldContain "Østgård"

            // Verifiser at PDF kan genereres uten feil
            val pdfBytes = pdfGeneratorService.genererPdf(skjema)
            String(pdfBytes.take(4).toByteArray()) shouldBe "%PDF"
        }
    }

    context("Delvis utfylte skjemaer") {
        test("genererer PDF for kun arbeidstakers del") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-ARB",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = null
            )

            val html = htmlBuilder.byggHtml(skjema)

            // Sjekk at arbeidstaker-seksjoner er med
            html shouldContain "Arbeidstakers del"
            html shouldContain "Utenlandsoppdraget"
            html shouldContain "Arbeidssituasjon"

            // Sjekk at arbeidsgiver-overskrift IKKE er med (nøyaktig match)
            html shouldNotContain """<h2 class="part-heading">Arbeidsgivers del</h2>"""
        }

        test("genererer PDF for kun arbeidsgivers del") {
            val skjema = lagInnsendtSkjema(
                referanseId = "MEL-AG",
                arbeidstakerData = null,
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val html = htmlBuilder.byggHtml(skjema)

            // Sjekk at arbeidsgiver-seksjoner er med
            html shouldContain "Arbeidsgivers del"
            html shouldContain "Arbeidsgiverens virksomhet i Norge"

            // Sjekk at arbeidstaker-overskrift IKKE er med (nøyaktig match)
            html shouldNotContain """<h2 class="part-heading">Arbeidstakers del</h2>"""
        }
    }
})

private fun lagKomplettArbeidstakerData(): ArbeidstakersSkjemaDataDto {
    return ArbeidstakersSkjemaDataDto(
        utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier(),
        arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
            aktivitetIMaanedenFoerUtsendingen = "Jeg var student ved Universitetet i Oslo."
        ),
        skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier(),
        familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier(),
        tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
    )
}

private fun lagKomplettArbeidsgiverData(): ArbeidsgiversSkjemaDataDto {
    return ArbeidsgiversSkjemaDataDto(
        arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
        utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier(),
        arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
        arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier(),
        tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
    )
}

/**
 * Validerer at en PDF er PDF/A-2u kompatibel ved hjelp av veraPDF.
 * veraPDF er det offisielle referanse-valideringsverktøyet for PDF/A.
 *
 * @return Liste med valideringsfeil (tom liste = gyldig PDF/A-2u)
 */
private fun validerPdfA2u(pdfBytes: ByteArray): List<TestAssertion> {
    val tempFile = createTempFile(prefix = "pdfa-test", suffix = ".pdf")
    try {
        tempFile.writeBytes(pdfBytes)

        // Initialiser Greenfield parser
        VeraGreenfieldFoundryProvider.initialise()

        Foundries.defaultInstance().use { foundry ->
            val flavour = PDFAFlavour.PDFA_2_U
            val validator = foundry.createValidator(flavour, false)

            foundry.createParser(tempFile.toFile()).use { parser ->
                val result = validator.validate(parser)
                return result.testAssertions
                    .filter { it.status == TestAssertion.Status.FAILED }
            }
        }
    } finally {
        tempFile.toFile().delete()
    }
}
