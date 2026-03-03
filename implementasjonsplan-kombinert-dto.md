# Implementasjonsplan: Kombinert arbeidsgiver + arbeidstaker DTO

## Mål

Implementere en ny kombinert DTO (`UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto`) som holder
både arbeidsgiver- og arbeidstakerdata i ett skjema. Refaktorere REST-endepunkter slik at
`/arbeidsgiver/` og `/arbeidstaker/` path-segmentene fjernes for det kombinerte skjemaet.
Splitte ut "utsendingsperiode og land" til et nytt delt steg.

## Designprinsipper

- Ny kombinert DTO grupperer data i nestede `ArbeidsgiversData` og `ArbeidstakersData` inner classes
- `tilleggsopplysninger` ligger på toppnivå (delt mellom begge parter)
- `utsendingsperiodeOgLand` (nytt) ligger også på toppnivå - nytt delt steg med `utsendelseLand: LandKode` og `utsendelsePeriode: PeriodeDto`
- `arbeidsgiversData` og `arbeidstakersData` bruker tomme objekter som default (ikke nullable)
- Der felter overlapper mellom arbeidsgiver og arbeidstaker, tar arbeidsgiver presedens
- `UtenlandsoppdragetArbeidstakersDelDto` sine felter (`utsendelsesLand`, `utsendelsePeriode`) flyttes til ny `UtsendingsperiodeOgLandDto`
- `UtenlandsoppdragetDto` (arbeidsgiver) mister `utsendelseLand` og `arbeidstakerUtsendelsePeriode` (flyttes til `UtsendingsperiodeOgLandDto`)
- De to private update-metodene slås sammen til en generisk `updateSkjemaData()`
- Gamle `/arbeidsgiver/{skjemaId}/...` og `/arbeidstaker/{skjemaId}/...` endepunkter erstattes med `/{skjemaId}/...` endepunkter
- Kombinert flyt trenger IKKE `SkjemaKoblingService`
- Ny `Skjemadel` enum-verdi: `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL`

## Konvensjoner

- Save method naming: `save{DtoClassNameMinusDto}` (f.eks. `saveUtenlandsoppdraget` ikke `saveUtenlandsoppdragInfo`)
- `SkjemaKoblingService` trenger revisjon i separat branch — `finnMatch()` har for mange moving parts og `motpart()` vil krasje for combined type
- Ikke i produksjon, så breaking frontend-endringer er OK

---

## Gjennomført (steg 1–13) ✅

### Steg 1: Ny `UtsendingsperiodeOgLandDto` og kombinert DTO
- Opprettet `UtsendingsperiodeOgLandDto.kt` i `types/felles/`
- Opprettet `UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.kt` i `types/arbeidsgiverOgArbeidstaker/`
- Registrert i Jackson `@JsonSubTypes` i `SkjemaData.kt`

### Steg 2: Fjernet land/periode fra `UtenlandsoppdragetDto`
- Fjernet `utsendelseLand` og `arbeidstakerUtsendelsePeriode` fra `UtenlandsoppdragetDto`
- Oppdatert `UtenlandsoppdragetValidator`, testdata, `SeksjonRenderer`

### Steg 3: Oppdatert kombinert DTO
- Lagt til `utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null` på toppnivå

### Steg 4: Ny `Skjemadel` enum-verdi
- `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL` lagt til i `Skjemadel` enum

### Steg 5: Oppdatert `emptyData()`
- Ny branch i `SkjemaExtensions.emptyData()` for `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL`

### Steg 6: `UtsendingsperiodeOgLand` i alle tre DTOer + sletting av `UtenlandsoppdragetArbeidstakersDelDto`
- Lagt til `utsendingsperiodeOgLand` i arbeidsgiver- og arbeidstaker-DTOene
- Slettet `UtenlandsoppdragetArbeidstakersDelDto` helt — felter flyttet til `UtsendingsperiodeOgLandDto`
- Oppgradert `UtsendtArbeidstakerSkjemaData` interface til å ha `tilleggsopplysninger` og `utsendingsperiodeOgLand`
- Oppdatert `SkjemaKoblingService` — forenklet `hentPeriode()` til en one-liner via interface
- Kaskadefikser gjennom `M2MSkjemaService`, `SeksjonRenderer`, `UtsendtArbeidstakerService`, `UtsendtArbeidstakerController`, `ApiInputValidator`

### Steg 7: Oppdatert `M2MSkjemaService`
- Begge `when`-uttrykk håndterer `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL`
- Slettet `hentArbeidstakerOgArbeidsgiverData()` Pair-logikk — erstattet med direkte `skjema.data`

