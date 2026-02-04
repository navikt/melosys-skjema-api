CREATE TABLE skjema (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(50) NOT NULL CHECK (status IN ('UTKAST', 'SENDT', 'MOTTATT')),
    type VARCHAR(50) NOT NULL,
    fnr VARCHAR(11) NOT NULL,
    orgnr VARCHAR(9) NOT NULL,
    arbeidsgiver_data JSONB,
    arbeidstaker_data JSONB,
    opprettet_dato TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_dato TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    opprettet_av VARCHAR(11) NOT NULL,
    endret_av VARCHAR(11) NOT NULL
);

CREATE TABLE fullmakt (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skjema_id UUID NOT NULL REFERENCES skjema(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL CHECK (status IN ('VENTER', 'GODKJENT', 'AVSLATT')),
    opprettet_dato TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    besluttet_dato TIMESTAMP WITH TIME ZONE,
    besluttet_av VARCHAR(11)
);

CREATE TABLE vedlegg (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skjema_id UUID NOT NULL REFERENCES skjema(id) ON DELETE CASCADE,
    filnavn VARCHAR(255) NOT NULL,
    storage_url VARCHAR(500) NOT NULL,
    filtype VARCHAR(50) NOT NULL,
    opprettet_dato TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skjema_fnr ON skjema(fnr);
CREATE INDEX idx_skjema_orgnr ON skjema(orgnr);
CREATE INDEX idx_skjema_status ON skjema(status);
CREATE INDEX idx_skjema_opprettet_dato ON skjema(opprettet_dato);
CREATE INDEX idx_fullmakt_skjema_id ON fullmakt(skjema_id);
CREATE INDEX idx_fullmakt_status ON fullmakt(status);
CREATE INDEX idx_vedlegg_skjema_id ON vedlegg(skjema_id);

CREATE OR REPLACE FUNCTION update_endret_dato()
RETURNS TRIGGER AS $$
BEGIN
    NEW.endret_dato = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_skjema_endret_dato
    BEFORE UPDATE ON skjema
    FOR EACH ROW
    EXECUTE FUNCTION update_endret_dato();