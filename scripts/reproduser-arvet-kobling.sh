#!/usr/bin/env bash
# Reproduserer "arvet kobling -> komplett PDF" (MEL-16515 / MEL-8135) mot LOKAL benk.
#
# Sender tre ekte skjemaer via melosys-skjema-api (:8090) med ekte tokens fra
# mock-oauth2-server (:8082). Resten (Kafka -> melosys-api -> sak) skjer av seg selv.
#
# Identiteter (se melosys-docker-compose/TESTBRUKERE.md):
#   Arbeidstaker (DEG_SELV):  HANS HANSEN      01816023404
#   Arbeidsgiver (ARBEIDSGIVER): KARAFFEL TRIVIELL 30056928150 (Altinn-tilgang til Ståles Stål)
#   Organisasjon:             Ståles Stål AS   999999999
set -euo pipefail

SKJEMA_API=${SKJEMA_API:-http://localhost:8090}
MOCK_OAUTH=${MOCK_OAUTH:-http://localhost:8082}
BASE="$SKJEMA_API/api/skjema/utsendt-arbeidstaker"

ARBEIDSTAKER_FNR=01816023404
ARBEIDSGIVER_FNR=30056928150
ORGNR=999999999
ORGNAVN="Ståles Stål AS"
LAND=BE
# Randomiser periode så hver kjøring blir en fersk sak (unngå kollisjon med gamle saker).
_Y=$((2027 + RANDOM % 5)); _M=$(printf '%02d' $((1 + RANDOM % 9)))
FRA=${FRA:-${_Y}-${_M}-01}
TIL=${TIL:-${_Y}-${_M}-28}
# Midlertidig demping for samtidighets-bugen (duplikate saker): vent mellom innsendinger
# så melosys-api rekker å committe sak + mapping før neste melding konsumeres.
SLEEP=${SLEEP:-4}

token() { # token <issuer> <param>   (param: "pid=.." for tokenx, "scope=.." for isso)
  curl -s -X POST "$MOCK_OAUTH/$1/token" \
    -d "grant_type=client_credentials&client_id=test&client_secret=dummy&$2" | jq -r '.access_token'
}

post() { # post <token> <path> <json>   -> echoes body, asserts 2xx
  local code body
  body=$(curl -s -w '\n%{http_code}' -X POST "$BASE/$2" \
    -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -d "$3")
  code=$(tail -n1 <<<"$body"); body=$(sed '$d' <<<"$body")
  if [[ "$code" != 2* ]]; then echo "  ✗ POST /$2 -> $code: $body" >&2; exit 1; fi
  echo "$body"
}

opprett() { # opprett <token> <representasjonstype>  -> echoes skjemaId
  local rep=$2 person
  if [[ "$rep" == DEG_SELV ]]; then person="{\"fnr\":\"$ARBEIDSTAKER_FNR\"}";
  else person="{\"fnr\":\"$ARBEIDSTAKER_FNR\",\"etternavn\":\"HANSEN\"}"; fi
  post "$1" "opprett-med-kontekst" \
    "{\"representasjonstype\":\"$rep\",\"radgiverfirma\":null,\"arbeidsgiver\":{\"orgnr\":\"$ORGNR\",\"navn\":\"$ORGNAVN\"},\"arbeidstaker\":$person}" \
    | jq -r '.id'
}

PERIODE="{\"utsendelseLand\":\"$LAND\",\"utsendelsePeriode\":{\"fraDato\":\"$FRA\",\"tilDato\":\"$TIL\"}}"

fyll_arbeidstakerdel() { # <token> <skjemaId>
  post "$1" "$2/utsendingsperiode-og-land" "$PERIODE" >/dev/null
  post "$1" "$2/arbeidssituasjon" '{"harVaertEllerSkalVaereILonnetArbeidFoerUtsending":true,"skalJobbeForFlereVirksomheter":false}' >/dev/null
  post "$1" "$2/skatteforhold-og-inntekt" '{"erSkattepliktigTilNorgeIHeleutsendingsperioden":true,"mottarPengestotteFraAnnetEosLandEllerSveits":false,"inntektFraNorskEllerUtenlandskVirksomhet":["NORSK_VIRKSOMHET"],"hvilkeTyperInntektHarDu":["LOENN"]}' >/dev/null
  post "$1" "$2/familiemedlemmer" '{"skalHaMedFamiliemedlemmer":false}' >/dev/null
  post "$1" "$2/tilleggsopplysninger" '{"harFlereOpplysningerTilSoknaden":false}' >/dev/null
  post "$1" "$2/vedlegg" '{"harAnnenDokumentasjon":false}' >/dev/null
}

fyll_arbeidsgiverdel() { # <token> <skjemaId>
  post "$1" "$2/utsendingsperiode-og-land" "$PERIODE" >/dev/null
  post "$1" "$2/arbeidsgiverens-virksomhet-i-norge" '{"erArbeidsgiverenOffentligVirksomhet":false,"erArbeidsgiverenBemanningsEllerVikarbyraa":false,"opprettholderArbeidsgiverenVanligDrift":true}' >/dev/null
  post "$1" "$2/utenlandsoppdraget" '{"arbeidsgiverHarOppdragILandet":true,"arbeidstakerBleAnsattForUtenlandsoppdraget":false,"arbeidstakerForblirAnsattIHelePerioden":true,"arbeidstakerErstatterAnnenPerson":false}' >/dev/null
  post "$1" "$2/arbeidstakerens-lonn" '{"arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden":true}' >/dev/null
  post "$1" "$2/arbeidssted-i-utlandet" '{"arbeidsstedType":"PA_SKIP","paSkip":{"navnPaVirksomhet":"navn","navnPaSkip":"navn skip","yrketTilArbeidstaker":"yrke","seilerI":"INTERNASJONALT_FARVANN","flaggland":"BE"}}' >/dev/null
  post "$1" "$2/tilleggsopplysninger" '{"harFlereOpplysningerTilSoknaden":false}' >/dev/null
  post "$1" "$2/vedlegg" '{"harAnnenDokumentasjon":false}' >/dev/null
}

send_inn() { # <token> <skjemaId> -> echoes referanseId
  post "$1" "$2/send-inn" '' | jq -r '.referanseId'
}

echo "▶ Minter tokens…"
TOK_AT=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSTAKER_FNR")
TOK_AG=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSGIVER_FNR")

echo "▶ 1) Arbeidstaker (DEG_SELV) sender ARBEIDSTAKERS_DEL (v1)…"
AT_V1=$(opprett "$TOK_AT" DEG_SELV);            echo "   skjemaId=$AT_V1"
fyll_arbeidstakerdel "$TOK_AT" "$AT_V1"
REF1=$(send_inn "$TOK_AT" "$AT_V1");            echo "   ✓ sendt, referanse=$REF1"
echo "   ⏳ venter ${SLEEP}s (lar melosys-api opprette sak + mapping)…"; sleep "$SLEEP"

echo "▶ 2) Arbeidsgiver (ARBEIDSGIVER) sender ARBEIDSGIVERS_DEL…"
AG=$(opprett "$TOK_AG" ARBEIDSGIVER);           echo "   skjemaId=$AG"
fyll_arbeidsgiverdel "$TOK_AG" "$AG"
REF2=$(send_inn "$TOK_AG" "$AG");               echo "   ✓ sendt, referanse=$REF2 (kobles mot v1)"
echo "   ⏳ venter ${SLEEP}s…"; sleep "$SLEEP"

echo "▶ 3) Arbeidstaker (DEG_SELV) sender NY ARBEIDSTAKERS_DEL (v2)…"
AT_V2=$(opprett "$TOK_AT" DEG_SELV);            echo "   skjemaId=$AT_V2"
fyll_arbeidstakerdel "$TOK_AT" "$AT_V2"
REF3=$(send_inn "$TOK_AT" "$AT_V2");            echo "   ✓ sendt, referanse=$REF3 (arver kobling -> BUG)"

echo
echo "▶ Verifiserer M2M-data + PDF for v2 ($AT_V2)…"
TOK_M2M=$(token isso "scope=melosys-localhost")
DATA=$(curl -s "$SKJEMA_API/m2m/api/skjema/utsendt-arbeidstaker/$AT_V2/data" -H "Authorization: Bearer $TOK_M2M")
echo "   skjemadel       = $(jq -r '.skjema.metadata.skjemadel' <<<"$DATA")"
echo "   kobletSkjema.id  = $(jq -r '.kobletSkjema.id' <<<"$DATA")  (≠ null = bug)"
echo "   koblet skjemadel = $(jq -r '.kobletSkjema.metadata.skjemadel' <<<"$DATA")"
curl -s "$SKJEMA_API/m2m/api/skjema/$AT_V2/pdf" -H "Authorization: Bearer $TOK_M2M" -o /tmp/v2-$AT_V2.pdf
echo "   PDF lagret: /tmp/v2-$AT_V2.pdf ($(wc -c </tmp/v2-$AT_V2.pdf) bytes)"

echo
echo "�fullført. Referanser: v1=$REF1  AG=$REF2  v2=$REF3"
echo "Saken skal nå dukke opp i melosys-api/melosys-web (Kafka -> melosys-api oppretter sak)."
