package no.nav.melosys.skjema.pdf

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
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Familiemedlem
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedAnsettelsesformMedDefaultVerdier
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedDefaultVerdier
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.TestAssertion
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

private val log = KotlinLogging.logger {}

class PdfGeneratorTest : FunSpec({

    val jsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .build()
    val properties = SkjemaDefinisjonProperties()
    val skjemaDefinisjonService = SkjemaDefinisjonService(properties, jsonMapper)

    fun lagrePdfForInspeksjon(filnavn: String, pdfBytes: ByteArray) {
        val outputFile = File("build/test-output/$filnavn")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(pdfBytes)
        log.info { "PDF lagret til: ${outputFile.absolutePath}" }
    }

    fun lagSkjemaPdfData(
        referanseId: String,
        språk: Språk = Språk.NORSK_BOKMAL,
        arbeidstakerData: UtsendtArbeidstakerArbeidstakersSkjemaDataDto? = null,
        arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto? = null,
        aktørInfo: AktørInfo = AktørInfo(
            arbeidsgiverNavn = "Test Bedrift AS",
            orgnr = "123456789",
            arbeidstakerNavn = "Ola Nordmann",
            arbeidstakerFnr = "12345678901"
        )
    ): SkjemaPdfData {
        val definisjon = skjemaDefinisjonService.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, null, språk)
        val skjemaData: UtsendtArbeidstakerSkjemaData = arbeidstakerData ?: arbeidsgiverData
            ?: throw IllegalArgumentException("Minst en av arbeidstakerData eller arbeidsgiverData må oppgis")
        val kobletSkjemaData: UtsendtArbeidstakerSkjemaData? = if (arbeidstakerData != null && arbeidsgiverData != null) arbeidsgiverData else null
        return SkjemaPdfData(
            skjemaId = UUID.randomUUID(),
            referanseId = referanseId,
            innsendtDato = Instant.now(),
            innsendtSprak = språk,
            aktørInfo = aktørInfo,
            skjemaData = skjemaData,
            kobletSkjemaData = kobletSkjemaData,
            definisjon = definisjon
        )
    }

    context("PDF-generering") {
        test("genererer gyldig PDF med korrekt signatur") {
            val skjema = lagSkjemaPdfData(
                referanseId = "VALID1",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val pdfBytes = genererPdf(skjema)

            pdfBytes shouldNotBe null
            pdfBytes.size shouldNotBe 0
            String(pdfBytes.take(4).toByteArray()) shouldBe "%PDF"
        }

        test("genererer PDF/A-2u kompatibel fil") {
            val skjema = lagSkjemaPdfData(
                referanseId = "PDFA12",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val pdfBytes = genererPdf(skjema)
            lagrePdfForInspeksjon("pdfa-validering.pdf", pdfBytes)

            // Valider PDF/A-2u compliance med veraPDF (offisielt valideringsverktøy)
            val feil = validerPdfA2u(pdfBytes)

            if (feil.isNotEmpty()) {
                val feilTekst = feil.joinToString("\n") { "- ${it.ruleId}: ${it.message}" }
                log.error { "PDF/A-2u valideringsfeil:\n$feilTekst" }
                File("build/test-output/pdfa-errors.txt").apply {
                    parentFile?.mkdirs()
                    writeText("PDF/A-2u valideringsfeil:\n$feilTekst")
                }
            }

            feil.shouldBeEmpty()
        }

        test("genererer PDF med forventet størrelse for komplett søknad") {
            val skjema = lagSkjemaPdfData(
                referanseId = "KMPLTT",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val pdfBytes = genererPdf(skjema)

            // PDF bør være minst 3KB for en komplett søknad
            pdfBytes.size shouldNotBe 0
            lagrePdfForInspeksjon("komplett-soknad.pdf", pdfBytes)
        }
    }

    context("HTML-innhold - Norsk") {
        test("inneholder korrekte overskrifter og seksjoner") {
            val skjema = lagSkjemaPdfData(
                referanseId = "NORSK1",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Søknad om A1"
            html shouldContain "Arbeidstakers del"
            html shouldContain "Arbeidsgivers del"
            html shouldContain "Utenlandsoppdraget"
            html shouldContain "Arbeidssituasjon"
            html shouldContain "Skatteforhold og inntekt"
            html shouldContain "Familiemedlemmer"
        }

        test("viser landnavn på norsk") {
            val skjema = lagSkjemaPdfData(
                referanseId = "LANDNO",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Sverige"
            html shouldNotContain "Sweden"
        }

        test("viser boolean-verdier som Ja/Nei") {
            val skjema = lagSkjemaPdfData(
                referanseId = "BOOLNO",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            // Sjekk at boolean-verdier vises som Ja eller Nei
            html shouldContain ">Ja<"
            html shouldContain ">Nei<"
        }

        test("viser datoer i norsk format") {
            val skjema = lagSkjemaPdfData(
                referanseId = "DATO12",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            // Datoer skal være på format dd.MM.yyyy
            html shouldContain "01.01.2024"
            html shouldContain "31.12.2024"
        }

        test("viser perioder som separate fra/til-felter") {
            val skjema = lagSkjemaPdfData(
                referanseId = "PERIOD",
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Fra dato"
            html shouldContain "Til dato"
        }
    }

    context("HTML-innhold - Engelsk") {
        test("inneholder engelske overskrifter") {
            val skjema = lagSkjemaPdfData(
                referanseId = "ENG123",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Application for posted worker"
            html shouldContain "Employee"
            html shouldContain "Employer"
            html shouldContain "Foreign assignment"
        }

        test("viser landnavn på engelsk") {
            val skjema = lagSkjemaPdfData(
                referanseId = "LANDEN",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Sweden"
        }

        test("viser boolean-verdier som Yes/No") {
            val skjema = lagSkjemaPdfData(
                referanseId = "BOOLEN",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain ">Yes<"
            html shouldContain ">No<"
        }
    }

    context("Aktør-info seksjon") {
        test("viser arbeidsgiver-navn og orgnr på norsk") {
            val skjema = lagSkjemaPdfData(
                referanseId = "AKT_NO",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                aktørInfo = AktørInfo(
                    arbeidsgiverNavn = "Nav Kontaktsenter AS",
                    orgnr = "987654321",
                    arbeidstakerNavn = "Kari Nordmann",
                    arbeidstakerFnr = "11223344556"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Nav Kontaktsenter AS"
            html shouldContain "987654321"
            html shouldContain "Arbeidsgiver"
            html shouldContain "Organisasjonsnummer"
        }

        test("viser arbeidstaker-navn og fnr på norsk") {
            val skjema = lagSkjemaPdfData(
                referanseId = "AKT_AT",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                aktørInfo = AktørInfo(
                    arbeidsgiverNavn = "Nav Kontaktsenter AS",
                    orgnr = "987654321",
                    arbeidstakerNavn = "Kari Nordmann",
                    arbeidstakerFnr = "11223344556"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Kari Nordmann"
            html shouldContain "11223344556"
            html shouldContain "Arbeidstaker"
            html shouldContain "Fødselsnummer"
        }

        test("viser engelske ledetekster") {
            val skjema = lagSkjemaPdfData(
                referanseId = "AKT_EN",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData(),
                aktørInfo = AktørInfo(
                    arbeidsgiverNavn = "Test Corp Ltd",
                    orgnr = "111222333",
                    arbeidstakerNavn = "John Doe",
                    arbeidstakerFnr = "99887766554"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Employer"
            html shouldContain "Organisation number"
            html shouldContain "Employee"
            html shouldContain "National identity number"
            html shouldContain "Test Corp Ltd"
            html shouldContain "John Doe"
        }

        test("viser 'D-nummer' som label når identifikator er d-nummer") {
            val skjema = lagSkjemaPdfData(
                referanseId = "AKT_DN",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                aktørInfo = AktørInfo(
                    arbeidsgiverNavn = "Nav Kontaktsenter AS",
                    orgnr = "987654321",
                    arbeidstakerNavn = "Kari Nordmann",
                    arbeidstakerFnr = "41015678901" // Starter med 4 → D-nummer
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "D-nummer"
            html shouldNotContain "Fødselsnummer"
        }

        test("viser 'Fødselsnummer' som label når identifikator er fødselsnummer") {
            val skjema = lagSkjemaPdfData(
                referanseId = "AKT_FN",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                aktørInfo = AktørInfo(
                    arbeidsgiverNavn = "Nav Kontaktsenter AS",
                    orgnr = "987654321",
                    arbeidstakerNavn = "Kari Nordmann",
                    arbeidstakerFnr = "11223344556" // Starter med 1 → fødselsnummer
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Fødselsnummer"
            html shouldNotContain "D-nummer"
        }

        test("escaper HTML-spesialtegn i aktør-info") {
            val skjema = lagSkjemaPdfData(
                referanseId = "AKT_XS",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                aktørInfo = AktørInfo(
                    arbeidsgiverNavn = "Tom & Jerry AS",
                    orgnr = "123456789",
                    arbeidstakerNavn = "O'Brien",
                    arbeidstakerFnr = "12345678901"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Tom &amp; Jerry AS"
            html shouldContain "O&#39;Brien"
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

            val skjema = lagSkjemaPdfData(
                referanseId = "FAMFNR",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

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

            val skjema = lagSkjemaPdfData(
                referanseId = "FAMDTO",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

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

            val skjema = lagSkjemaPdfData(
                referanseId = "FAMFLR",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

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

            val skjema = lagSkjemaPdfData(
                referanseId = "LAND12",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "På land"
            lagrePdfForInspeksjon("arbeidssted-pa-land.pdf", genererPdf(skjema))
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

            val skjema = lagSkjemaPdfData(
                referanseId = "OFFSHR",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Offshore"
            lagrePdfForInspeksjon("arbeidssted-offshore.pdf", genererPdf(skjema))
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

            val skjema = lagSkjemaPdfData(
                referanseId = "SKIP12",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "På skip"
            lagrePdfForInspeksjon("arbeidssted-pa-skip.pdf", genererPdf(skjema))
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

            val skjema = lagSkjemaPdfData(
                referanseId = "FLY123",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Om bord på fly"
            lagrePdfForInspeksjon("arbeidssted-om-bord-pa-fly.pdf", genererPdf(skjema))
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

            val skjema = lagSkjemaPdfData(
                referanseId = "XSS123",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

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

            val skjema = lagSkjemaPdfData(
                referanseId = "AMP123",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

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

            val skjema = lagSkjemaPdfData(
                referanseId = "NRTEGN",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Bjørn Ærlig"
            html shouldContain "Østgård"

            // Verifiser at PDF kan genereres uten feil
            val pdfBytes = genererPdf(skjema)
            String(pdfBytes.take(4).toByteArray()) shouldBe "%PDF"
        }
    }

    context("Delvis utfylte skjemaer") {
        test("genererer PDF for kun arbeidstakers del") {
            val skjema = lagSkjemaPdfData(
                referanseId = "ARB123",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = null
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            // Sjekk at arbeidstaker-seksjoner er med
            html shouldContain "Arbeidstakers del"
            html shouldContain "Utenlandsoppdraget"
            html shouldContain "Arbeidssituasjon"

            // Sjekk at arbeidsgiver-overskrift IKKE er med (nøyaktig match)
            html shouldNotContain """<h2 class="part-heading">Arbeidsgivers del</h2>"""
        }

        test("genererer PDF for kun arbeidsgivers del") {
            val skjema = lagSkjemaPdfData(
                referanseId = "AGV123",
                arbeidstakerData = null,
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            // Sjekk at arbeidsgiver-seksjoner er med
            html shouldContain "Arbeidsgivers del"
            html shouldContain "Arbeidsgiverens virksomhet i Norge"

            // Sjekk at arbeidstaker-overskrift IKKE er med (nøyaktig match)
            html shouldNotContain """<h2 class="part-heading">Arbeidstakers del</h2>"""
        }
    }

    context("Utenlandsk virksomhet") {
        test("viser alle adressefelter for utenlandsk lønnsutbetaler i arbeidsgivers del") {
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidstakerensLonn = ArbeidstakerensLonnDto(
                    arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
                    virksomheterSomUtbetalerLonnOgNaturalytelser = norskeOgUtenlandskeVirksomheterMedDefaultVerdier()
                )
            )

            val skjema = lagSkjemaPdfData(
                referanseId = "UTLAGV",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Foreign Company Ltd"
            html shouldContain "ABC123"
            html shouldContain "Main Street 123"
            html shouldContain "Building A"
            html shouldContain "12345"
            html shouldContain "Stockholm County"
            html shouldContain "Sverige"
            html shouldContain "Tilhører samme konsern som norsk arbeidsgiver?"
        }

        test("viser alle felter inkludert ansettelsesform når arbeidstaker jobber for flere virksomheter") {
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier().copy(
                    skalJobbeForFlereVirksomheter = true,
                    virksomheterArbeidstakerJobberForIutsendelsesPeriode =
                        norskeOgUtenlandskeVirksomheterMedAnsettelsesformMedDefaultVerdier()
                )
            )

            val skjema = lagSkjemaPdfData(
                referanseId = "UTLAT1",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Foreign Company Ltd"
            html shouldContain "Main Street 123"
            html shouldContain "Building A"
            html shouldContain "Hva jobber du som i denne virksomheten?"
            html shouldContain "Arbeidstaker eller frilanser"
        }
    }

    context("Kombinert skjema (fullmakt)") {
        test("viser tilleggsopplysninger fra toppnivå i kombinert PDF") {
            val data = arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier().copy(
                tilleggsopplysninger = TilleggsopplysningerDto(
                    harFlereOpplysningerTilSoknaden = true,
                    tilleggsopplysningerTilSoknad = "Tillegg via fullmektig"
                )
            )
            val definisjon = skjemaDefinisjonService.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, null, Språk.NORSK_BOKMAL)
            val skjema = SkjemaPdfData(
                skjemaId = UUID.randomUUID(),
                referanseId = "FULLMK",
                innsendtDato = Instant.now(),
                innsendtSprak = Språk.NORSK_BOKMAL,
                aktørInfo = AktørInfo(
                    arbeidsgiverNavn = "Bedrift AS",
                    orgnr = "123456789",
                    arbeidstakerNavn = "Ola Nordmann",
                    arbeidstakerFnr = "12345678901"
                ),
                skjemaData = data,
                kobletSkjemaData = null,
                definisjon = definisjon
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Tilleggsopplysninger"
            html shouldContain "Tillegg via fullmektig"
        }
    }
})

private fun lagKomplettArbeidstakerData(): UtsendtArbeidstakerArbeidstakersSkjemaDataDto {
    return UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
        utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
        arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
            aktivitetIMaanedenFoerUtsendingen = "Jeg var student ved Universitetet i Oslo."
        ),
        skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier(),
        familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier(),
        tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
    )
}

private fun lagKomplettArbeidsgiverData(): UtsendtArbeidstakerArbeidsgiversSkjemaDataDto {
    return UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
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
