#!/usr/bin/env bash
# Reproduserer journalførings-bugen i det ENKLE tilfellet (ingen ny versjon / arvet kobling):
# Arbeidsgiver sender sin del FØRST, så sender arbeidstaker sin del. Arbeidstakers del blir
# motpart-koblet til arbeidsgivers del, og arbeidstakers PDF får da BEGGE deler.
#
# Periode randomiseres så hver kjøring blir en fersk sak. Kjør mot LOKAL benk.
set -euo pipefail

SKJEMA_API=${SKJEMA_API:-http://localhost:8090}
MOCK_OAUTH=${MOCK_OAUTH:-http://localhost:8082}
BASE="$SKJEMA_API/api/skjema/utsendt-arbeidstaker"

ARBEIDSTAKER_FNR=01816023404      # HANS HANSEN (DEG_SELV)
ARBEIDSGIVER_FNR=30056928150      # KARAFFEL TRIVIELL (ARBEIDSGIVER, Altinn-tilgang til Ståles Stål)
ORGNR=999999999; ORGNAVN="Ståles Stål AS"; LAND=BE
_Y=$((2027 + RANDOM % 5)); _M=$(printf '%02d' $((1 + RANDOM % 9)))
FRA=${FRA:-${_Y}-${_M}-01}; TIL=${TIL:-${_Y}-${_M}-28}
SLEEP=${SLEEP:-4}

token() { curl -s -X POST "$MOCK_OAUTH/$1/token" -d "grant_type=client_credentials&client_id=test&client_secret=dummy&$2" | jq -r '.access_token'; }
post() {
  local body code; body=$(curl -s -w '\n%{http_code}' -X POST "$BASE/$2" -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -d "$3")
  code=$(tail -n1 <<<"$body"); body=$(sed '$d' <<<"$body")
  [[ "$code" == 2* ]] || { echo "  ✗ POST /$2 -> $code: $body" >&2; exit 1; }; echo "$body"
}
opprett() {
  local rep=$2 person; if [[ "$rep" == DEG_SELV ]]; then person="{\"fnr\":\"$ARBEIDSTAKER_FNR\"}"; else person="{\"fnr\":\"$ARBEIDSTAKER_FNR\",\"etternavn\":\"HANSEN\"}"; fi
  post "$1" "opprett-med-kontekst" "{\"representasjonstype\":\"$rep\",\"radgiverfirma\":null,\"arbeidsgiver\":{\"orgnr\":\"$ORGNR\",\"navn\":\"$ORGNAVN\"},\"arbeidstaker\":$person}" | jq -r '.id'
}
PERIODE="{\"utsendelseLand\":\"$LAND\",\"utsendelsePeriode\":{\"fraDato\":\"$FRA\",\"tilDato\":\"$TIL\"}}"
fyll_at() {
  post "$1" "$2/utsendingsperiode-og-land" "$PERIODE" >/dev/null
  post "$1" "$2/arbeidssituasjon" '{"harVaertEllerSkalVaereILonnetArbeidFoerUtsending":true,"skalJobbeForFlereVirksomheter":false}' >/dev/null
  post "$1" "$2/skatteforhold-og-inntekt" '{"erSkattepliktigTilNorgeIHeleutsendingsperioden":true,"mottarPengestotteFraAnnetEosLandEllerSveits":false,"inntektFraNorskEllerUtenlandskVirksomhet":["NORSK_VIRKSOMHET"],"hvilkeTyperInntektHarDu":["LOENN"]}' >/dev/null
  post "$1" "$2/familiemedlemmer" '{"skalHaMedFamiliemedlemmer":false}' >/dev/null
  post "$1" "$2/tilleggsopplysninger" '{"harFlereOpplysningerTilSoknaden":false}' >/dev/null
  post "$1" "$2/vedlegg" '{"harAnnenDokumentasjon":false}' >/dev/null
}
fyll_ag() {
  post "$1" "$2/utsendingsperiode-og-land" "$PERIODE" >/dev/null
  post "$1" "$2/arbeidsgiverens-virksomhet-i-norge" '{"erArbeidsgiverenOffentligVirksomhet":false,"erArbeidsgiverenBemanningsEllerVikarbyraa":false,"opprettholderArbeidsgiverenVanligDrift":true}' >/dev/null
  post "$1" "$2/utenlandsoppdraget" '{"arbeidsgiverHarOppdragILandet":true,"arbeidstakerBleAnsattForUtenlandsoppdraget":false,"arbeidstakerForblirAnsattIHelePerioden":true,"arbeidstakerErstatterAnnenPerson":false}' >/dev/null
  post "$1" "$2/arbeidstakerens-lonn" '{"arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden":true}' >/dev/null
  post "$1" "$2/arbeidssted-i-utlandet" '{"arbeidsstedType":"PA_SKIP","paSkip":{"navnPaVirksomhet":"navn","navnPaSkip":"navn skip","yrketTilArbeidstaker":"yrke","seilerI":"INTERNASJONALT_FARVANN","flaggland":"BE"}}' >/dev/null
  post "$1" "$2/tilleggsopplysninger" '{"harFlereOpplysningerTilSoknaden":false}' >/dev/null
  post "$1" "$2/vedlegg" '{"harAnnenDokumentasjon":false}' >/dev/null
}
send_inn() { post "$1" "$2/send-inn" '' | jq -r '.referanseId'; }

echo "▶ Periode: $FRA – $TIL"
TOK_AT=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSTAKER_FNR")
TOK_AG=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSGIVER_FNR")

echo "▶ 1) Arbeidsgiver (ARBEIDSGIVER) sender ARBEIDSGIVERS_DEL FØRST…"
AG=$(opprett "$TOK_AG" ARBEIDSGIVER); echo "   skjemaId=$AG"
fyll_ag "$TOK_AG" "$AG"; REF_AG=$(send_inn "$TOK_AG" "$AG"); echo "   ✓ sendt, referanse=$REF_AG (ingen motpart ennå)"
echo "   ⏳ venter ${SLEEP}s…"; sleep "$SLEEP"

echo "▶ 2) Arbeidstaker (DEG_SELV) sender ARBEIDSTAKERS_DEL etterpå…"
AT=$(opprett "$TOK_AT" DEG_SELV); echo "   skjemaId=$AT"
fyll_at "$TOK_AT" "$AT"; REF_AT=$(send_inn "$TOK_AT" "$AT"); echo "   ✓ sendt, referanse=$REF_AT (motpart-kobles til arbeidsgivers del)"

echo
echo "▶ Sjekker arbeidstakers del ($AT)…"
TOK_M2M=$(token isso "scope=melosys-localhost")
DATA=$(curl -s "$SKJEMA_API/m2m/api/skjema/utsendt-arbeidstaker/$AT/data" -H "Authorization: Bearer $TOK_M2M")
echo "   skjemadel        = $(jq -r '.skjema.metadata.skjemadel' <<<"$DATA")"
echo "   kobletSkjema.id   = $(jq -r '.kobletSkjema.id' <<<"$DATA")"
curl -s "$SKJEMA_API/m2m/api/skjema/$AT/pdf" -H "Authorization: Bearer $TOK_M2M" -o "/tmp/at-$AT.pdf"
echo "   PDF: /tmp/at-$AT.pdf ($(wc -c </tmp/at-$AT.pdf) bytes)"
echo
echo "Referanser: AG=$REF_AG  AT=$REF_AT  | periode $FRA–$TIL"
