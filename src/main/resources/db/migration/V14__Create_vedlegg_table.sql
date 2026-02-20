CREATE TABLE vedlegg (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skjema_id UUID NOT NULL REFERENCES skjema(id) ON DELETE CASCADE,
    filnavn VARCHAR(255) NOT NULL,
    original_filnavn VARCHAR(255) NOT NULL,
    filtype VARCHAR(20) NOT NULL CHECK (filtype IN ('PDF', 'JPEG', 'PNG')),
    filstorrelse BIGINT NOT NULL,
    storage_referanse VARCHAR(500) NOT NULL,
    opprettet_dato TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    opprettet_av VARCHAR(11) NOT NULL
);

CREATE INDEX idx_vedlegg_skjema_id ON vedlegg(skjema_id);
