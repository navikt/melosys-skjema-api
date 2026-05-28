package no.nav.melosys.skjema.pdf

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.time.Instant
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
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheter
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedAnsettelsesformMedDefaultVerdier
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedDefaultVerdier
import no.nav.melosys.skjema.utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier
import no.nav.melosys.skjema.utenlandskVirksomhetMedDefaultVerdier
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import org.apache.pdfbox.Loader
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
        ),
        fullmektigInfo: FullmektigInfo? = null,
        radgiverInfo: RadgiverInfo? = null
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
            fullmektigInfo = fullmektigInfo,
            radgiverInfo = radgiverInfo,
            skjemaData = skjemaData,
            kobletSkjemaData = kobletSkjemaData,
            vedlegg = emptyList(),
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
            lagrePdfForInspeksjon("komplett-soknad.pdf", pdfBytes)

            // PDF bør være minst 3KB for en komplett søknad
            pdfBytes.size shouldBeGreaterThan 3_000
        }

        test("lagrer PDF for inspeksjon av seksjon som spenner over flere sider") {
            val mangeUtenlandskeVirksomheter = List(30) { index ->
                utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier().copy(
                    navn = "Sidebryttest virksomhet ${index + 1}",
                    organisasjonsnummer = "PGBREAK-${index + 1}",
                    vegnavnOgHusnummer = "Lang testgate ${index + 1}",
                    bygning = "Bygg ${index + 1}",
                    postkode = "${1000 + index}",
                    byStedsnavn = "Testby ${index + 1}",
                    region = "Testregion ${index + 1}"
                )
            }
            val arbeidstakerData = lagKomplettArbeidstakerData().copy(
                arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier().copy(
                    harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
                    skalJobbeForFlereVirksomheter = true,
                    virksomheterArbeidstakerJobberForIutsendelsesPeriode =
                        norskeOgUtenlandskeVirksomheterMedAnsettelsesformMedDefaultVerdier().copy(
                            utenlandskeVirksomheter = mangeUtenlandskeVirksomheter
                        )
                )
            )
            val skjema = lagSkjemaPdfData(
                referanseId = "PGBREAK",
                arbeidstakerData = arbeidstakerData,
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val pdfBytes = genererPdf(skjema)
            lagrePdfForInspeksjon("page-break-lang-boks.pdf", pdfBytes)

            Loader.loadPDF(pdfBytes).use { document ->
                document.numberOfPages shouldBeGreaterThan 3
            }
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

        test("CSS forhindrer linjeskift mellom overskrift og innhold") {
            val skjema = lagSkjemaPdfData(
                referanseId = "LSKIFT",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                arbeidsgiverData = lagKomplettArbeidsgiverData()
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "page-break-after: avoid"
            html shouldContain "page-break-inside: avoid"
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

            html shouldContain "Application for A1 for workers posted in the EEA or Switzerland"
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
        test("viser alle aktør-felter med norske ledetekster") {
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

            html shouldContain "Arbeidsgiver"
            html shouldContain "Nav Kontaktsenter AS"
            html shouldContain "Organisasjonsnummer"
            html shouldContain "987654321"
            html shouldContain "Arbeidstaker"
            html shouldContain "Kari Nordmann"
            html shouldContain "Fødselsnummer"
            html shouldContain "11223344556"
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

    context("Fullmektig-info seksjon") {
        test("viser fullmektig med navn og fødselsnummer på norsk") {
            val skjema = lagSkjemaPdfData(
                referanseId = "FLM_NO",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                fullmektigInfo = FullmektigInfo(
                    navn = "Per Fullmansen",
                    fnr = "11223344556"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Fullmektig"
            html shouldContain "Per Fullmansen"
            html shouldContain "11223344556"
            html shouldContain "Fødselsnummer"
        }

        test("viser ikke fullmektig når fullmektigInfo er null") {
            val skjema = lagSkjemaPdfData(
                referanseId = "FLM_NL",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                fullmektigInfo = null
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldNotContain "Fullmektig"
            html shouldNotContain "Power of attorney"
        }

        test("viser fullmektig med engelske ledetekster") {
            val skjema = lagSkjemaPdfData(
                referanseId = "FLM_EN",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData(),
                fullmektigInfo = FullmektigInfo(
                    navn = "Per Fullmansen",
                    fnr = "11223344556"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Power of attorney"
            html shouldContain "Per Fullmansen"
            html shouldContain "National identity number"
        }

        test("viser D-nummer label for fullmektig med d-nummer") {
            val skjema = lagSkjemaPdfData(
                referanseId = "FLM_DN",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                fullmektigInfo = FullmektigInfo(
                    navn = "Dina Dnummer",
                    fnr = "41015678901" // Starter med 4 → D-nummer
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "D-nummer"
            html shouldContain "Dina Dnummer"
        }

        test("fullmektig vises etter arbeidstaker og før arbeidsgiver") {
            val skjema = lagSkjemaPdfData(
                referanseId = "FLM_OR",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                fullmektigInfo = FullmektigInfo(
                    navn = "Per Fullmansen",
                    fnr = "11223344556"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            val arbeidstakerPos = html.indexOf("""<h3 class="form-summary-heading">Arbeidstaker</h3>""")
            val fullmektigPos = html.indexOf("""<h3 class="form-summary-heading">Fullmektig</h3>""")
            val arbeidsgiverPos = html.indexOf("""<h3 class="form-summary-heading">Arbeidsgiver</h3>""")

            (arbeidstakerPos < fullmektigPos) shouldBe true
            (fullmektigPos < arbeidsgiverPos) shouldBe true
        }
    }

    context("Rådgiver-info seksjon") {
        test("viser rådgiverfirma og rådgiver-person på norsk") {
            val skjema = lagSkjemaPdfData(
                referanseId = "RAD_NO",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                radgiverInfo = RadgiverInfo(
                    firmaNavn = "Regnskap & Råd AS",
                    firmaOrgnr = "998877665",
                    personNavn = "Kari Rådgiver",
                    personFnr = "22334455667"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Rådgiverfirma som representerer arbeidsgiver"
            html shouldContain "Regnskap &amp; Råd AS"
            html shouldContain "998877665"
            html shouldContain "Person hos rådgiverfirma med delegert tilgang til arbeidsgiver"
            html shouldContain "Kari Rådgiver"
            html shouldContain "22334455667"
        }

        test("viser ikke rådgiver-info når radgiverInfo er null") {
            val skjema = lagSkjemaPdfData(
                referanseId = "RAD_NL",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                radgiverInfo = null
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldNotContain "Rådgiverfirma"
            html shouldNotContain "Advisory firm"
        }

        test("viser rådgiver-info med engelske ledetekster") {
            val skjema = lagSkjemaPdfData(
                referanseId = "RAD_EN",
                språk = Språk.ENGELSK,
                arbeidstakerData = lagKomplettArbeidstakerData(),
                radgiverInfo = RadgiverInfo(
                    firmaNavn = "Advice Corp",
                    firmaOrgnr = "998877665",
                    personNavn = "John Advisor",
                    personFnr = "22334455667"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Advisory firm representing employer"
            html shouldContain "Person at advisory firm with delegated access to employer"
            html shouldContain "Advice Corp"
            html shouldContain "John Advisor"
        }

        test("viser D-nummer label for rådgiver-person med d-nummer") {
            val skjema = lagSkjemaPdfData(
                referanseId = "RAD_DN",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                radgiverInfo = RadgiverInfo(
                    firmaNavn = "Råd AS",
                    firmaOrgnr = "998877665",
                    personNavn = "Kari Rådgiver",
                    personFnr = "41015678901" // Starter med 4 → D-nummer
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "D-nummer"
            html shouldContain "Kari Rådgiver"
        }

        test("rådgiver-info vises etter arbeidsgiver") {
            val skjema = lagSkjemaPdfData(
                referanseId = "RAD_OR",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                radgiverInfo = RadgiverInfo(
                    firmaNavn = "Råd AS",
                    firmaOrgnr = "998877665",
                    personNavn = "Kari Rådgiver",
                    personFnr = "22334455667"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            val arbeidsgiverPos = html.indexOf("""<h3 class="form-summary-heading">Arbeidsgiver</h3>""")
            val radgiverPos = html.indexOf("Rådgiverfirma som representerer arbeidsgiver")

            (arbeidsgiverPos < radgiverPos) shouldBe true
        }
    }

    context("Komplett aktør-rekkefølge") {
        test("rekkefølge: Arbeidstaker -> Fullmektig -> Arbeidsgiver -> Rådgiver") {
            val skjema = lagSkjemaPdfData(
                referanseId = "RKKFLG",
                arbeidstakerData = lagKomplettArbeidstakerData(),
                fullmektigInfo = FullmektigInfo(
                    navn = "Per Fullmansen",
                    fnr = "11223344556"
                ),
                radgiverInfo = RadgiverInfo(
                    firmaNavn = "Råd AS",
                    firmaOrgnr = "998877665",
                    personNavn = "Kari Rådgiver",
                    personFnr = "22334455667"
                )
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            val arbeidstakerPos = html.indexOf("""<h3 class="form-summary-heading">Arbeidstaker</h3>""")
            val fullmektigPos = html.indexOf("""<h3 class="form-summary-heading">Fullmektig</h3>""")
            val arbeidsgiverPos = html.indexOf("""<h3 class="form-summary-heading">Arbeidsgiver</h3>""")
            val radgiverPos = html.indexOf("Rådgiverfirma som representerer arbeidsgiver")

            (arbeidstakerPos < fullmektigPos) shouldBe true
            (fullmektigPos < arbeidsgiverPos) shouldBe true
            (arbeidsgiverPos < radgiverPos) shouldBe true
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
            html shouldContain "Land"
            html shouldContain "Sverige"
            lagrePdfForInspeksjon("arbeidssted-pa-land.pdf", genererPdf(skjema))
        }

        test("viser land for fast arbeidssted uten adresse") {
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                    arbeidsstedType = ArbeidsstedType.PA_LAND,
                    paLand = paLandDtoMedDefaultVerdier().copy(fastArbeidssted = null),
                    offshore = null,
                    paSkip = null,
                    omBordPaFly = null
                )
            )

            val skjema = lagSkjemaPdfData(
                referanseId = "LANDNA",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Land"
            html shouldContain "Sverige"
        }

        test("viser ikke fast adresse for vekslende arbeidssted på land") {
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                    arbeidsstedType = ArbeidsstedType.PA_LAND,
                    paLand = paLandDtoMedDefaultVerdier().copy(
                        fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE
                    ),
                    offshore = null,
                    paSkip = null,
                    omBordPaFly = null
                )
            )

            val skjema = lagSkjemaPdfData(
                referanseId = "VEKSL",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Veksler ofte"
            html shouldNotContain "Test Street"
            html shouldNotContain "Stockholm"
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
                tilleggsopplysninger = TilleggsopplysningerDto(
                    harFlereOpplysningerTilSoknaden = true,
                    tilleggsopplysningerTilSoknad = "Bjørn Ærlig Østgård"
                )
            )

            val skjema = lagSkjemaPdfData(
                referanseId = "NRTEGN",
                arbeidstakerData = arbeidstakerData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Bjørn Ærlig Østgård"

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

            html shouldContain "Søknad om A1 for utsendte arbeidstakere i EØS eller Sveits"
            html shouldNotContain "Bekreftelse fra arbeidsgiver på utsending til annet EØS-land eller Sveits"

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

            html shouldContain "Bekreftelse fra arbeidsgiver på utsending til annet EØS-land eller Sveits"
            html shouldNotContain "Søknad om A1"

            // Sjekk at arbeidsgiver-seksjoner er med
            html shouldContain "Arbeidsgivers del"
            html shouldContain "Arbeidsgiverens virksomhet i Norge"

            html shouldContain "I hvilket land skal arbeidet utføres?"
            html shouldContain "Sverige"
            html shouldContain "Fra dato"
            html shouldContain "Til dato"

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
            val tilhorerKonsernVerdi = verdiEtterLabel(html, "Tilhører samme konsern som norsk arbeidsgiver?")
            tilhorerKonsernVerdi shouldBe "Ja"
        }

        test("rendrer tilhorerSammeKonsern=false som Nei") {
            val utenlandsk = utenlandskVirksomhetMedDefaultVerdier().copy(tilhorerSammeKonsern = false)
            val arbeidsgiverData = lagKomplettArbeidsgiverData().copy(
                arbeidstakerensLonn = ArbeidstakerensLonnDto(
                    arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
                    virksomheterSomUtbetalerLonnOgNaturalytelser = NorskeOgUtenlandskeVirksomheter(
                        norskeVirksomheter = null,
                        utenlandskeVirksomheter = listOf(utenlandsk)
                    )
                )
            )

            val skjema = lagSkjemaPdfData(
                referanseId = "UTLAGN",
                arbeidsgiverData = arbeidsgiverData
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            val tilhorerKonsernVerdi = verdiEtterLabel(html, "Tilhører samme konsern som norsk arbeidsgiver?")
            tilhorerKonsernVerdi shouldBe "Nei"
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
                vedlegg = emptyList(),
                definisjon = definisjon
            )

            val html = HtmlDokumentGenerator.byggHtml(skjema)

            html shouldContain "Tilleggsopplysninger"
            html shouldContain "Tillegg via fullmektig"
        }
    }
})

/**
 * Plukker ut verdien som rendreres rett etter en gitt label i den genererte HTML-en.
 * Brukes for å verifisere felt-verdier uten å være avhengig av label-uavhengige strenger som
 * "Ja"/"Nei", som finnes mange steder i en typisk PDF.
 */
private fun verdiEtterLabel(html: String, label: String): String? {
    val regex = Regex(
        """<span class="list-item-label">${Regex.escape(label)}</span>\s*<span class="list-item-value">([^<]*)</span>"""
    )
    return regex.find(html)?.groupValues?.get(1)
}

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
        utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
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
