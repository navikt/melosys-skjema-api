-- Legg til referanse_id kolonne for brukervennlig referanse til søknaden
ALTER TABLE skjema ADD COLUMN referanse_id VARCHAR(20);

-- Unique index for rask oppslag og unikhet
CREATE UNIQUE INDEX idx_skjema_referanse_id ON skjema(referanse_id) WHERE referanse_id IS NOT NULL;
