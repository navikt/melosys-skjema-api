package no.nav.melosys.skjema.types.utsendtarbeidstaker

import no.nav.melosys.skjema.types.common.Språk

object UtsendtArbeidstakerDokumentTittel {

    fun utled(skjemaData: UtsendtArbeidstakerSkjemaData, språk: Språk): String =
        when (skjemaData) {
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> when (språk) {
                Språk.NORSK_BOKMAL -> "Bekreftelse fra arbeidsgiver på utsending til annet EØS-land eller Sveits"
                Språk.NYNORSK -> "Stadfesting frå arbeidsgivar på utsending til anna EØS-land eller Sveits"
                Språk.ENGELSK -> "Employer's confirmation of posting to another EEA country or Switzerland"
            }
            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto,
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> when (språk) {
                Språk.NORSK_BOKMAL -> "Søknad om A1 for utsendte arbeidstakere i EØS eller Sveits"
                Språk.NYNORSK -> "Søknad om A1 for utsende arbeidstakarar i EØS eller Sveits"
                Språk.ENGELSK -> "Application for an A1 Certificate for Posted Workers in the EEA or Switzerland"
            }
        }
}
