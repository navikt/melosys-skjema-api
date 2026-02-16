package no.nav.melosys.skjema.kafka

import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding

interface SkjemaMottattProducer {
    /**
     * Blokkerende variant av sendSkjemaMottatt.
     * Venter på at meldingen er sendt før den returnerer.
     *
     * @throws no.nav.melosys.skjema.kafka.exception.SendSkjemaMottattMeldingFeilet hvis sending feiler
     */
    fun blokkerendeSendSkjemaMottatt(skjemaMottattMelding: SkjemaMottattMelding): Result<*>
}