### Steg 8: Generisk `updateSkjemaData()`
- Slått sammen to private update-metoder til `inline fun <reified T : UtsendtArbeidstakerSkjemaData> updateSkjemaData()`
- Med `default: () -> T` factory-parameter — constructor references som `::UtsendtArbeidstakerArbeidsgiversSkjemaDataDto` brukes på call sites
- Renamed alle `save*Info` → `save{DtoClassNameMinusDto}`

### Steg 9: Refaktorert controller-endepunkter
- Fjernet `/arbeidsgiver/` og `/arbeidstaker/` fra alle URL-er
- Slått sammen to `tilleggsopplysninger`-endepunkter til ett delt

### Steg 10: Oppdatert `InnsendtSkjemaResponse`
- Endret fra `arbeidstakerData`/`arbeidsgiverData` til enkelt polymorfisk `skjemaData: UtsendtArbeidstakerSkjemaData`
- Jackson `@JsonTypeInfo` på interface håndterer serialisering automatisk

### Steg 11: Oppdatert `ArbeidstakerVarslingService`
- Lagt til early return guard for `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL` (skip all varsling)

### Steg 12: Oppdatert PDF-generering
- Endret `SkjemaPdfData` fra `arbeidstakerData`/`arbeidsgiverData` til `skjemaData` + `kobletSkjemaData`
- Oppdatert `HtmlDokumentGenerator` med polymorfisk dispatch via `when` på DTO-type
- Lagt til `byggKombinertDel()`, `byggKombinertArbeidsgiversSeksjoner()`, `byggKombinertArbeidstakersSeksjoner()`

### Steg 13: Renamed validator
- `UtenlandsoppdragetArbeidstakersDelValidator` → `UtsendingsperiodeOgLandValidator`

### Commits
- `958b0a4`: "Refaktorer til kombinert DTO med felles utsendingsperiode" — steg 1-11
- `bfd766c`: "Oppdater PDF-generering til polymorfisk SkjemaPdfData" — steg 12
- `20a445c`: "Flytt og rename validator til UtsendingsperiodeOgLandValidator" — steg 13

### Kompileringsstatus
- **`./gradlew compileKotlin` passerer** ✅
- **`./gradlew compileTestKotlin` feiler** ❌ (5 testfiler)

---

## Discoveries (tekniske oppdagelser underveis)

1. **Jackson polymorfisme**: `SkjemaData` bruker `@JsonTypeInfo`/`@JsonSubTypes` for polymorfisk deserialisering basert på `type`-property — ny kombinert DTO registrert med navn `"UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL"`

2. **Skjemadel-flyt**: Settes ved opprettelse i `OpprettSoknadMedKontekstRequest.skjemadel` og lagres i metadata — driver empty data creation, kobling-logikk og M2M data assembly

3. **Interface-oppgradering**: `UtsendtArbeidstakerSkjemaData` interface definerer nå `tilleggsopplysninger: TilleggsopplysningerDto?` og `utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto?` — alle tre DTOer overstyr. Forenklet `SkjemaKoblingService.hentPeriode()` fra 3-branch `when` til en one-liner

4. **`.copy()` er ikke polymorfisk**: `saveTilleggsopplysninger` trenger fortsatt en `when`-blokk fordi `.copy()` genereres per data class

5. **`SkjemaKoblingService.finnOgKobl()`**: Vil krasje for kombinert skjemadel ved motpart-søk — trenger guard, men utsettes til separat branch

6. **Tilgangskontroll**: `harInnloggetBrukerTilgangTilSkjema()` — DEG_SELV → fnr-match, ARBEIDSGIVER/RADGIVER → Altinn-tilgang, ARBEIDSGIVER_MED_FULLMAKT/RADGIVER_MED_FULLMAKT/ANNEN_PERSON → fullmektigFnr-match + repr-api

7. **`Skjema.validerTyper()`**: Sjekker kun `is UtsendtArbeidstakerSkjemaData` så ny kombinert DTO er allerede valid

8. **Generisk update**: `updateSkjemaData()` bruker `inline fun <reified T>` med `default: () -> T` factory

9. **`InnsendtSkjemaResponse`**: Bruker nå single polymorphic `skjemaData` felt — Jackson `@JsonTypeInfo` håndterer alt

10. **`SkjemaPdfData`**: Endret til `skjemaData: UtsendtArbeidstakerSkjemaData` + `kobletSkjemaData: UtsendtArbeidstakerSkjemaData?`

