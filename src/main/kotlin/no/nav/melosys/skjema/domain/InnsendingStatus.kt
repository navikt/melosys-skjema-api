package no.nav.melosys.skjema.domain

/**
 * Status for asynkron bakgrunnsprosessering av innsendte søknader.
 *
 * Sporer fremdrift for Kafka-sending til melosys-api.
 *
 * Merk: Dette er intern prosesseringsstatus, ikke synlig for bruker.
 * Bruker ser kun [no.nav.melosys.skjema.types.common.SkjemaStatus] (UTKAST/SENDT).
 */
enum class InnsendingStatus {

    /** Innsending registrert, asynkron prosessering ikke startet ennå */
    MOTTATT,

    /** Prosessering pågår - brukes for å forhindre at scheduler plukker opp samme innsending flere ganger */
    UNDER_BEHANDLING,

    /** Alt OK - sendt til melosys-api via Kafka */
    FERDIG,

    /** Kafka-sending feilet - vil bli forsøkt på nytt av scheduler */
    KAFKA_FEILET
}
