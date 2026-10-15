package no.nav.melosys.skjema.integrasjon.aareg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.aareg.dto.Arbeidsforhold
import no.nav.melosys.skjema.types.felles.PeriodeDto
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class AaregService(
    private val aaregClient: AaregClient
) {

    /**
     * Om arbeidstakeren har et ordinært arbeidsforhold hos arbeidsgiveren som overlapper [periode].
     * Arbeidsforholdet matcher når arbeidsgiveren er [orgnr] (oppgitt i søknaden) eller når
     * opplysningspliktig er [juridiskEnhetOrgnr]. Et arbeidsforhold uten sluttdato er aktivt.
     */
    fun harOrdinaertArbeidsforhold(fnr: String, orgnr: String, juridiskEnhetOrgnr: String, periode: PeriodeDto): Boolean {
        val arbeidsforhold = aaregClient.hentOrdinaereArbeidsforhold(fnr)
        val harTreff = arbeidsforhold.any {
            it.type?.kode == ARBEIDSFORHOLDTYPE_ORDINAERT && it.gjelder(orgnr, juridiskEnhetOrgnr) && it.overlapper(periode)
        }
        log.info { "AAREG: ${arbeidsforhold.size} ordinære arbeidsforhold, treff hos arbeidsgiver i perioden: $harTreff" }
        return harTreff
    }

    private fun Arbeidsforhold.gjelder(orgnr: String, juridiskEnhetOrgnr: String): Boolean =
        arbeidssted?.organisasjonsnummer() == orgnr || opplysningspliktig?.organisasjonsnummer() == juridiskEnhetOrgnr

    private fun Arbeidsforhold.overlapper(periode: PeriodeDto): Boolean {
        val ansatt = ansettelsesperiode ?: return false
        return (ansatt.startdato == null || !ansatt.startdato.isAfter(periode.tilDato)) &&
            (ansatt.sluttdato == null || !ansatt.sluttdato.isBefore(periode.fraDato))
    }
}
