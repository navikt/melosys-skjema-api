#!/usr/bin/env bash
# Repro: arbeidstaker (v1) -> arbeidsgiver -> ny versjon arbeidstakers del (v2) som arver kobling.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/_repro-lib.sh"

echo "▶ Minter tokens…"
TOK_AT=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSTAKER_FNR")
TOK_AG=$(token tokenx "audience=melosys-skjema-api&pid=$ARBEIDSGIVER_FNR")

echo "▶ 1) Arbeidstaker (DEG_SELV) sender ARBEIDSTAKERS_DEL (v1)…"
send_arbeidstakerdel "$TOK_AT"; AT_V1=$SKJEMA_ID; REF1=$REFERANSE
echo "   skjemaId=$AT_V1, referanse=$REF1"
echo "   ⏳ venter ${SLEEP}s (lar melosys-api opprette sak + mapping)…"; sleep "$SLEEP"

echo "▶ 2) Arbeidsgiver (ARBEIDSGIVER) sender ARBEIDSGIVERS_DEL…"
send_arbeidsgiverdel "$TOK_AG"; AG=$SKJEMA_ID; REF2=$REFERANSE
echo "   skjemaId=$AG, referanse=$REF2 (kobles mot v1)"
echo "   ⏳ venter ${SLEEP}s…"; sleep "$SLEEP"

echo "▶ 3) Arbeidstaker (DEG_SELV) sender NY ARBEIDSTAKERS_DEL (v2)…"
send_arbeidstakerdel "$TOK_AT"; AT_V2=$SKJEMA_ID; REF3=$REFERANSE
echo "   skjemaId=$AT_V2, referanse=$REF3 (arver kobling -> BUG)"

echo
echo "▶ Verifiserer M2M-data + PDF for v2 ($AT_V2)…"
TOK_M2M=$(token isso "scope=melosys-localhost")
DATA=$(curl -s "$SKJEMA_API/m2m/api/skjema/utsendt-arbeidstaker/$AT_V2/data" -H "Authorization: Bearer $TOK_M2M")
echo "   skjemadel        = $(jq -r '.skjema.metadata.skjemadel' <<<"$DATA")"
echo "   kobletSkjema.id   = $(jq -r '.kobletSkjema.id' <<<"$DATA")  (≠ null = bug)"
echo "   koblet skjemadel  = $(jq -r '.kobletSkjema.metadata.skjemadel' <<<"$DATA")"
PDF="$OUTDIR/v2-$AT_V2.pdf"
curl -s "$SKJEMA_API/m2m/api/skjema/$AT_V2/pdf" -H "Authorization: Bearer $TOK_M2M" -o "$PDF"
echo "   PDF lagret: $PDF ($(wc -c <"$PDF") bytes)"
inspiser_pdf "$PDF"

echo
echo "✓ Fullført. Referanser: v1=$REF1  AG=$REF2  v2=$REF3"
echo "Saken skal nå dukke opp i melosys-api/melosys-web (Kafka -> melosys-api oppretter sak)."
