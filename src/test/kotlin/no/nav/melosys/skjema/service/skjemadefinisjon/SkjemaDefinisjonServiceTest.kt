package no.nav.melosys.skjema.service.skjemadefinisjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.Språk
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
})