11. **Test-typo**: Data class field `expectedDataAfterost` (mangler 'P') men alle call sites brukte `expectedDataAfterPost` — allerede broken før våre endringer

---

## Nye controller-endepunkter (URL-referanse)

| Endepunkt | URL |
|-----------|-----|
| Arbeidsgiverens virksomhet | `POST /{skjemaId}/arbeidsgiverens-virksomhet-i-norge` |
| Utenlandsoppdraget (AG) | `POST /{skjemaId}/utenlandsoppdraget` |
| Arbeidstakerens lønn | `POST /{skjemaId}/arbeidstakerens-lonn` |
| Arbeidssted i utlandet | `POST /{skjemaId}/arbeidssted-i-utlandet` |
| Utsendingsperiode og land | `POST /{skjemaId}/utsendingsperiode-og-land` |
| Arbeidssituasjon | `POST /{skjemaId}/arbeidssituasjon` |
| Skatteforhold og inntekt | `POST /{skjemaId}/skatteforhold-og-inntekt` |
| Familiemedlemmer | `POST /{skjemaId}/familiemedlemmer` |
| Tilleggsopplysninger (delt) | `POST /{skjemaId}/tilleggsopplysninger` |
| Send inn | `POST /{id}/send-inn` |
| Kvittering | `GET /{id}/innsendt-kvittering` |
| Arbeidsgiver view | `GET /{id}/arbeidsgiver-view` |
| Arbeidstaker view | `GET /{id}/arbeidstaker-view` |
| PDF | `GET /{id}/pdf` |
| Innsendt | `GET /{id}/innsendt` |

---

## Steg 14: Fiks alle ødelagte testfiler (PÅGÅR)

`./gradlew compileTestKotlin` feiler med feil i 5 filer. Her er detaljert feilbeskrivelse og nødvendige fikser per fil.

### Nøkkelendringer som påvirker testene

| Hva som er endret | Gammel | Ny |
|-------------------|--------|-----|
| Arbeidstaker DTO: `utenlandsoppdraget` felt | `utenlandsoppdraget: UtenlandsoppdragetArbeidstakersDelDto?` | `utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto?` |
| Arbeidsgiver DTO: periode i utenlandsoppdraget | `utenlandsoppdraget.arbeidstakerUtsendelsePeriode` | `utsendingsperiodeOgLand.utsendelsePeriode` |
| Arbeidsgiver DTO: land i utenlandsoppdraget | `utenlandsoppdraget.utsendelseLand` | `utsendingsperiodeOgLand.utsendelseLand` |
| `UtenlandsoppdragetArbeidstakersDelDto` | Eksisterte | **Slettet** |
| `SkjemaPdfData` constructor | `arbeidstakerData: AT?, arbeidsgiverData: AG?` | `skjemaData: UtsendtArbeidstakerSkjemaData, kobletSkjemaData: UtsendtArbeidstakerSkjemaData?` |
| Controller URL-er | `/arbeidsgiver/{id}/...` og `/arbeidstaker/{id}/...` | `/{id}/...` |
| Tilleggsopplysninger | Separate AG/AT endepunkter | Ett delt endepunkt |

### 14.1: `TestData.kt` (3 feil)

- **Linje ~53**: Import av slettet `UtenlandsoppdragetArbeidstakersDelDto` → fjern import
- **Linje ~183-186**: Funksjon `utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier()` refererer slettet DTO → slett funksjonen, legg til:
  ```kotlin
  fun utsendingsperiodeOgLandDtoMedDefaultVerdier() = UtsendingsperiodeOgLandDto(
      utsendelseLand = LandKode.SE,
      utsendelsePeriode = periodeDtoMedDefaultVerdier()
  )
  ```
- **Linje ~251**: `arbeidstakersSkjemaDataDtoMedDefaultVerdier()` bruker `utenlandsoppdraget = ...` → endre til `utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier()`

### 14.2: `UtsendtArbeidstakerControllerIntegrationTest.kt` (17 feil)

