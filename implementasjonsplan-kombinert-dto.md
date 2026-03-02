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

---

## Allerede gjort

- [x] Opprettet branch `7850_sende_inn_begge_skjemadeler`
- [x] Opprettet `UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.kt` med nestede inner classes
- [x] Registrert ny type i `SkjemaData.kt` Jackson `@JsonSubTypes`
- [x] Verifisert at prosjektet kompilerer

---

## Steg 1: Ny `UtsendingsperiodeOgLandDto`

**Fil:** `melosys-skjema-api-types/src/main/kotlin/no/nav/melosys/skjema/types/felles/UtsendingsperiodeOgLandDto.kt`

Opprett ny data class:

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendingsperiodeOgLandDto(
    val utsendelseLand: LandKode,
    @field:Valid
    val utsendelsePeriode: PeriodeDto
)
```

---

## Steg 2: Fjern land/periode fra `UtenlandsoppdragetDto`

**Fil:** `melosys-skjema-api-types/.../arbeidsgiver/utenlandsoppdraget/UtenlandsoppdragetDto.kt`

Fjern disse to feltene:
- `utsendelseLand: LandKode`
- `arbeidstakerUtsendelsePeriode: PeriodeDto`

Kaskadefikser i:
- `UtenlandsoppdragetValidator` - fjern validering av `arbeidstakerUtsendelsePeriode`
- `SeksjonRenderer.byggUtenlandsoppdragetArbeidsgiver()` - fjern `felt("utsendelseLand", ...)` og `felt("arbeidstakerUtsendelsePeriode", ...)`
- Testdata (`TestData.kt`) - fjern disse feltene fra test-instanser
- `UtenlandsoppdragetValidatorTest` - oppdater tester

---

## Steg 3: Oppdater kombinert DTO

**Fil:** `.../arbeidsgiverOgArbeidstaker/UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.kt`

Legg til `utsendingsperiodeOgLand` på toppnivå:

```kotlin
data class UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(
    override val type: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL",
    val arbeidsgiversData: ArbeidsgiversData = ArbeidsgiversData(),
    val arbeidstakersData: ArbeidstakersData = ArbeidstakersData(),
    val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
) : UtsendtArbeidstakerSkjemaData {
    // ... inner classes uendret
}
```

---

## Steg 4: Ny `Skjemadel` enum-verdi

**Fil:** `.../types/UtsendtArbeidstakerMetadata.kt`

```kotlin
enum class Skjemadel {
    ARBEIDSTAKERS_DEL,
    ARBEIDSGIVERS_DEL,
    ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
}
```

---

## Steg 5: Oppdater `SkjemaExtensions.emptyData()`

**Fil:** `src/.../extensions/SkjemaExtensions.kt`

Legg til ny branch i `when`:

```kotlin
private fun Skjemadel.emptyData(): UtsendtArbeidstakerSkjemaData = when (this) {
    Skjemadel.ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
    Skjemadel.ARBEIDSGIVERS_DEL -> UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
    Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto()
}
```

---

## Steg 6: Oppdater `SkjemaKoblingService`

**Fil:** `src/.../service/SkjemaKoblingService.kt`

- `motpart()`: For `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL` finnes ingen motpart - returner `null` eller kast exception
- `hentPeriode()`: For den kombinerte typen, les fra `utsendingsperiodeOgLand.utsendelsePeriode`
- `finnOgKobl()`: Hopp over kobling for kombinert skjemadel (trenger ikke motpart)

---

## Steg 7: Oppdater `M2MSkjemaService.hentArbeidstakerOgArbeidsgiverData()`

**Fil:** `src/.../service/M2MSkjemaService.kt`

Legg til `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL` case i `when`-blokken.
For kombinert DTO: Map inner classes til separate arbeidstaker/arbeidsgiver DTOer for PDF-generering.

---

## Steg 8: Slå sammen update-metoder

**Fil:** `src/.../service/UtsendtArbeidstakerService.kt`

Slå sammen `updateArbeidsgiverSkjemaDataAndConvertToSkjemaDto()` og
`updateArbeidstakerSkjemaDataAndConvertToSkjemaDto()` til en generisk:

```kotlin
private fun <T : SkjemaData> updateSkjemaData(
    skjemaId: UUID,
    dataClass: KClass<T>,
    defaultFactory: () -> T,
    updateFunction: (T) -> T
): UtsendtArbeidstakerSkjemaDto
```

Oppdater alle `save*`-metoder til å bruke den nye generiske metoden.
For kombinert DTO: Alle save-metoder må håndtere tilfelle der `skjema.data` er den kombinerte typen.

---

## Steg 9: Refaktorer controller-endepunkter

**Fil:** `src/.../controller/UtsendtArbeidstakerController.kt`

### Nye endepunkter (uten `/arbeidsgiver/` og `/arbeidstaker/`):
- `POST /{skjemaId}/arbeidsgiverens-virksomhet-i-norge`
- `POST /{skjemaId}/utenlandsoppdraget` (arbeidsgiver sin versjon)
- `POST /{skjemaId}/arbeidstakerens-lonn`
- `POST /{skjemaId}/arbeidssted-i-utlandet`
- `POST /{skjemaId}/utsendingsperiode-og-land` (nytt)
- `POST /{skjemaId}/arbeidssituasjon`
- `POST /{skjemaId}/skatteforhold-og-inntekt`
- `POST /{skjemaId}/familiemedlemmer`
- `POST /{skjemaId}/tilleggsopplysninger` (slå sammen de to dupliserte)

### Beholde gamle endepunkter:
De eksisterende `/arbeidsgiver/...` og `/arbeidstaker/...` endepunktene beholdes for bakoverkompatibilitet
med separate skjema (ARBEIDSGIVERS_DEL / ARBEIDSTAKERS_DEL).

---

## Steg 10: Oppdater `hentInnsendtSkjema()`

**Fil:** `src/.../service/UtsendtArbeidstakerService.kt`

Håndter tilfelle der `skjema.data` er `UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto`.
Map fra kombinert DTO til separate arbeidstaker/arbeidsgiver data for `InnsendtSkjemaResponse`.

---

## Steg 11: Oppdater `ArbeidstakerVarslingService`

**Fil:** `src/.../service/ArbeidstakerVarslingService.kt`

- `harEksisterendeArbeidstakerUtkast()`: Håndter `ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL`
- For kombinert skjemadel: Ingen varsling til arbeidstaker (begge deler er i ett skjema)

---

## Steg 12: Oppdater PDF-rendering (`SeksjonRenderer`)

**Fil:** `src/.../pdf/SeksjonRenderer.kt`

- Legg til ny metode `byggUtsendingsperiodeOgLand()` for det nye delte steget
- For kombinert DTO: Bruk eksisterende arbeidsgiver/arbeidstaker seksjon-byggere, men les fra inner classes
- Fjern gammel arbeidstaker utenlandsoppdraget-rendering (erstattes av utsendingsperiodeOgLand)

---

## Steg 13: Oppdater `ApiInputValidator`

**Fil:** `src/.../validators/ApiInputValidator.kt`

- Legg til `validate(UtsendingsperiodeOgLandDto)` metode
- Vurder om `UtenlandsoppdragetArbeidstakersDelValidator` kan fjernes (felter flyttet)

---

## Steg 14: Oppdater alle tester

### Filer:
- `TestData.kt` - fjern land/periode fra UtenlandsoppdragetDto testdata, legg til kombinert testdata
- `UtsendtArbeidstakerControllerIntegrationTest.kt` - legg til tester for nye endepunkter
- `SkjemaKoblingServiceTest.kt` - test at kombinert skjemadel ikke kobles
- `ArbeidstakerVarslingServiceTest.kt` - test ny skjemadel
- `M2MSkjemaServiceIntegrationTest.kt` - test kombinert DTO
- `UtenlandsoppdragetValidatorTest.kt` - fjern tester for fjernede felter
- `MetadatatypeTest.kt` - legg til ny skjemadel
- `SkjemaDtoTypeTest.kt` - legg til kombinert type

---

## Steg 15: Bygg og kjør alle tester

```bash
./gradlew clean build
```

Fiks eventuelle kompileringsfeil eller testfeil.

---

## Steg 16: Revidere tilgangsstyring for skriveoperasjoner

Nå som `/arbeidsgiver/` og `/arbeidstaker/` path-segmentene er fjernet og alle skriveendepunkter
ligger under `/{skjemaId}/...`, må tilgangsstyringen revideres:

- Gjennomgå `hentSkjemaMedTilgangsstyring()` og vurder om den gir riktig tilgangskontroll
  for det kombinerte skjemaet (`ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL`)
- Vurder om det trengs differensiert tilgangsstyring per seksjon (f.eks. at arbeidsgiver
  kun kan skrive til arbeidsgiver-seksjoner, og arbeidstaker kun til arbeidstaker-seksjoner)
- For det kombinerte skjemaet: Bestem hvem som har skrivetilgang til hvilke seksjoner
- Oppdater tilgangsstyring i `UtsendtArbeidstakerService` der nødvendig

**NB:** Dette steget tas helt til slutt, etter at alt annet er på plass og tester passerer.

---

## Relevante filer (referanse)

### Allerede modifisert:
- `melosys-skjema-api-types/.../arbeidsgiverOgArbeidstaker/UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.kt`
- `melosys-skjema-api-types/.../types/SkjemaData.kt`

### Types-modulen:
- `melosys-skjema-api-types/.../types/felles/` (ny: `UtsendingsperiodeOgLandDto.kt`)
- `melosys-skjema-api-types/.../arbeidsgiver/utenlandsoppdraget/UtenlandsoppdragetDto.kt`
- `melosys-skjema-api-types/.../types/UtsendtArbeidstakerMetadata.kt`
- `melosys-skjema-api-types/.../arbeidstaker/utenlandsoppdraget/UtenlandsoppdragetArbeidstakersDelDto.kt`

### Hovedapp:
- `src/.../controller/UtsendtArbeidstakerController.kt`
- `src/.../service/UtsendtArbeidstakerService.kt`
- `src/.../extensions/SkjemaExtensions.kt`
- `src/.../service/SkjemaKoblingService.kt`
- `src/.../service/M2MSkjemaService.kt`
- `src/.../service/ArbeidstakerVarslingService.kt`
- `src/.../pdf/SeksjonRenderer.kt`
- `src/.../validators/ApiInputValidator.kt`
- `src/.../validators/utenlandsoppdraget/UtenlandsoppdragetValidator.kt`

### Tester:
- `src/test/.../TestData.kt`
- `src/test/.../controller/UtsendtArbeidstakerControllerIntegrationTest.kt`
- `src/test/.../service/SkjemaKoblingServiceTest.kt`
- `src/test/.../service/ArbeidstakerVarslingServiceTest.kt`
- `src/test/.../service/M2MSkjemaServiceIntegrationTest.kt`
- `src/test/.../validators/utenlandsoppdraget/UtenlandsoppdragetValidatorTest.kt`
- `melosys-skjema-api-types/src/test/.../MetadatatypeTest.kt`
- `melosys-skjema-api-types/src/test/.../SkjemaDtoTypeTest.kt`
