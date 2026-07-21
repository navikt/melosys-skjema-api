ALTER TABLE innsending ADD COLUMN saksstatus VARCHAR(20);
ALTER TABLE innsending ADD COLUMN saksstatus_oppdatert TIMESTAMP WITH TIME ZONE;

-- Oppslag på saksnummer (visning/admin - synken selv oppdaterer per skjema-id)
CREATE INDEX idx_innsending_saksnummer ON innsending (saksnummer);
