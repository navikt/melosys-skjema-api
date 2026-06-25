#!/usr/bin/env bash
# Delt hjelpebibliotek for repro-skriptene. Source-es inn av kallende skript:
#   source "$(dirname "${BASH_SOURCE[0]}")/_repro-lib.sh"
#
# Gir env-defaults, testidentiteter, lavnivå-helpere (token/post/opprett/fyll/send_inn) og
# høynivå-helpere (send_arbeidstakerdel/send_arbeidsgiverdel) slik at det er trivielt å sende
# inn flere skjema fra et kallende skript.
#
# Identiteter (se melosys-docker-compose/TESTBRUKERE.md):
#   Arbeidstaker (DEG_SELV):     HANS HANSEN       01816023404
#   Arbeidsgiver (ARBEIDSGIVER): KARAFFEL TRIVIELL 30056928150 (Altinn-tilgang til Ståles Stål)
#   Organisasjon:                Ståles Stål AS    999999999

SKJEMA_API=${SKJEMA_API:-http://localhost:8090}
MOCK_OAUTH=${MOCK_OAUTH:-http://localhost:8082}
BASE="$SKJEMA_API/api/skjema/utsendt-arbeidstaker"

ARBEIDSTAKER_FNR=${ARBEIDSTAKER_FNR:-01816023404}
ARBEIDSGIVER_FNR=${ARBEIDSGIVER_FNR:-30056928150}
ORGNR=${ORGNR:-999999999}
ORGNAVN=${ORGNAVN:-"Ståles Stål AS"}
LAND=${LAND:-BE}

# Randomiser periode så hver kjøring blir en fersk sak (unngå kollisjon med gamle saker).
_Y=$((2027 + RANDOM % 5)); _M=$(printf '%02d' $((1 + RANDOM % 9)))
FRA=${FRA:-${_Y}-${_M}-01}
TIL=${TIL:-${_Y}-${_M}-28}
PERIODE="{\"utsendelseLand\":\"$LAND\",\"utsendelsePeriode\":{\"fraDato\":\"$FRA\",\"tilDato\":\"$TIL\"}}"

# Pause mellom innsendinger så melosys-api rekker å committe sak + mapping før neste melding
# konsumeres (demper duplikatsak-bug ved samtidig prosessering).
SLEEP=${SLEEP:-4}

# Hvor PDF-er lagres. Overstyr med OUTDIR=. for å skrive til arbeidsmappa.
OUTDIR=${OUTDIR:-/tmp}

token() { # token <issuer> <param>   (param: "pid=.." for tokenx, "scope=.." for isso)
  curl -s -X POST "$MOCK_OAUTH/$1/token" \
    -d "grant_type=client_credentials&client_id=test&client_secret=dummy&$2" | jq -r '.access_token'
}

post() { # post <token> <path> <json>   -> echoer body, feiler ved ikke-2xx
  local code body
  body=$(curl -s -w '\n%{http_code}' -X POST "$BASE/$2" \
    -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -d "$3")
  code=$(tail -n1 <<<"$body"); body=$(sed '$d' <<<"$body")
  if [[ "$code" != 2* ]]; then echo "  ✗ POST /$2 -> $code: $body" >&2; exit 1; fi
  echo "$body"
}

opprett() { # opprett <token> <representasjonstype>  -> echoer skjemaId
  local rep=$2 person
  if [[ "$rep" == DEG_SELV ]]; then person="{\"fnr\":\"$ARBEIDSTAKER_FNR\"}";
  else person="{\"fnr\":\"$ARBEIDSTAKER_FNR\",\"etternavn\":\"HANSEN\"}"; fi
  post "$1" "opprett-med-kontekst" \
    "{\"representasjonstype\":\"$rep\",\"radgiverfirma\":null,\"arbeidsgiver\":{\"orgnr\":\"$ORGNR\",\"navn\":\"$ORGNAVN\"},\"arbeidstaker\":$person}" \
    | jq -r '.id'
}

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

send_inn() { # <token> <skjemaId> -> echoer referanseId
  post "$1" "$2/send-inn" '' | jq -r '.referanseId'
}

# Høynivå: opprett + fyll + send en komplett del i ett kall. Setter SKJEMA_ID og REFERANSE
# slik at flere innsendinger blir en enkel sekvens (eller løkke) i kallende skript.
send_arbeidstakerdel() { # <token>
  SKJEMA_ID=$(opprett "$1" DEG_SELV)
  fyll_arbeidstakerdel "$1" "$SKJEMA_ID"
  REFERANSE=$(send_inn "$1" "$SKJEMA_ID")
}

send_arbeidsgiverdel() { # <token>
  SKJEMA_ID=$(opprett "$1" ARBEIDSGIVER)
  fyll_arbeidsgiverdel "$1" "$SKJEMA_ID"
  REFERANSE=$(send_inn "$1" "$SKJEMA_ID")
}
