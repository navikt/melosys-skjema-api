DROP TABLE IF EXISTS vedlegg;

ALTER TABLE skjema ADD COLUMN data JSONB;

ALTER TABLE skjema DROP COLUMN IF EXISTS arbeidsgiver_data;
ALTER TABLE skjema DROP COLUMN IF EXISTS arbeidstaker_data;

DROP INDEX IF EXISTS idx_vedlegg_skjema_id;