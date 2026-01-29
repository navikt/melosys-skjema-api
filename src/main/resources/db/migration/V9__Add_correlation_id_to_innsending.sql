-- Legg til correlation_id for sporing av innsendinger på tvers av systemer
ALTER TABLE innsending ADD COLUMN correlation_id VARCHAR(255);
