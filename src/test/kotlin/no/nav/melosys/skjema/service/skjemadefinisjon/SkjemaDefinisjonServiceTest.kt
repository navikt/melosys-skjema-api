package no.nav.melosys.skjema.service.skjemadefinisjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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
})
