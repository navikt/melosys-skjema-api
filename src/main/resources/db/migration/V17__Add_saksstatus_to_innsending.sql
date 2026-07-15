ALTER TABLE innsending ADD COLUMN saksstatus VARCHAR(20);
ALTER TABLE innsending ADD COLUMN saksstatus_oppdatert TIMESTAMP WITH TIME ZONE;

-- Saksstatus-synk slår opp alle innsendinger på samme sak
CREATE INDEX idx_innsending_saksnummer ON innsending (saksnummer);
