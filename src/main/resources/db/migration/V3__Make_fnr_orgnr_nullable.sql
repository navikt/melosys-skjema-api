-- Make fnr and orgnr columns nullable
ALTER TABLE skjema ALTER COLUMN fnr DROP NOT NULL;
ALTER TABLE skjema ALTER COLUMN orgnr DROP NOT NULL;

-- Add constraint to ensure at least one of fnr or orgnr is set
ALTER TABLE skjema ADD CONSTRAINT check_fnr_or_orgnr_not_null 
    CHECK (fnr IS NOT NULL OR orgnr IS NOT NULL);