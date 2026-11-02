package no.nav.melosys.skjema.service.skjemadefinisjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.FeltFormat
import no.nav.melosys.skjema.types.skjemadefinisjon.ListeFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.TextFeltDefinisjon
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class SkjemaDefinisjonServiceTest : FunSpec({

    val jsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .build()
    val service = SkjemaDefinisjonService(SkjemaDefinisjonProperties(), jsonMapper)

    test("hent med NYNORSK gir nynorsk tekst, ikke bokmål-fallback") {
        val definisjon = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "1", Språk.NYNORSK)

        val seksjon = definisjon.seksjoner["utsendingsperiodeOgLand"].shouldNotBeNull()
        // nb-tittelen er «Utenlandsoppdraget» — nynorskversjonen beviser at nn faktisk slår gjennom
        seksjon.tittel shouldBe "Utsendingsperiode og land"
        definisjon.seksjoner["familiemedlemmer"].shouldNotBeNull().tittel shouldBe "Familiemedlemmar"
    }

    test("hent med ENGELSK gir engelsk tekst") {
        val definisjon = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "1", Språk.ENGELSK)

        definisjon.seksjoner["utsendingsperiodeOgLand"].shouldNotBeNull().tittel shouldBe "Posting Period and Country"
    }

    test("hent med NORSK_BOKMAL gir uendret bokmålstekst") {
        val definisjon = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "1", Språk.NORSK_BOKMAL)

        definisjon.seksjoner["utsendingsperiodeOgLand"].shouldNotBeNull().tittel shouldBe "Utenlandsoppdraget"
    }

    test("v2 fjerner manuelt offentlig-spørsmål uten å endre v1") {
        val v1 = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "1", Språk.NORSK_BOKMAL)
        val v2 = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "2", Språk.NORSK_BOKMAL)
        val v1Felter = v1.seksjoner["arbeidsgiverensVirksomhetINorge"].shouldNotBeNull().felter
        val v2Felter = v2.seksjoner["arbeidsgiverensVirksomhetINorge"].shouldNotBeNull().felter

        v2.versjon shouldBe "2"
        v1Felter shouldContainKey "erArbeidsgiverenOffentligVirksomhet"
        v2Felter shouldNotContainKey "erArbeidsgiverenOffentligVirksomhet"
        v2Felter shouldContainKey "erArbeidsgiverenBemanningsEllerVikarbyraa"
        v2Felter shouldContainKey "opprettholderArbeidsgiverenVanligDrift"
    }

    test("v3 legger til opplysninger om foretakets samlede virksomhet med prosentformat på andelene") {
        val v2 = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "2", Språk.NORSK_BOKMAL)
        val v3 = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "3", Språk.NORSK_BOKMAL)
        val v2Felter = v2.seksjoner["arbeidsgiverensVirksomhetINorge"].shouldNotBeNull().felter
        val v3Felter = v3.seksjoner["arbeidsgiverensVirksomhetINorge"].shouldNotBeNull().felter
        val antallFelter = listOf("antallAdministrativtAnsatte", "antallUtsendteArbeidstakere")
        val andelFelter = listOf(
            "andelAnsatteRekruttertINorge",
            "andelOmsetningINorge",
            "andelOppdragUtfortINorge",
            "andelOppdragskontrakterInngattINorge"
        )

        v3.versjon shouldBe "3"
        (antallFelter + andelFelter).forEach {
            v2Felter shouldNotContainKey it
            v3Felter shouldContainKey it
        }
        antallFelter.forEach { (v3Felter[it] as TextFeltDefinisjon).format shouldBe null }
        andelFelter.forEach { (v3Felter[it] as TextFeltDefinisjon).format shouldBe FeltFormat.PROSENT }
        v3Felter["andelOmsetningINorge"].shouldNotBeNull().label shouldBe "Andel omsetning opptjent i Norge"
    }

    val virksomhetslister = listOf(
        "arbeidssituasjon" to "virksomheterArbeidstakerJobberForIutsendelsesPeriode",
        "arbeidstakerensLonn" to "virksomheterSomUtbetalerLonnOgNaturalytelser",
        "skatteforholdOgInntekt" to "virksomheterSomUtbetalerLonnOgNaturalytelser"
    )

    test("v6 krever navn, postkode og by/stedsnavn for utenlandske virksomheter") {
        val v6 = service.hent(SkjemaType.UTSENDT_ARBEIDSTAKER, "6", Språk.NORSK_BOKMAL)

        virksomhetslister.forEach { (seksjon, felt) ->
            val liste = v6.seksjoner[seksjon].shouldNotBeNull().felter[felt] as ListeFeltDefinisjon
            val elementDefinisjon = liste.elementDefinisjon

            elementDefinisjon.getValue("navn").label shouldBe "Navn på virksomheten"
            elementDefinisjon.getValue("organisasjonsnummer").label shouldBe "Organisasjonsnummer"
            elementDefinisjon.getValue("organisasjonsnummer").pakrevd shouldBe true
            elementDefinisjon.getValue("organisasjonsnummerEllerRegistreringsnummer").label shouldBe "Organisasjonsnummer eller registreringsnummer"
            elementDefinisjon.getValue("organisasjonsnummerEllerRegistreringsnummer").pakrevd shouldBe false
            listOf("navn", "postkode", "byStedsnavn").forEach {
                elementDefinisjon.getValue(it).pakrevd shouldBe true
            }
            elementDefinisjon.containsKey("ansettelsesform") shouldBe (seksjon == "arbeidssituasjon")
        }
    }
})
