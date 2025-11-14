package no.nav.melosys.skjema

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.time.LocalDate
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.dto.SubmitSkjemaRequest
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiveren.ArbeidsgiverenDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakeren.ArbeidstakerenArbeidsgiversDelDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaLandDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaLandFastArbeidsstedDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidstakeren.ArbeidstakerenDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.dto.felles.NorskVirksomhet
import no.nav.melosys.skjema.dto.felles.UtenlandskVirksomhet
import no.nav.melosys.skjema.dto.felles.NorskeOgUtenlandskeVirksomheter
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import no.nav.melosys.skjema.integrasjon.ereg.dto.*

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

fun arbeidsgiversSkjemaDataDtoMedDefaultVerdier() = ArbeidsgiversSkjemaDataDto(
    arbeidsgiveren = arbeidsgiverenDtoMedDefaultVerdier(),
    arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
    utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier(),
    arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier()
)

fun arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier() = ArbeidsgiverensVirksomhetINorgeDto(
    erArbeidsgiverenOffentligVirksomhet = true,
    erArbeidsgiverenBemanningsEllerVikarbyraa = false,
    opprettholderArbeidsgiverenVanligDrift = true
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

fun paLandFastArbeidsstedDtoMedDefaultVerdier() = PaLandFastArbeidsstedDto(
    vegadresse = "Test Street",
    nummer = "123",
    postkode = "12345",
    bySted = "Stockholm"
)

fun paLandDtoMedDefaultVerdier() = PaLandDto(
    fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
    fastArbeidssted = paLandFastArbeidsstedDtoMedDefaultVerdier(),
    beskrivelseVekslende = null,
    erHjemmekontor = false
)

fun arbeidsstedIUtlandetDtoMedDefaultVerdier() = ArbeidsstedIUtlandetDto(
    arbeidsstedType = ArbeidsstedType.PA_LAND,
    paLand = paLandDtoMedDefaultVerdier(),
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
    virksomheterArbeidstakerJobberForIutsendelsesPeriode = null,
)

fun arbeidstakerenArbeidsgiversDelDtoMedDefaultVerdier() = ArbeidstakerenArbeidsgiversDelDto(
    fodselsnummer = "11111111111"
)

fun utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier() = UtenlandsoppdragetArbeidstakersDelDto(
    utsendelsesLand = "SV",
    utsendelseFraDato = LocalDate.of(2024, 1, 1),
    utsendelseTilDato = LocalDate.of(2024, 12, 31)
)

fun arbeidssituasjonDtoMedDefaultVerdier() = ArbeidssituasjonDto(
    harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
    aktivitetIMaanedenFoerUtsendingen = "LONNET_ARBEID",
    skalJobbeForFlereVirksomheter = false,
    virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
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

fun norskVirksomhetMedDefaultVerdier() = NorskVirksomhet(
    organisasjonsnummer = "987654321"
)

fun utenlandskVirksomhetMedDefaultVerdier() = UtenlandskVirksomhet(
    navn = "Foreign Company Ltd",
    organisasjonsnummer = "ABC123",
    vegnavnOgHusnummer = "Main Street 123",
    bygning = "Building A",
    postkode = "12345",
    byStedsnavn = "Stockholm",
    region = "Stockholm County",
    land = "SE",
    tilhorerSammeKonsern = true
)

fun norskeOgUtenlandskeVirksomheterMedDefaultVerdier() = NorskeOgUtenlandskeVirksomheter(
    norskeVirksomheter = listOf(norskVirksomhetMedDefaultVerdier()),
    utenlandskeVirksomheter = listOf(utenlandskVirksomhetMedDefaultVerdier())
)

fun arbeidstakersSkjemaDataDtoMedDefaultVerdier() = ArbeidstakersSkjemaDataDto(
    arbeidstakeren = arbeidstakerenDtoMedDefaultVerdier(),
    skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier(),
    familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier(),
    tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
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

// EREG test data

fun navnMedDefaultVerdier() = Navn(
    sammensattnavn = "Test Bedrift AS",
    navnelinje1 = "Test Bedrift AS"
)

fun adresseMedDefaultVerdier() = Adresse(
    adresselinje1 = "Testveien 1",
    postnummer = "0123",
    poststed = "Oslo",
    landkode = "NO",
    kommunenummer = "0301"
)

fun bruksperiodeMedDefaultVerdier() = Bruksperiode(
    fom = "2020-01-01"
)

fun gyldighetsperiodeMedDefaultVerdier() = Gyldighetsperiode(
    fom = LocalDate.of(2020, 1, 1)
)

fun inngaarIJuridiskEnhetMedDefaultVerdier() = InngaarIJuridiskEnhet(
    organisasjonsnummer = "123456789",
    navn = navnMedDefaultVerdier()
)

fun juridiskEnhetMedDefaultVerdier() = JuridiskEnhet(
    organisasjonsnummer = "123456789",
    navn = navnMedDefaultVerdier(),
    type = "JuridiskEnhet"
)

fun virksomhetMedDefaultVerdier() = Virksomhet(
    organisasjonsnummer = "987654321",
    navn = navnMedDefaultVerdier(),
    type = "Virksomhet"
)

fun organisasjonsleddMedDefaultVerdier() = Organisasjonsledd(
    organisasjonsnummer = "555555555",
    navn = navnMedDefaultVerdier(),
    type = "Organisasjonsledd"
)
