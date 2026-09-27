-- Når brukeren bekreftet at hen vil svare så riktig som mulig, før utkastet ble opprettet.
-- Null for skjemaer opprettet før tidspunktet ble lagret.
ALTER TABLE skjema
    ADD COLUMN bekreftet_riktige_opplysninger_tidspunkt TIMESTAMP WITH TIME ZONE;
