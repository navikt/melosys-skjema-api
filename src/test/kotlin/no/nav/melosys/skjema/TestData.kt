package no.nav.melosys.skjema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.LocalDate
import no.nav.melosys.skjema.dto.Representasjonstype
import no.nav.melosys.skjema.dto.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.dto.SubmitSkjemaRequest
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaLandDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaLandFastArbeidsstedDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.OffshoreDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.TypeInnretning
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaSkipDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.Farvann
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.OmBordPaFlyDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.dto.felles.NorskVirksomhet
import no.nav.melosys.skjema.dto.felles.UtenlandskVirksomhet
import no.nav.melosys.skjema.dto.felles.NorskeOgUtenlandskeVirksomheter
import no.nav.melosys.skjema.dto.felles.PeriodeDto
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import no.nav.melosys.skjema.integrasjon.ereg.dto.*
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt

// Defaultverdiene tar utgangspunkt i gyldige data hva gjelder formater og sammenhenger mtp validatorene (no/nav/melosys/skjema/controller/validators).
// NB! Endringer i defaultverdier i testdata skal sjeldent føre til at tester feiler.
// Hvis endringer i verdier her skulle føre til at tester feiler, så fiks det ved å overstyre verdiene i de feilende testene, ikke verdiene i TestData.
// Med unntak av tilfeller hvor defaultverdiene har ugyldige formater og kombinasjoner mtp validatorene (no/nav/melosys/skjema/controller/validators).


val korrektSyntetiskFnr = "02837999890"
val etAnnetKorrektSyntetiskFnr = "20925297314"

val korrektSyntetiskOrgnr = "312587963"

fun altinnTilgangerResponseMedDefaultVerdier() = AltinnTilgangerResponse(
    isError = false,
    hierarki = listOf(
        AltinnTilgang(
            orgnr = korrektSyntetiskOrgnr,
            navn = "Test Org",
            organisasjonsform = "AS",
            altinn3Tilganger = setOf("test-fager", "annen-rolle")
        )
    ),
    tilgangTilOrgNr = mapOf(
        "test-fager" to setOf(korrektSyntetiskOrgnr, korrektSyntetiskOrgnr),
        "annen-rolle" to setOf(korrektSyntetiskOrgnr)
    ),
    orgNrTilTilganger = emptyMap()
)

fun arbeidsgiversSkjemaDataDtoMedDefaultVerdier() = ArbeidsgiversSkjemaDataDto(
    arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
    utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier(),
    arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier()
)

fun arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier() = ArbeidsgiverensVirksomhetINorgeDto(
    erArbeidsgiverenOffentligVirksomhet = true
)

fun periodeDtoMedDefaultVerdier() = PeriodeDto(
    fraDato = LocalDate.of(2024, 1, 1),
    tilDato = LocalDate.of(2024, 12, 31)
)

