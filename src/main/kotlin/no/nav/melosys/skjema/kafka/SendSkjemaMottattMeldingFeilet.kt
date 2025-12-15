package no.nav.melosys.skjema.kafka

/**
 * Exception som kastes når sending av skjema-mottatt melding til Kafka feiler.
 */
class SendSkjemaMottattMeldingFeilet(
    message: String,
    cause: Throwable
) : RuntimeException(message, cause)