- **Linje 67**: Typo `expectedDataAfterost` → rename til `expectedDataAfterPost`
- **Linje 269, 296**: URL-er inneholder `/arbeidsgiver/` og `/arbeidstaker/` → fjern path-segmenter
- **Linje 372-399**: `arbeidsgiverEndpointsSomKreverTilgang()` — alle URL-er trenger `/arbeidsgiver/` fjernet
- **Linje 431-469**: `arbeidstakerEndpointsSomKreverTilgang()` — alle URL-er trenger `/arbeidstaker/` fjernet, erstatt `utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier()` med `utsendingsperiodeOgLandDtoMedDefaultVerdier()`
- **Linje 494-508**: `postEndpoints()` — oppdater URL-er, slå sammen duplikat tilleggsopplysninger
- **Linje 552-591**: `arbeidstakerStegTestFixtures()` — erstatt `utenlandsoppdraget`-steg med `utsendingsperiode-og-land`, erstatt DTO-referanser
- **Linje 616-695**: `endepunkterMedUgyldigData()` — oppdater URL-er, erstatt validerings-test for `UtenlandsoppdragetDto` (gammel brukte `arbeidstakerUtsendelsePeriode` som ikke finnes lenger → bruk `arbeidsgiverHarOppdragILandet=false` + blank `utenlandsoppholdetsBegrunnelse`), erstatt arbeidstaker `utenlandsoppdraget` validering med `utsendingsperiode-og-land` + `UtsendingsperiodeOgLandDto`
- **Design**: Legg til `applyFixture()` helper, skjema factory helpers, oppdater fixture data class med nye felter

### 14.3: `SkjemaKoblingServiceTest.kt` (24 feil)

Alle feil er samme mønster gjentatt ~12 ganger:
- `arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(utenlandsoppdraget = ...)` → `.copy(utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier().copy(utsendelsePeriode = thePeriode))`
- `arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(utenlandsoppdraget = ...copy(arbeidstakerUtsendelsePeriode = ...))` → `.copy(utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(utsendelseLand = LandKode.SE, utsendelsePeriode = thePeriode))`
- Legg til imports: `utsendingsperiodeOgLandDtoMedDefaultVerdier`, `LandKode`, `UtsendingsperiodeOgLandDto`

### 14.4: `PdfGeneratorTest.kt` (5 feil)

- **Linje ~63-78**: `lagSkjemaPdfData()` helper bruker gammel constructor `arbeidstakerData`/`arbeidsgiverData` → endre til `skjemaData`/`kobletSkjemaData`
- **Linje ~530**: `lagKomplettArbeidstakerData()` bruker `utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier()` → endre til `utsendingsperiodeOgLand = utsendingsperiodeOgLandDtoMedDefaultVerdier()`
- **Linje ~37**: Import av `utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier` → erstatt med `utsendingsperiodeOgLandDtoMedDefaultVerdier`
- **Nøkkelvalg**: `lagSkjemaPdfData()` signatur endres til å matche ny `SkjemaPdfData` constructor

### 14.5: `M2MSkjemaServiceIntegrationTest.kt` (2 feil)

- **Linje ~47-51**: `arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(utenlandsoppdraget = ...)` → `.copy(utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(utsendelseLand = LandKode.SE, utsendelsePeriode = overlappendePeriode))`
- **Linje ~53-56**: `arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(utenlandsoppdraget = ...copy(arbeidstakerUtsendelsePeriode = ...))` → `.copy(utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(utsendelseLand = LandKode.SE, utsendelsePeriode = overlappendePeriode))`
- **Linje ~24**: Import: erstatt `utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier` med `utsendingsperiodeOgLandDtoMedDefaultVerdier`, legg til `UtsendingsperiodeOgLandDto`, `LandKode`

---

## Testinfrastruktur-referanser

- `src/test/kotlin/no/nav/melosys/skjema/ApiTestBase.kt` — test base class med SpringBootTest, MockOAuth2Server
- `src/test/kotlin/no/nav/melosys/skjema/MockOAuth2ServerExtensions.kt` — `getToken()` helper med `pid` claim
- `src/test/kotlin/no/nav/melosys/skjema/WireMockInitializer.kt` — WireMock setup
- `src/main/kotlin/no/nav/melosys/skjema/integrasjon/repr/ReprService.kt` — Spring @Service, IKKE mocked i nåværende controller integration test

### Regler for test-rewrite
- Start med `DEG_SELV` representasjonstype for cross-product tester, utvid til `ARBEIDSGIVER` og `ARBEIDSGIVER_MED_FULLMAKT` senere
- `@MockkBean` for `ReprService` skal kun legges til når det faktisk trengs (for `ARBEIDSGIVER_MED_FULLMAKT` tester)
- Lag `applyFixture(fixture)` helper som populerer DB hvis `existingSkjemaer` er satt, setter opp Altinn mock hvis `altinnHarTilgang` er non-null, setter opp repr mock hvis `reprHarFullmakt` er non-null
- `altinnHarTilgang` og `reprHarFullmakt` skal være nullable med default null (betyr "ikke mock")

