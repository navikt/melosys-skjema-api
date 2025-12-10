package no.nav.melosys.skjema.domain

/**
 * Status for asynkron bakgrunnsprosessering av innsendte søknader.
 *
 * Lagres i skjema.metadata.innsending og sporer fremdrift gjennom:
 * 1. Journalføring til Joark
 * 2. Kafka-sending til melosys-api
 * 3. Varsling til arbeidstaker (best effort)
 *
 * Merk: Dette er intern prosesseringsstatus, ikke synlig for bruker.
 * Bruker ser kun [no.nav.melosys.skjema.entity.SkjemaStatus] (UTKAST/SENDT).
 */
enum class InnsendingStatus {

    /** Innsending registrert, asynkron prosessering ikke startet ennå */
    MOTTATT,

    /** Journalført OK i Joark, venter på Kafka-sending */
    JOURNALFORT,

    /** Alt OK - journalført og sendt til melosys-api */
    FERDIG,

    /** Journalføring feilet - vil bli forsøkt på nytt av scheduler */
    JOURNALFORING_FEILET,

    /** Kafka-sending feilet - vil bli forsøkt på nytt av scheduler */
    KAFKA_FEILET
}
