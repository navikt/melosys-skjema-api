-- Arbeidsgiver-delen utkastet ble forhåndsutfylt fra (motpart-CTA), for å kunne vise
-- motpartens oppgitte verdier under utfylling selv etter at bruker har endret dem.
ALTER TABLE skjema
    ADD COLUMN prefylt_fra_skjema_id UUID REFERENCES skjema (id) ON DELETE SET NULL;
