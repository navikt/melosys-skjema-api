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
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettUtsendtArbeidstakerSoknadRequest
import no.nav.melosys.skjema.types.felles.PersonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsinntektKilde
import no.nav.melosys.skjema.types.utsendtarbeidstaker.InntektType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverfirmaInfo
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.felles.SimpleOrganisasjonDto
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Farvann
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OffshoreDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OmBordPaFlyDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandFastArbeidsstedDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaSkipDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.TypeInnretning
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Familiemedlem
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
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

// Defaultverdiene tar utgangspunkt i gyldige data hva gjelder formater og sammenhenger mtp validatorene (no/nav/melosys/skjema/controller/validators).
// NB! Endringer i defaultverdier i testdata skal sjeldent føre til at tester feiler.
// Hvis endringer i verdier her skulle føre til at tester feiler, så fiks det ved å overstyre verdiene i de feilende testene, ikke verdiene i TestData.
// Med unntak av tilfeller hvor defaultverdiene har ugyldige formater og kombinasjoner mtp validatorene (no/nav/melosys/skjema/controller/validators).


val korrektSyntetiskFnr = "01816023404" // HANS HANSEN (mock)
val etAnnetKorrektSyntetiskFnr = "10908012327" // SIRI SANSEN (mock)

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

fun arbeidsgiversSkjemaDataDtoMedDefaultVerdier() = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
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

fun utsendingsperiodeOgLandDtoMedDefaultVerdier() = UtsendingsperiodeOgLandDto(
    utsendelseLand = LandKode.SE,
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
    pengestotteSomMottasFraAndreLandBeskrivelse = null,
    inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
    hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
    inntekt = null,
    inntektFraEgenVirksomhet = null
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

fun arbeidstakersSkjemaDataDtoMedDefaultVerdier() = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
    skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier(),
    familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier(),
    tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier(),
    utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
    arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier(),
)

fun arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier() = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(
    arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
        arbeidsgiverensVirksomhetINorge = arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier(),
        utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier(),
        arbeidstakerensLonn = arbeidstakerensLonnDtoMedDefaultVerdier(),
    ),
    arbeidstakersData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData(
        arbeidssituasjon = arbeidssituasjonDtoMedDefaultVerdier(),
        skatteforholdOgInntekt = skatteforholdOgInntektDtoMedDefaultVerdier(),
        familiemedlemmer = familiemedlemmerDtoMedDefaultVerdier(),
    ),
    utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier(),
    tilleggsopplysninger = tilleggsopplysningerDtoMedDefaultVerdier(),
)

fun utsendtArbeidstakerMetadataMedDefaultVerdier(
    representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
    skjemadel: Skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
    arbeidsgiverNavn: String = "Test Arbeidsgiver AS",
    arbeidstakerNavn: String = "Test Testesen",
    fullmektigFnr: String? = etAnnetKorrektSyntetiskFnr,
    radgiverfirma: RadgiverfirmaInfo? = null,
    juridiskEnhetOrgnr: String = korrektSyntetiskOrgnr,
    kobletSkjemaId: UUID? = null,
    erstatterSkjemaId: UUID? = null,
): UtsendtArbeidstakerMetadata {
    return when (representasjonstype) {
        Representasjonstype.DEG_SELV -> DegSelvMetadata(
            skjemadel = skjemadel,
            arbeidsgiverNavn = arbeidsgiverNavn,
            juridiskEnhetOrgnr = juridiskEnhetOrgnr,
            arbeidstakerNavn = arbeidstakerNavn,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
        Representasjonstype.ARBEIDSGIVER -> ArbeidsgiverMetadata(
            skjemadel = skjemadel,
            arbeidsgiverNavn = arbeidsgiverNavn,
            juridiskEnhetOrgnr = juridiskEnhetOrgnr,
            arbeidstakerNavn = arbeidstakerNavn,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
        Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> ArbeidsgiverMedFullmaktMetadata(
            skjemadel = skjemadel,
            arbeidsgiverNavn = arbeidsgiverNavn,
            juridiskEnhetOrgnr = juridiskEnhetOrgnr,
            fullmektigFnr = fullmektigFnr ?: throw IllegalArgumentException("fullmektigFnr er påkrevd for ARBEIDSGIVER_MED_FULLMAKT"),
            arbeidstakerNavn = arbeidstakerNavn,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
        Representasjonstype.RADGIVER -> RadgiverMetadata(
            skjemadel = skjemadel,
            arbeidsgiverNavn = arbeidsgiverNavn,
            juridiskEnhetOrgnr = juridiskEnhetOrgnr,
            arbeidstakerNavn = arbeidstakerNavn,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId,
            radgiverfirma = radgiverfirma ?: radgiverfirmaInfoMedDefaultVerdier()
        )
        Representasjonstype.RADGIVER_MED_FULLMAKT -> RadgiverMedFullmaktMetadata(
            skjemadel = skjemadel,
            arbeidsgiverNavn = arbeidsgiverNavn,
            juridiskEnhetOrgnr = juridiskEnhetOrgnr,
            fullmektigFnr = fullmektigFnr ?: throw IllegalArgumentException("fullmektigFnr er påkrevd for RADGIVER_MED_FULLMAKT"),
            arbeidstakerNavn = arbeidstakerNavn,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId,
            radgiverfirma = radgiverfirma ?: radgiverfirmaInfoMedDefaultVerdier()
        )
        Representasjonstype.ANNEN_PERSON -> AnnenPersonMetadata(
            skjemadel = skjemadel,
            arbeidsgiverNavn = arbeidsgiverNavn,
            juridiskEnhetOrgnr = juridiskEnhetOrgnr,
            fullmektigFnr = fullmektigFnr ?: throw IllegalArgumentException("fullmektigFnr er påkrevd for ANNEN_PERSON"),
            arbeidstakerNavn = arbeidstakerNavn,
            kobletSkjemaId = kobletSkjemaId,
            erstatterSkjemaId = erstatterSkjemaId
        )
    }
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

fun opprettUtsendtArbeidstakerSoknadRequestMedDefaultVerdier(
    representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV,
    radgiverfirma: SimpleOrganisasjonDto? = null,
    arbeidsgiver: SimpleOrganisasjonDto = simpleOrganisasjonDtoMedDefaultVerdier(),
    arbeidstaker: PersonDto = personDtoMedDefaultVerdier()
) = OpprettUtsendtArbeidstakerSoknadRequest(
    representasjonstype = representasjonstype,
    radgiverfirma = radgiverfirma,
    arbeidsgiver = arbeidsgiver,
    arbeidstaker = arbeidstaker
)

fun skjemaMedDefaultVerdier(
    id: UUID? = null,
    fnr: String = korrektSyntetiskFnr,
    orgnr: String = korrektSyntetiskOrgnr,
    status: SkjemaStatus = SkjemaStatus.UTKAST,
    type: SkjemaType = SkjemaType.UTSENDT_ARBEIDSTAKER,
    data: no.nav.melosys.skjema.types.SkjemaData? = null,
    metadata: UtsendtArbeidstakerMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(),
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
    referanseId: String = UUID.randomUUID().toString().take(6).uppercase(),
    skjemaDefinisjonVersjon: String = "1",
    innsendtSprak: Språk = Språk.NORSK_BOKMAL,
    innsenderFnr: String = "12345678901"
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
    innsendtSprak = innsendtSprak,
    innsenderFnr = innsenderFnr
)
