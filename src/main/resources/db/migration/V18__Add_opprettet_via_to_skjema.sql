-- Hvordan skjemaet ble startet (null = ordinær flyt). Kun for aggregert bruksstatistikk.
ALTER TABLE skjema
    ADD COLUMN opprettet_via VARCHAR(50);
