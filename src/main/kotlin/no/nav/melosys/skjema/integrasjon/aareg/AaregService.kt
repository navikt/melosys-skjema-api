package no.nav.melosys.skjema.integrasjon.aareg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.aareg.dto.Arbeidsforhold
import no.nav.melosys.skjema.types.felles.PeriodeDto
import java.time.LocalDate
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class AaregService(
    private val aaregClient: AaregClient
) {

    /**
     * Om arbeidstakeren har et ordinært arbeidsforhold hos arbeidsgiveren som er, eller har vært, aktivt
     * gjennom hele [periode]. Arbeidsforholdet matcher når arbeidsgiveren er [orgnr] (oppgitt i søknaden)
     * eller når opplysningspliktig er [juridiskEnhetOrgnr]. Et arbeidsforhold uten sluttdato er aktivt.
     */
    fun harOrdinaertArbeidsforhold(fnr: String, orgnr: String, juridiskEnhetOrgnr: String, periode: PeriodeDto): Boolean {
        val arbeidsforhold = aaregClient.hentOrdinaereArbeidsforhold(fnr)
        val harTreff = arbeidsforhold.any {
            it.type?.kode == ARBEIDSFORHOLDTYPE_ORDINAERT && it.gjelder(orgnr, juridiskEnhetOrgnr) && it.dekker(periode)
        }
        log.info { "AAREG: ${arbeidsforhold.size} ordinære arbeidsforhold, treff hos arbeidsgiver i perioden: $harTreff" }
        return harTreff
    }

    /**
     * Organisasjonsnummeret til opplysningspliktig (juridisk enhet) for hvert ordinære arbeidsforhold som er
     * aktivt i dag, uten duplikater. Arbeidsforhold der opplysningspliktig er en person, er ikke med.
     */
    fun hentOpplysningspliktigeForAktiveArbeidsforhold(fnr: String): List<String> {
        val iDag = LocalDate.now().let { PeriodeDto(it, it) }
        val orgnumre = aaregClient.hentOrdinaereArbeidsforhold(fnr)
            .filter { it.type?.kode == ARBEIDSFORHOLDTYPE_ORDINAERT && it.dekker(iDag) }
            .mapNotNull { it.opplysningspliktig?.organisasjonsnummer() }
            .distinct()
        log.info { "AAREG: ${orgnumre.size} opplysningspliktige med aktive ordinære arbeidsforhold" }
        return orgnumre
    }

    private fun Arbeidsforhold.gjelder(orgnr: String, juridiskEnhetOrgnr: String): Boolean =
        arbeidssted?.organisasjonsnummer() == orgnr || opplysningspliktig?.organisasjonsnummer() == juridiskEnhetOrgnr

    private fun Arbeidsforhold.dekker(periode: PeriodeDto): Boolean {
        val ansatt = ansettelsesperiode ?: return false
        return (ansatt.startdato == null || !ansatt.startdato.isAfter(periode.fraDato)) &&
            (ansatt.sluttdato == null || !ansatt.sluttdato.isBefore(periode.tilDato))
    }
}
