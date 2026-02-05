package no.nav.melosys.skjema

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import no.nav.melosys.skjema.integrasjon.ereg.dto.Adresse
import no.nav.melosys.skjema.integrasjon.ereg.dto.Bruksperiode
import no.nav.melosys.skjema.integrasjon.ereg.dto.Gyldighetsperiode
import no.nav.melosys.skjema.integrasjon.ereg.dto.InngaarIJuridiskEnhet
import no.nav.melosys.skjema.integrasjon.ereg.dto.JuridiskEnhet
import no.nav.melosys.skjema.integrasjon.ereg.dto.Navn
import no.nav.melosys.skjema.integrasjon.ereg.dto.Organisasjonsledd
import no.nav.melosys.skjema.integrasjon.ereg.dto.Virksomhet
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.melosys.skjema.types.OpprettSoknadMedKontekstRequest
import no.nav.melosys.skjema.types.PersonDto
import no.nav.melosys.skjema.types.RadgiverfirmaInfo
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.SimpleOrganisasjonDto
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.Farvann
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.OffshoreDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.OmBordPaFlyDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaLandDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaLandFastArbeidsstedDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaSkipDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.TypeInnretning
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.Familiemedlem
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.Ansettelsesform
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.NorskVirksomhet
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheter
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheterMedAnsettelsesform
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhet
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhetMedAnsettelsesform
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

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
    utsendelseLand = LandKode.SE,
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
    navnPaVirksomhet = "Test Inc",
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
    navnPaVirksomhet = "Test Inc",
    navnPaInnretning = "Test Platform",
    typeInnretning = TypeInnretning.PLATTFORM_ELLER_ANNEN_FAST_INNRETNING,
    sokkelLand = LandKode.SE
)

fun paSkipDtoMedDefaultVerdier() = PaSkipDto(
    navnPaVirksomhet = "Test Inc",
    navnPaSkip = "MS Test Ship",
    yrketTilArbeidstaker = "Skipsfører",
    seilerI = Farvann.INTERNASJONALT_FARVANN,
    flaggland = LandKode.SE,
    territorialfarvannLand = null
)

fun omBordPaFlyDtoMedDefaultVerdier() = OmBordPaFlyDto(
    navnPaVirksomhet = "Test Inc",
    hjemmebaseLand = LandKode.SE,
    hjemmebaseNavn = "Oslo Airport",
    erVanligHjemmebase = true,
    vanligHjemmebaseLand = null,
    vanligHjemmebaseNavn = null
)

fun familiemedlemMedDefaultVerdier() = Familiemedlem(
    fornavn = "John",
    etternavn = "Doe",
    harNorskFodselsnummerEllerDnummer = true,
    fodselsnummer = korrektSyntetiskFnr,
    fodselsdato = null
)

fun familiemedlemmerDtoMedDefaultVerdier() = FamiliemedlemmerDto(
    skalHaMedFamiliemedlemmer = false,
    familiemedlemmer = emptyList()
)

