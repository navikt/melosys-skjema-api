#!/usr/bin/env bash
# Repro: arbeidsgiver sender sin del først, så arbeidstaker → motpart-kobling.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/_repro-lib.sh"

echo "▶ Periode: $FRA – $TIL"
TOK_AT=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSTAKER_FNR")
TOK_AG=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSGIVER_FNR")

echo "▶ 1) Arbeidsgiver (ARBEIDSGIVER) sender ARBEIDSGIVERS_DEL FØRST…"
send_arbeidsgiverdel "$TOK_AG"; AG=$SKJEMA_ID; REF_AG=$REFERANSE
echo "   skjemaId=$AG, referanse=$REF_AG (ingen motpart ennå)"
echo "   ⏳ venter ${SLEEP}s…"; sleep "$SLEEP"

echo "▶ 2) Arbeidstaker (DEG_SELV) sender ARBEIDSTAKERS_DEL etterpå…"
send_arbeidstakerdel "$TOK_AT"; AT=$SKJEMA_ID; REF_AT=$REFERANSE
echo "   skjemaId=$AT, referanse=$REF_AT (motpart-kobles til arbeidsgivers del)"

echo
echo "▶ Sjekker arbeidstakers del ($AT)…"
TOK_M2M=$(token isso "scope=melosys-localhost")
DATA=$(curl -s "$SKJEMA_API/m2m/api/skjema/utsendt-arbeidstaker/$AT/data" -H "Authorization: Bearer $TOK_M2M")
echo "   skjemadel        = $(jq -r '.skjema.metadata.skjemadel' <<<"$DATA")"
echo "   kobletSkjema.id   = $(jq -r '.kobletSkjema.id' <<<"$DATA")"
PDF="$OUTDIR/at-$AT.pdf"
curl -s "$SKJEMA_API/m2m/api/skjema/$AT/pdf" -H "Authorization: Bearer $TOK_M2M" -o "$PDF"
echo "   PDF: $PDF ($(wc -c <"$PDF") bytes)"
inspiser_pdf "$PDF"
echo
echo "Referanser: AG=$REF_AG  AT=$REF_AT  | periode $FRA–$TIL"
