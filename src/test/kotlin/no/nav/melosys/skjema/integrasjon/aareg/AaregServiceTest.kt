package no.nav.melosys.skjema.integrasjon.aareg

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import no.nav.melosys.skjema.integrasjon.aareg.dto.Ansettelsesperiode
import no.nav.melosys.skjema.integrasjon.aareg.dto.Arbeidsforhold
import no.nav.melosys.skjema.integrasjon.aareg.dto.Enhet
import no.nav.melosys.skjema.integrasjon.aareg.dto.Ident
import no.nav.melosys.skjema.integrasjon.aareg.dto.Kodeverksentitet
import no.nav.melosys.skjema.types.felles.PeriodeDto

class AaregServiceTest : FunSpec({

    val fnr = "12345678910"
    val orgnr = "910000001"
    val juridiskEnhetOrgnr = "910000000"
    val soknadsperiode = PeriodeDto(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 8, 31))

    val aaregClient = mockk<AaregClient>()
    val aaregService = AaregService(aaregClient)

    fun arbeidsforhold(
        type: String? = ARBEIDSFORHOLDTYPE_ORDINAERT,
        arbeidsgiver: String? = orgnr,
        opplysningspliktig: String? = juridiskEnhetOrgnr,
        fom: LocalDate? = LocalDate.of(2020, 1, 1),
        tom: LocalDate? = null
    ) = Arbeidsforhold(
        type = Kodeverksentitet(kode = type),
        arbeidssted = Enhet(type = "Underenhet", identer = listOf(Ident(type = "ORGANISASJONSNUMMER", ident = arbeidsgiver))),
        opplysningspliktig = Enhet(type = "Hovedenhet", identer = listOf(Ident(type = "ORGANISASJONSNUMMER", ident = opplysningspliktig))),
        ansettelsesperiode = Ansettelsesperiode(startdato = fom, sluttdato = tom)
    )

    fun harTreff(vararg arbeidsforhold: Arbeidsforhold): Boolean {
        every { aaregClient.hentOrdinaereArbeidsforhold(fnr) } returns arbeidsforhold.toList()
        return aaregService.harOrdinaertArbeidsforhold(fnr, orgnr, juridiskEnhetOrgnr, soknadsperiode)
    }

    test("aktivt arbeidsforhold hos oppgitt arbeidsgiver gir treff") {
        harTreff(arbeidsforhold()) shouldBe true
    }

    test("arbeidsforhold hos annen underenhet under samme juridiske enhet gir treff") {
        harTreff(arbeidsforhold(arbeidsgiver = "920000001")) shouldBe true
    }

    test("arbeidsforhold hos annen arbeidsgiver og annen juridisk enhet gir ikke treff") {
        harTreff(arbeidsforhold(arbeidsgiver = "920000001", opplysningspliktig = "920000000")) shouldBe false
    }

    test("arbeidsforhold av annen type enn ordinært gir ikke treff") {
        harTreff(arbeidsforhold(type = "maritimtArbeidsforhold")) shouldBe false
    }

    test("arbeidsforhold som dekker nøyaktig søknadsperioden gir treff") {
        harTreff(arbeidsforhold(fom = LocalDate.of(2026, 3, 1), tom = LocalDate.of(2026, 8, 31))) shouldBe true
    }

    test("arbeidsforhold avsluttet før søknadsperioden gir ikke treff") {
        harTreff(arbeidsforhold(tom = LocalDate.of(2026, 2, 28))) shouldBe false
    }

    test("arbeidsforhold avsluttet i søknadsperioden gir ikke treff") {
        harTreff(arbeidsforhold(tom = LocalDate.of(2026, 8, 30))) shouldBe false
    }

    test("arbeidsforhold som starter etter søknadsperioden gir ikke treff") {
        harTreff(arbeidsforhold(fom = LocalDate.of(2026, 9, 1))) shouldBe false
    }

    test("arbeidsforhold som starter i søknadsperioden gir ikke treff") {
        harTreff(arbeidsforhold(fom = LocalDate.of(2026, 3, 2))) shouldBe false
    }

    test("orgnr matches bare mot identer av typen organisasjonsnummer") {
        harTreff(
            arbeidsforhold(opplysningspliktig = "920000000").copy(
                arbeidssted = Enhet(type = "Person", identer = listOf(Ident(type = "FOLKEREGISTERIDENT", ident = orgnr)))
            )
        ) shouldBe false
    }

    test("ingen arbeidsforhold gir ikke treff") {
        harTreff() shouldBe false
    }

    test("treff på ett av flere arbeidsforhold gir treff") {
        harTreff(
            arbeidsforhold(arbeidsgiver = "920000001", opplysningspliktig = "920000000"),
            arbeidsforhold()
        ) shouldBe true
    }
})
