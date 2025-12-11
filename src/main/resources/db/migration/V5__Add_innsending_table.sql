-- Egen tabell for innsendingsprosessering (retry, feilhåndtering)

-- Legg til journalpost_id på skjema
ALTER TABLE skjema ADD COLUMN journalpost_id VARCHAR(255);

-- Opprett innsending-tabell for prosesseringsstatus
CREATE TABLE innsending (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skjema_id UUID NOT NULL UNIQUE REFERENCES skjema(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    feilmelding TEXT,
    antall_forsok INTEGER NOT NULL DEFAULT 0,
    siste_forsoek TIMESTAMP WITH TIME ZONE,
    opprettet_dato TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indeks for scheduler-query (findRetryKandidater)
CREATE INDEX idx_innsending_status ON innsending(status);
CREATE INDEX idx_innsending_status_forsok ON innsending(status, antall_forsok);

-- Migrer eksisterende MOTTATT-status til SENDT
UPDATE skjema SET status = 'SENDT' WHERE status = 'MOTTATT';

-- Oppdater constraint til kun å tillate UTKAST og SENDT
ALTER TABLE skjema DROP CONSTRAINT IF EXISTS skjema_status_check;
ALTER TABLE skjema ADD CONSTRAINT skjema_status_check CHECK (status IN ('UTKAST', 'SENDT'));