---

## Steg 15: Kjør alle tester

```bash
./gradlew clean build
```

Fiks eventuelle runtime testfeil.

---

## Steg 16: Revidere tilgangsstyring for skriveoperasjoner

Nå som `/arbeidsgiver/` og `/arbeidstaker/` path-segmentene er fjernet og alle skriveendepunkter
ligger under `/{skjemaId}/...`, må tilgangsstyringen revideres:

- Gjennomgå `hentSkjemaMedTilgangsstyring()` og vurder om den gir riktig tilgangskontroll
  for det kombinerte skjemaet (`ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL`)
- Vurder om det trengs differensiert tilgangsstyring per seksjon
- For det kombinerte skjemaet: Bestem hvem som har skrivetilgang til hvilke seksjoner
- Oppdater tilgangsstyring i `UtsendtArbeidstakerService` der nødvendig

**NB:** Dette steget tas helt til slutt, etter at alt annet er på plass og tester passerer.

---

## Relevante filer (referanse)

### Opprettet:
- `melosys-skjema-api-types/src/main/kotlin/no/nav/melosys/skjema/types/arbeidsgiverOgArbeidstaker/UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.kt`
- `melosys-skjema-api-types/src/main/kotlin/no/nav/melosys/skjema/types/felles/UtsendingsperiodeOgLandDto.kt`

### Modifisert (types-modul):
- `melosys-skjema-api-types/.../types/SkjemaData.kt` — JsonSubTypes for kombinert DTO
- `melosys-skjema-api-types/.../types/UtsendtArbeidstakerSkjemaData.kt` — interface med tilleggsopplysninger og utsendingsperiodeOgLand
- `melosys-skjema-api-types/.../types/UtsendtArbeidstakerMetadata.kt` — ny Skjemadel enum, Representasjonstype enum
- `melosys-skjema-api-types/.../arbeidsgiver/UtsendtArbeidstakerArbeidsgiversSkjemaDataDto.kt`
- `melosys-skjema-api-types/.../arbeidsgiver/utenlandsoppdraget/UtenlandsoppdragetDto.kt`
- `melosys-skjema-api-types/.../arbeidstaker/UtsendtArbeidstakerArbeidstakersSkjemaDataDto.kt`
- `melosys-skjema-api-types/.../types/InnsendtSkjemaResponse.kt`

### Slettet:
- `melosys-skjema-api-types/.../arbeidstaker/utenlandsoppdraget/UtenlandsoppdragetArbeidstakersDelDto.kt`
- `src/main/kotlin/.../validators/utenlandsoppdraget/UtenlandsoppdragetArbeidstakersDelValidator.kt`

### Modifisert (hovedapp):
- `src/main/kotlin/.../controller/UtsendtArbeidstakerController.kt`
- `src/main/kotlin/.../service/UtsendtArbeidstakerService.kt`
- `src/main/kotlin/.../extensions/SkjemaExtensions.kt`
- `src/main/kotlin/.../service/SkjemaKoblingService.kt`
- `src/main/kotlin/.../service/M2MSkjemaService.kt`
- `src/main/kotlin/.../service/ArbeidstakerVarslingService.kt`
- `src/main/kotlin/.../pdf/SkjemaPdfData.kt`
- `src/main/kotlin/.../pdf/HtmlDokumentGenerator.kt`
- `src/main/kotlin/.../pdf/SeksjonRenderer.kt`
- `src/main/kotlin/.../validators/ApiInputValidator.kt`
- `src/main/kotlin/.../validators/utenlandsoppdraget/UtenlandsoppdragetValidator.kt`
- `src/main/kotlin/.../validators/utsendingsperiodeogland/UtsendingsperiodeOgLandValidator.kt` (opprettet, renamed)

### Testfiler (ØDELAGT, må fikses i steg 14):
- `src/test/kotlin/no/nav/melosys/skjema/TestData.kt`
- `src/test/kotlin/no/nav/melosys/skjema/controller/UtsendtArbeidstakerControllerIntegrationTest.kt`
- `src/test/kotlin/no/nav/melosys/skjema/service/SkjemaKoblingServiceTest.kt`
- `src/test/kotlin/no/nav/melosys/skjema/pdf/PdfGeneratorTest.kt`
- `src/test/kotlin/no/nav/melosys/skjema/service/M2MSkjemaServiceIntegrationTest.kt`
