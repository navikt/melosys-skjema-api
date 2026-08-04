-- Fjern SLETTET fra skjema-status etter at admin-oppryddingen er kjørt (MELOSYS-8157).
-- Det finnes ikke lenger soft-slettede utkast, så constrainten begrenses til aktive statuser.
ALTER TABLE skjema DROP CONSTRAINT IF EXISTS skjema_status_check;
ALTER TABLE skjema ADD CONSTRAINT skjema_status_check CHECK (status IN ('UTKAST', 'SENDT'));
