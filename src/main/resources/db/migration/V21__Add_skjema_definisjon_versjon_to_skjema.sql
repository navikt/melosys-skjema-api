ALTER TABLE skjema
    ADD COLUMN skjema_definisjon_versjon VARCHAR(50);

UPDATE skjema s
SET skjema_definisjon_versjon = i.skjema_definisjon_versjon
FROM innsending i
WHERE i.skjema_id = s.id;

UPDATE skjema
SET skjema_definisjon_versjon = '1'
WHERE skjema_definisjon_versjon IS NULL;

ALTER TABLE skjema
    -- DEFAULT dekker inserts fra gamle podder under rolling deploy.
    ALTER COLUMN skjema_definisjon_versjon SET DEFAULT '1',
    ALTER COLUMN skjema_definisjon_versjon SET NOT NULL;
