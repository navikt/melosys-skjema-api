package no.nav.melosys.skjema.types.utsendtarbeidstaker

import no.nav.melosys.skjema.types.common.Språk

object UtsendtArbeidstakerDokumentTittel {

    fun utled(skjemaData: UtsendtArbeidstakerSkjemaData, språk: Språk): String =
        when (skjemaData) {
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> when (språk) {
                Språk.NORSK_BOKMAL -> "Bekreftelse fra arbeidsgiver på utsending til annet EØS-land eller Sveits"
                Språk.ENGELSK -> "Employer's confirmation of posting to another EU/EEA country or Switzerland"
            }
            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto,
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> when (språk) {
                Språk.NORSK_BOKMAL -> "Søknad om A1 for utsendte arbeidstakere i EØS eller Sveits"
                Språk.ENGELSK -> "Application for A1 for posted workers in the EEA or Switzerland"
            }
        }
}
