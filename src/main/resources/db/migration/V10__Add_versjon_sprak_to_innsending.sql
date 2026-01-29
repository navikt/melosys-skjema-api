-- Legg til skjema_definisjon_versjon og innsendt_sprak
ALTER TABLE innsending ADD COLUMN skjema_definisjon_versjon VARCHAR(50) NOT NULL DEFAULT '1';
ALTER TABLE innsending ADD COLUMN innsendt_sprak VARCHAR(10) NOT NULL DEFAULT 'nb';

-- Fjern defaults (kolonner skal settes eksplisitt ved insert)
ALTER TABLE innsending ALTER COLUMN skjema_definisjon_versjon DROP DEFAULT;
ALTER TABLE innsending ALTER COLUMN innsendt_sprak DROP DEFAULT;
