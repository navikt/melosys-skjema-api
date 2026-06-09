-- Engangs-opprydding: slett soft-deletede skjemaer (gammel SLETTET-status).
DELETE FROM skjema WHERE status = 'SLETTET';

-- Stram status-constraint til kun gyldige verdier etter at SLETTET-rader er borte.
ALTER TABLE skjema DROP CONSTRAINT IF EXISTS skjema_status_check;
ALTER TABLE skjema ADD CONSTRAINT skjema_status_check CHECK (status IN ('UTKAST', 'SENDT'));