fun utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier() = UtenlandsoppdragetArbeidstakersDelDto(
    utsendelsesLand = LandKode.SE,
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

fun utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier() = UtenlandskVirksomhetMedAnsettelsesform(
    navn = "Foreign Company Ltd",
    organisasjonsnummer = "ABC123",
    vegnavnOgHusnummer = "Main Street 123",
    bygning = "Building A",
    postkode = "12345",
    byStedsnavn = "Stockholm",
    region = "Stockholm County",
    land = "SE",
    tilhorerSammeKonsern = true,
    ansettelsesform = Ansettelsesform.ARBEIDSTAKER_ELLER_FRILANSER
)

fun norskeOgUtenlandskeVirksomheterMedDefaultVerdier() = NorskeOgUtenlandskeVirksomheter(
    norskeVirksomheter = listOf(norskVirksomhetMedDefaultVerdier()),
    utenlandskeVirksomheter = listOf(utenlandskVirksomhetMedDefaultVerdier())
)

fun norskeOgUtenlandskeVirksomheterMedAnsettelsesformMedDefaultVerdier() = NorskeOgUtenlandskeVirksomheterMedAnsettelsesform(
    norskeVirksomheter = listOf(norskVirksomhetMedDefaultVerdier()),
    utenlandskeVirksomheter = listOf(utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier())
)

fun arbeidstakersSkjemaDataDtoMedDefaultVerdier() = ArbeidstakersSkjemaDataDto(
    skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier(),
    familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier(),
    tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier(),
    utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier(),
    arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier(),
)

fun utsendtArbeidstakerMetadataMedDefaultVerdier(
    representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
    harFullmakt: Boolean = false,
    skjemadel: Skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
    arbeidsgiverNavn: String = "Test Arbeidsgiver AS",
    fullmektigFnr: String? = null,
    radgiverfirma: RadgiverfirmaInfo? = null,
    juridiskEnhetOrgnr: String = korrektSyntetiskOrgnr,
    kobletSkjemaId: UUID? = null,
): UtsendtArbeidstakerMetadata {

    return UtsendtArbeidstakerMetadata(
        representasjonstype = representasjonstype,
        harFullmakt = harFullmakt,
        skjemadel = skjemadel,
        arbeidsgiverNavn = arbeidsgiverNavn,
        fullmektigFnr = fullmektigFnr,
        radgiverfirma = radgiverfirma,
        juridiskEnhetOrgnr = juridiskEnhetOrgnr,
        kobletSkjemaId = kobletSkjemaId,
    )
}

fun radgiverfirmaInfoMedDefaultVerdier(
    navn: String = "Rådgiverfirma AS",
    orgnr: String = "987654321"
): RadgiverfirmaInfo {
    return RadgiverfirmaInfo(
        orgnr = orgnr,
        navn = navn,
    )
}

fun personDtoMedDefaultVerdier(
    fnr: String = korrektSyntetiskFnr,
    etternavn: String? = "Testesen"
) = PersonDto(
    fnr = fnr,
    etternavn = etternavn
)

fun simpleOrganisasjonDtoMedDefaultVerdier(
    orgnr: String = korrektSyntetiskOrgnr,
    navn: String = "Test AS"
) = SimpleOrganisasjonDto(
    orgnr = orgnr,
    navn = navn
)

fun opprettSoknadMedKontekstRequestMedDefaultVerdier(
    representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
    skjemadel: Skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
    radgiverfirma: SimpleOrganisasjonDto? = null,
    arbeidsgiver: SimpleOrganisasjonDto = simpleOrganisasjonDtoMedDefaultVerdier(),
    arbeidstaker: PersonDto = personDtoMedDefaultVerdier(),
    harFullmakt: Boolean = false
) = OpprettSoknadMedKontekstRequest(
    representasjonstype = representasjonstype,
    skjemadel = skjemadel,
    radgiverfirma = radgiverfirma,
    arbeidsgiver = arbeidsgiver,
    arbeidstaker = arbeidstaker,
    harFullmakt = harFullmakt
)

fun utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
    representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
    harFullmakt: Boolean = false,
    arbeidsgiverNavn: String = "Test Arbeidsgiver AS",
    fullmektigFnr: String? = null,
    radgiverfirma: RadgiverfirmaInfo? = null,
    skjemadel: Skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
    juridiskEnhetOrgnr: String = korrektSyntetiskOrgnr,
    kobletSkjemaId: UUID? = null
): JsonNode {

    return JsonMapper.builder().addModule(kotlinModule()).build().valueToTree(utsendtArbeidstakerMetadataMedDefaultVerdier(
        representasjonstype = representasjonstype,
        harFullmakt = harFullmakt,
        arbeidsgiverNavn = arbeidsgiverNavn,
        fullmektigFnr = fullmektigFnr,
        radgiverfirma = radgiverfirma,
        skjemadel = skjemadel,
        juridiskEnhetOrgnr = juridiskEnhetOrgnr,
        kobletSkjemaId = kobletSkjemaId
    ))
}

fun skjemaMedDefaultVerdier(
    id: UUID? = null,
    fnr: String = korrektSyntetiskFnr,
    orgnr: String = korrektSyntetiskOrgnr,
    status: SkjemaStatus = SkjemaStatus.UTKAST,
    type: SkjemaType = SkjemaType.UTSENDT_ARBEIDSTAKER,
    data: JsonNode? = null,
    metadata: JsonNode = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(),
    opprettetDato: Instant = Instant.now(),
    endretDato: Instant = Instant.now(),
    opprettetAv: String = fnr,
    endretAv: String = fnr
): Skjema {
    return Skjema(
        id = id,
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
    id: UUID? = null,
    skjema: Skjema = skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT),
    status: InnsendingStatus = InnsendingStatus.MOTTATT,
    antallForsok: Int = 0,
    opprettetDato: Instant = Instant.now(),
    sisteForsoekTidspunkt: Instant? = null,
    feilmelding: String? = null,
    referanseId: String = "MEL-${UUID.randomUUID().toString().take(6).uppercase()}",
    skjemaDefinisjonVersjon: String = "1",
    innsendtSprak: Språk = Språk.NORSK_BOKMAL
) = Innsending(
    id = id,
    skjema = skjema,
    status = status,
    antallForsok = antallForsok,
    opprettetDato = opprettetDato,
    sisteForsoekTidspunkt = sisteForsoekTidspunkt,
    feilmelding = feilmelding,
    referanseId = referanseId,
    skjemaDefinisjonVersjon = skjemaDefinisjonVersjon,
    innsendtSprak = innsendtSprak
)
