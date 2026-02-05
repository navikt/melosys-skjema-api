-- Journalføring skjer i melosys-api, ikke her. Fjerner ubrukt kolonne.
ALTER TABLE skjema DROP COLUMN IF EXISTS journalpost_id;
