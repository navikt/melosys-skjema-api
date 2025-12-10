package no.nav.melosys.skjema.domain

/**
 * Status for asynkron prosessering av innsendte søknader.
 *
 * Brukes til å spore fremdrift gjennom journalføring og Kafka-sending.
 */
enum class InnsendingStatus {
    /** Søknad mottatt, venter på prosessering */
    MOTTATT,

    /** Journalført OK i Joark, venter på Kafka-sending */
    JOURNALFORT,

    /** Alt OK - journalført og sendt til melosys-api */
    FERDIG,

    /** Journalføring feilet */
    JOURNALFORING_FEILET,

    /** Kafka-sending feilet */
    KAFKA_FEILET
}
