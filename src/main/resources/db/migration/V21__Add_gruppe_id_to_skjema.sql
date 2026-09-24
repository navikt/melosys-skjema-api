-- Stabil gruppe-ID for relaterte deler av samme søknad (MELOSYS-8151).
--
-- Alle deler av samme søknad (samme fnr + juridisk enhet + overlappende utsendelsesperiode)
-- deler én gruppe_id. Den tildeles én gang ved første innsending i gruppen og gjenbrukes av
-- de øvrige delene, slik at verdien er stabil uavhengig av hvilken rekkefølge delene sendes i.
--
-- Verdien sendes videre på SkjemaMottattMelding og brukes av melosys-api til å serialisere
-- behandlingen per gruppe. En utregning per melding ville ikke vært stabil: gruppen bygges av
-- skjemaer med status SENDT, og et tidligere opprettet utkast som sendes inn senere ville
-- flyttet «tidligst opprettede» og dermed endret gruppe-ID-en underveis.
ALTER TABLE skjema ADD COLUMN gruppe_id UUID;

-- Oppslag på gruppe_id ved tildeling (finn eksisterende ID blant relaterte skjemaer).
CREATE INDEX idx_skjema_gruppe_id ON skjema (gruppe_id);

-- Backfill: eksisterende SENDT-skjemaer får seg selv som gruppe. De er ferdig prosessert av
-- melosys-api, så de skal ikke serialiseres mot noe. Nye innsendinger tildeles gruppe i koden.
UPDATE skjema SET gruppe_id = id WHERE gruppe_id IS NULL AND status = 'SENDT';
