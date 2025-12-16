package no.nav.melosys.skjema.kafka.exception

class SendBrukervarselFeilet(
    message: String,
    cause: Throwable
) : RuntimeException(message, cause)
