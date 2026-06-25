# E2E-reproduksjonsskript (lokal benk)

Skript som sender inn ekte `UTSENDT_ARBEIDSTAKER`-skjema mot lokal `melosys-skjema-api`
via de virkelige REST-endepunktene, med ekte tokens fra mock-oauth2-server. Brukes til å
reprodusere og verifisere koblings-/journalføringsadferd ende-til-ende.

## Forutsetninger

- Lokal benk oppe (`local-mock`): `melosys-skjema-api` på `:8090`, mock-oauth2-server på
  `:8082`, melosys-mock (PDL/EREG/Altinn) på `:8083`.
- Verktøy: `bash`, `curl`, `jq`. (`pdftotext` valgfritt for å lese PDF-teksten.)
- Testidentiteter fra `melosys-docker-compose/TESTBRUKERE.md`:
  - Arbeidstaker (DEG_SELV): HANS HANSEN `01816023404`
  - Arbeidsgiver (ARBEIDSGIVER): KARAFFEL TRIVIELL `30056928150` (Altinn-tilgang til Ståles Stål)
  - Organisasjon: Ståles Stål AS `999999999`

## Skript

| Skript | Scenario |
|--------|----------|
| `reproduser-arvet-kobling.sh` | Arbeidstaker → arbeidsgiver → **ny versjon** av arbeidstakers del. Den nye versjonen arver kobling til (utdatert) arbeidsgivers del. |
| `reproduser-motpart-ag-forst.sh` | Arbeidsgiver sender sin del **først**, så arbeidstaker. Arbeidstakers del motpart-kobles til arbeidsgivers del. |
| `_repro-lib.sh` | Delt bibliotek (source-es av de andre): env-defaults, testidentiteter og helpere. |

Begge henter til slutt M2M-PDF for arbeidstakers del og viser om den (feilaktig) inneholder
arbeidsgivers del.

### Sende flere søknader

`_repro-lib.sh` gir høynivå-helperne `send_arbeidstakerdel`/`send_arbeidsgiverdel` (opprett +
fyll ut alle steg + send inn i ett kall). De setter `SKJEMA_ID` og `REFERANSE`, så et eget skript
kan sende mange søknader på rad:

```bash
source scripts/_repro-lib.sh
TOK=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSTAKER_FNR")
for i in $(seq 1 5); do
  send_arbeidstakerdel "$TOK"
  echo "sendt #$i: skjemaId=$SKJEMA_ID referanse=$REFERANSE"
  sleep "$SLEEP"
done
```

## Kjøre

```bash
bash scripts/reproduser-arvet-kobling.sh
bash scripts/reproduser-motpart-ag-forst.sh
```

Periode randomiseres by default, så hver kjøring blir en fersk sak. Overstyr ved behov:

```bash
FRA=2030-05-01 TIL=2030-05-31 SLEEP=4 bash scripts/reproduser-motpart-ag-forst.sh
```

`SLEEP` er en pause mellom innsendingene slik at melosys-api rekker å opprette sak + mapping
før neste melding (unngår duplikatsaker ved samtidig prosessering).

## Forventet resultat

- **Før fiks:** PDF for et rent `ARBEIDSTAKERS_DEL`-skjema inneholder både «Arbeidstakers del»
  og «Arbeidsgivers del».
- **Etter fiks (MELOSYS-8092):** samme PDF inneholder kun «Arbeidstakers del».
