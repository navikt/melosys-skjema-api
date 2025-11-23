-- Legg til JSONB-kolonne for metadata som er spesifikk for skjematypen
-- For "Utsendt arbeidstaker" vil dette inneholde representasjonstype, rådgiverfirma, fullmakt osv.

ALTER TABLE skjema
    ADD COLUMN metadata JSONB;

-- GIN-indeks for å kunne søke effektivt i JSONB
CREATE INDEX idx_skjema_metadata_gin ON skjema USING GIN (metadata);

-- Spesifikke indekser for vanlige søk i metadata
CREATE INDEX idx_skjema_metadata_representasjonstype ON skjema ((metadata->>'representasjonstype'));
CREATE INDEX idx_skjema_metadata_fullmektig_fnr ON skjema ((metadata->>'fullmektigFnr')) WHERE metadata->>'fullmektigFnr' IS NOT NULL;

-- Kommentar for dokumentasjon
COMMENT ON COLUMN skjema.metadata IS 'Skjematype-spesifikk metadata. For Utsendt arbeidstaker: representasjonstype, rådgiverfirma, fullmakt, osv.';