fun utenlandsoppdragetDtoMedDefaultVerdier() = UtenlandsoppdragetDto(
    utsendelseLand = "SE",
    arbeidstakerUtsendelsePeriode = periodeDtoMedDefaultVerdier(),
    arbeidsgiverHarOppdragILandet = true,
    arbeidstakerBleAnsattForUtenlandsoppdraget = false,
    arbeidstakerForblirAnsattIHelePerioden = true,
    arbeidstakerErstatterAnnenPerson = false,
    arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = true,
    utenlandsoppholdetsBegrunnelse = "Prosjekt",
    ansettelsesforholdBeskrivelse = "Samme stilling",
    forrigeArbeidstakerUtsendelsePeriode = null
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

fun offshoreDtoMedDefaultVerdier() = OffshoreDto(
    navnPaInnretning = "Test Platform",
    typeInnretning = TypeInnretning.PLATTFORM_ELLER_ANNEN_FAST_INNRETNING,
    sokkelLand = "NO"
)

fun paSkipDtoMedDefaultVerdier() = PaSkipDto(
    navnPaSkip = "MS Test Ship",
    yrketTilArbeidstaker = "Skipsfører",
    seilerI = Farvann.INTERNASJONALT_FARVANN,
    flaggland = "NO",
    territorialfarvannLand = null
)

fun omBordPaFlyDtoMedDefaultVerdier() = OmBordPaFlyDto(
    hjemmebaseLand = "NO",
    hjemmebaseNavn = "Oslo Airport",
    erVanligHjemmebase = true,
    vanligHjemmebaseLand = null,
    vanligHjemmebaseNavn = null
)

fun submitSkjemaRequestMedDefaultVerdier() = SubmitSkjemaRequest(
    bekreftetRiktighet = true,
    submittedAt = Instant.now()
)

fun familiemedlemmerDtoMedDefaultVerdier() = FamiliemedlemmerDto(
    sokerForBarnUnder18SomSkalVaereMed = false,
    harEktefellePartnerSamboerEllerBarnOver18SomSenderEgenSoknad = false
)

fun utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier() = UtenlandsoppdragetArbeidstakersDelDto(
    utsendelsesLand = "SV",
    utsendelsePeriode = periodeDtoMedDefaultVerdier()
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
    organisasjonsnummer = korrektSyntetiskOrgnr
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
    skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier(),
    familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier(),
    tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier()
)

fun createDefaultMetadata(
    representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
    harFullmakt: Boolean = false,
    arbeidsgiverNavn: String? = null,
    fullmektigFnr: String? = null
): JsonNode {
    val objectMapper = jacksonObjectMapper()
    val metadata = UtsendtArbeidstakerMetadata(
        representasjonstype = representasjonstype,
        harFullmakt = harFullmakt,
        radgiverfirma = null,
        arbeidsgiverNavn = arbeidsgiverNavn,
        fullmektigFnr = fullmektigFnr
    )
    return objectMapper.valueToTree(metadata)
}

fun skjemaMedDefaultVerdier(
    fnr: String? = korrektSyntetiskFnr,
    orgnr: String? = korrektSyntetiskOrgnr,
    status: SkjemaStatus = SkjemaStatus.UTKAST,
    type: String = "A1",
    data: JsonNode? = null,
    metadata: JsonNode? = createDefaultMetadata(),
    opprettetDato: Instant = Instant.now(),
    endretDato: Instant = Instant.now(),
    opprettetAv: String = fnr ?: korrektSyntetiskFnr,
    endretAv: String = fnr ?: korrektSyntetiskFnr
): Skjema {
    return Skjema(
        status = status,
        type = type,
        fnr = fnr,
        orgnr = orgnr,
        data = data,
        metadata = metadata,
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
    organisasjonsnummer = korrektSyntetiskOrgnr,
    navn = navnMedDefaultVerdier()
)

fun juridiskEnhetMedDefaultVerdier() = JuridiskEnhet(
    organisasjonsnummer = korrektSyntetiskOrgnr,
    navn = navnMedDefaultVerdier(),
    type = "JuridiskEnhet"
)

fun virksomhetMedDefaultVerdier() = Virksomhet(
    organisasjonsnummer = korrektSyntetiskOrgnr,
    navn = navnMedDefaultVerdier(),
    type = "Virksomhet"
)

fun organisasjonsleddMedDefaultVerdier() = Organisasjonsledd(
    organisasjonsnummer = korrektSyntetiskOrgnr,
    navn = navnMedDefaultVerdier(),
    type = "Organisasjonsledd"
)

// Repr/Fullmakt test data

fun fullmaktMedDefaultVerdier() = Fullmakt(
    fullmaktsgiver = "12345678901",
    fullmektig = "98765432109",
    leserettigheter = listOf("MED"),
    skriverettigheter = listOf("MED")
)

fun innsendingMedDefaultVerdier(
    skjema: Skjema = skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT),
    status: InnsendingStatus = InnsendingStatus.MOTTATT,
    antallForsok: Int = 0,
    opprettetDato: Instant = Instant.now(),
    sisteForsoekTidspunkt: Instant? = null,
    feilmelding: String? = null
) = Innsending(
    skjema = skjema,
    status = status,
    antallForsok = antallForsok,
    opprettetDato = opprettetDato,
    sisteForsoekTidspunkt = sisteForsoekTidspunkt,
    feilmelding = feilmelding
)
