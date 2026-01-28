-- Add skjema_definisjon_versjon and innsendt_sprak columns to innsending table
ALTER TABLE innsending ADD COLUMN skjema_definisjon_versjon VARCHAR(50) NOT NULL DEFAULT '1';
ALTER TABLE innsending ADD COLUMN innsendt_sprak VARCHAR(10) NOT NULL DEFAULT 'nb';

-- Remove defaults after adding (columns should be set explicitly on insert)
ALTER TABLE innsending ALTER COLUMN skjema_definisjon_versjon DROP DEFAULT;
ALTER TABLE innsending ALTER COLUMN innsendt_sprak DROP DEFAULT;
