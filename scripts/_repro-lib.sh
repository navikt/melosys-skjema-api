#!/usr/bin/env bash
# Delt bibliotek for repro-skriptene. Source-es inn av de andre.
# Identiteter (melosys-docker-compose/TESTBRUKERE.md): arbeidstaker HANS HANSEN 01816023404,
# arbeidsgiver KARAFFEL TRIVIELL 30056928150, org Ståles Stål AS 999999999.

SKJEMA_API=${SKJEMA_API:-http://localhost:8090}
MOCK_OAUTH=${MOCK_OAUTH:-http://localhost:8082}
BASE="$SKJEMA_API/api/skjema/utsendt-arbeidstaker"

ARBEIDSTAKER_FNR=${ARBEIDSTAKER_FNR:-01816023404}
ARBEIDSGIVER_FNR=${ARBEIDSGIVER_FNR:-30056928150}
ORGNR=${ORGNR:-999999999}
ORGNAVN=${ORGNAVN:-"Ståles Stål AS"}
LAND=${LAND:-BE}

_Y=$((2027 + RANDOM % 5)); _M=$(printf '%02d' $((1 + RANDOM % 9)))
FRA=${FRA:-${_Y}-${_M}-01}
TIL=${TIL:-${_Y}-${_M}-28}
PERIODE="{\"utsendelseLand\":\"$LAND\",\"utsendelsePeriode\":{\"fraDato\":\"$FRA\",\"tilDato\":\"$TIL\"}}"

SLEEP=${SLEEP:-4}
OUTDIR=${OUTDIR:-/tmp}

token() {
  curl -s -X POST "$MOCK_OAUTH/$1/token" \
    -d "grant_type=client_credentials&client_id=test&client_secret=dummy&$2" | jq -r '.access_token'
}

post() {
  local code body
  body=$(curl -s -w '\n%{http_code}' -X POST "$BASE/$2" \
    -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -d "$3")
  code=$(tail -n1 <<<"$body"); body=$(sed '$d' <<<"$body")
  if [[ "$code" != 2* ]]; then echo "  ✗ POST /$2 -> $code: $body" >&2; exit 1; fi
  echo "$body"
}

opprett() {
  local rep=$2 person
  if [[ "$rep" == DEG_SELV ]]; then person="{\"fnr\":\"$ARBEIDSTAKER_FNR\"}";
  else person="{\"fnr\":\"$ARBEIDSTAKER_FNR\",\"etternavn\":\"HANSEN\"}"; fi
  post "$1" "opprett-med-kontekst" \
    "{\"representasjonstype\":\"$rep\",\"radgiverfirma\":null,\"arbeidsgiver\":{\"orgnr\":\"$ORGNR\",\"navn\":\"$ORGNAVN\"},\"arbeidstaker\":$person}" \
    | jq -r '.id'
}

fyll_arbeidstakerdel() {
  post "$1" "$2/utsendingsperiode-og-land" "$PERIODE" >/dev/null
  post "$1" "$2/arbeidssituasjon" '{"harVaertEllerSkalVaereILonnetArbeidFoerUtsending":true,"skalJobbeForFlereVirksomheter":false}' >/dev/null
  post "$1" "$2/skatteforhold-og-inntekt" '{"erSkattepliktigTilNorgeIHeleutsendingsperioden":true,"mottarPengestotteFraAnnetEosLandEllerSveits":false,"inntektFraNorskEllerUtenlandskVirksomhet":["NORSK_VIRKSOMHET"],"hvilkeTyperInntektHarDu":["LOENN"]}' >/dev/null
  post "$1" "$2/familiemedlemmer" '{"skalHaMedFamiliemedlemmer":false}' >/dev/null
  post "$1" "$2/tilleggsopplysninger" '{"harFlereOpplysningerTilSoknaden":false}' >/dev/null
  post "$1" "$2/vedlegg" '{"harAnnenDokumentasjon":false}' >/dev/null
}

fyll_arbeidsgiverdel() {
  post "$1" "$2/utsendingsperiode-og-land" "$PERIODE" >/dev/null
  post "$1" "$2/arbeidsgiverens-virksomhet-i-norge" '{"erArbeidsgiverenOffentligVirksomhet":false,"erArbeidsgiverenBemanningsEllerVikarbyraa":false,"opprettholderArbeidsgiverenVanligDrift":true}' >/dev/null
  post "$1" "$2/utenlandsoppdraget" '{"arbeidsgiverHarOppdragILandet":true,"arbeidstakerBleAnsattForUtenlandsoppdraget":false,"arbeidstakerForblirAnsattIHelePerioden":true,"arbeidstakerErstatterAnnenPerson":false}' >/dev/null
  post "$1" "$2/arbeidstakerens-lonn" '{"arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden":true}' >/dev/null
  post "$1" "$2/arbeidssted-i-utlandet" '{"arbeidsstedType":"PA_SKIP","paSkip":{"navnPaVirksomhet":"navn","navnPaSkip":"navn skip","yrketTilArbeidstaker":"yrke","seilerI":"INTERNASJONALT_FARVANN","flaggland":"BE"}}' >/dev/null
  post "$1" "$2/tilleggsopplysninger" '{"harFlereOpplysningerTilSoknaden":false}' >/dev/null
  post "$1" "$2/vedlegg" '{"harAnnenDokumentasjon":false}' >/dev/null
}

send_inn() {
  post "$1" "$2/send-inn" '' | jq -r '.referanseId'
}

send_arbeidstakerdel() {
  SKJEMA_ID=$(opprett "$1" DEG_SELV)
  fyll_arbeidstakerdel "$1" "$SKJEMA_ID"
  REFERANSE=$(send_inn "$1" "$SKJEMA_ID")
}

send_arbeidsgiverdel() {
  SKJEMA_ID=$(opprett "$1" ARBEIDSGIVER)
  fyll_arbeidsgiverdel "$1" "$SKJEMA_ID"
  REFERANSE=$(send_inn "$1" "$SKJEMA_ID")
}
