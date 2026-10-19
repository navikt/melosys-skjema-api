-- Erstatter correlation_id med W3C traceparent fra NAIS autoinstrumentation.
-- correlation_id beholdes inntil videre, fordi gamle podder skriver til den under rolling deploy.
ALTER TABLE innsending ADD COLUMN traceparent VARCHAR(55);
