-- Legg til kolonner for innsendingsstatus (flyttes fra metadata JSONB til egne felt)

ALTER TABLE skjema ADD COLUMN innsending_status VARCHAR(50);
ALTER TABLE skjema ADD COLUMN journalpost_id VARCHAR(255);
ALTER TABLE skjema ADD COLUMN referanse_id VARCHAR(50);
ALTER TABLE skjema ADD COLUMN innsending_feilmelding TEXT;
ALTER TABLE skjema ADD COLUMN innsending_antall_forsok INTEGER DEFAULT 0;
ALTER TABLE skjema ADD COLUMN innsending_siste_forsoek TIMESTAMP WITH TIME ZONE;

-- Indeks for scheduler-query (findRetryKandidater)
CREATE INDEX idx_skjema_innsending_status ON skjema(innsending_status);

-- Migrer eksisterende MOTTATT til SENDT før vi endrer constraint
-- MOTTATT var tidligere en SkjemaStatus, men vi bruker nå kun UTKAST og SENDT
-- MOTTATT-status flyttes til innsending_status = 'FERDIG' (antar allerede prosessert)
UPDATE skjema SET status = 'SENDT', innsending_status = 'FERDIG' WHERE status = 'MOTTATT';

-- Oppdater constraint til kun å tillate UTKAST og SENDT
ALTER TABLE skjema DROP CONSTRAINT IF EXISTS skjema_status_check;
ALTER TABLE skjema ADD CONSTRAINT skjema_status_check CHECK (status IN ('UTKAST', 'SENDT'));
