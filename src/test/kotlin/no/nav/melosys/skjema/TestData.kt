package no.nav.melosys.skjema

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.time.LocalDate
import no.nav.melosys.skjema.dto.ArbeidsgiverenDto
import no.nav.melosys.skjema.dto.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.SubmitSkjemaRequest
import no.nav.melosys.skjema.dto.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.ArbeidstakerenDto
import no.nav.melosys.skjema.dto.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.TilleggsopplysningerDto
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse

// NB! Endringer i defaultverdier i testdata skal ikke føre til at tester feiler.
// Hvis endringer i verdier her skulle føre til at tester feiler, så fiks det ved å overstyre verdiene i de feilende testene, ikke verdiene i TestData.

fun altinnTilgangerResponseMedDefaultVerdier() = AltinnTilgangerResponse(
    isError = false,
    hierarki = listOf(
        AltinnTilgang(
            orgnr = "123456789",
            navn = "Test Org",
            organisasjonsform = "AS",
            altinn3Tilganger = setOf("test-fager", "annen-rolle")
        )
    ),
    tilgangTilOrgNr = mapOf(
        "test-fager" to setOf("123456789", "987654321"),
        "annen-rolle" to setOf("123456789")
    ),
    orgNrTilTilganger = emptyMap()
)

fun arbeidsgiverenDtoMedDefaultVerdier() = ArbeidsgiverenDto(
    organisasjonsnummer = "123456789",
    organisasjonNavn = "Test Bedrift AS"
)

fun arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier() = ArbeidsgiverensVirksomhetINorgeDto(
    erArbeidsgiverenOffentligVirksomhet = true,
    erArbeidsgiverenBemanningsEllerVikarbyraa = false,
    opprettholderArbeidsgivereVanligDrift = true
)

fun utenlandsoppdragetDtoMedDefaultVerdier() = UtenlandsoppdragetDto(
    utsendelseLand = "SE",
    arbeidstakerUtsendelseFraDato = LocalDate.of(2024, 1, 1),
    arbeidstakerUtsendelseTilDato = LocalDate.of(2024, 12, 31),
    arbeidsgiverHarOppdragILandet = true,
    arbeidstakerBleAnsattForUtenlandsoppdraget = false,
    arbeidstakerForblirAnsattIHelePerioden = true,
    arbeidstakerErstatterAnnenPerson = false,
    arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = true,
    utenlandsoppholdetsBegrunnelse = "Prosjekt",
    ansettelsesforholdBeskrivelse = "Samme stilling",
    forrigeArbeidstakerUtsendelseFradato = null,
    forrigeArbeidstakerUtsendelseTilDato = null
)

fun arbeidstakerensLonnDtoMedDefaultVerdier() = ArbeidstakerensLonnDto(
    arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
    virksomheterSomUtbetalerLonnOgNaturalytelser = null
)

fun submitSkjemaRequestMedDefaultVerdier() = SubmitSkjemaRequest(
    bekreftetRiktighet = true,
    submittedAt = Instant.now()
)

fun familiemedlemmerDtoMedDefaultVerdier() = FamiliemedlemmerDto(
    sokerForBarnUnder18SomSkalVaereMed = false,
    harEktefellePartnerSamboerEllerBarnOver18SomSenderEgenSoknad = false
)

fun arbeidstakerenDtoMedDefaultVerdier() = ArbeidstakerenDto(
    harNorskFodselsnummer = true,
    fodselsnummer = "11111111111",
    fornavn = "Test",
    etternavn = "Testesen",
    fodselsdato = LocalDate.of(1990, 1, 1),
    harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
    aktivitetIMaanedenFoerUtsendingen = "LONNET_ARBEID",
    skalJobbeForFlereVirksomheter = false,
    norskeVirksomheterArbeidstakerJobberForIutsendelsesPeriode = null,
    utenlandskeVirksomheterArbeidstakerJobberForIutsendelsesPeriode = null
)

fun skatteforholdOgInntektDtoMedDefaultVerdier() = SkatteforholdOgInntektDto(
    erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
    mottarPengestotteFraAnnetEosLandEllerSveits = false,
    landSomUtbetalerPengestotte = null,
    pengestotteSomMottasFraAndreLandBelop = null,
    pengestotteSomMottasFraAndreLandBeskrivelse = null
)

fun tilleggsopplysningerDtoMedDefaultVerdier() = TilleggsopplysningerDto(
    harFlereOpplysningerTilSoknaden = false,
    tilleggsopplysningerTilSoknad = null
)

fun skjemaMedDefaultVerdier(
    fnr: String? = "11111111111",
    orgnr: String? = "123456789",
    status: SkjemaStatus = SkjemaStatus.UTKAST,
    type: String = "A1",
    data: JsonNode? = null,
    opprettetDato: Instant = Instant.now(),
    endretDato: Instant = Instant.now(),
    opprettetAv: String = fnr ?: "11111111111",
    endretAv: String = fnr ?: "11111111111"
): Skjema {
    return Skjema(
        status = status,
        type = type,
        fnr = fnr,
        orgnr = orgnr,
        data = data,
        opprettetDato = opprettetDato,
        endretDato = endretDato,
        opprettetAv = opprettetAv,
        endretAv = endretAv
    )
}
