-- Remove the constraint that was added when columns were nullable
ALTER TABLE skjema DROP CONSTRAINT IF EXISTS check_fnr_or_orgnr_not_null;

-- Make fnr, orgnr and metadata columns NOT NULL
ALTER TABLE skjema ALTER COLUMN fnr SET NOT NULL;
ALTER TABLE skjema ALTER COLUMN orgnr SET NOT NULL;
ALTER TABLE skjema ALTER COLUMN metadata SET NOT NULL;
