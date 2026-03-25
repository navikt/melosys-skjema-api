-- Utvid status-constraint til å inkludere SLETTET (soft delete av utkast)
ALTER TABLE skjema DROP CONSTRAINT IF EXISTS skjema_status_check;
ALTER TABLE skjema ADD CONSTRAINT skjema_status_check CHECK (status IN ('UTKAST', 'SENDT', 'SLETTET'));
