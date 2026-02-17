package no.nav.melosys.skjema.kafka

interface BrukervarselProducer {
    fun sendBrukervarsel(brukervarselMelding: BrukervarselMelding)
}
