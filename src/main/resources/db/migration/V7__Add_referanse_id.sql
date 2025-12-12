-- Legg til referanse_id kolonne i innsending-tabellen (non-null for innsendte skjemaer)
ALTER TABLE innsending ADD COLUMN referanse_id VARCHAR(20) NOT NULL;

-- Unique index for rask oppslag og unikhet
CREATE UNIQUE INDEX idx_innsending_referanse_id ON innsending(referanse_id);
