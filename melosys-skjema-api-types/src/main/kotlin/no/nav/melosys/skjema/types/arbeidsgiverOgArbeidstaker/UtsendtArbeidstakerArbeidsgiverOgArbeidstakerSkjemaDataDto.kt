package no.nav.melosys.skjema.types.arbeidsgiverOgArbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(
    override val type: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL",
    val arbeidsgiversData: ArbeidsgiversData = ArbeidsgiversData(),
    val arbeidstakersData: ArbeidstakersData = ArbeidstakersData(),
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
) : UtsendtArbeidstakerSkjemaData {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ArbeidsgiversData(
        val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
        val utenlandsoppdraget: UtenlandsoppdragetDto? = null,
        val arbeidstakerensLonn: ArbeidstakerensLonnDto? = null,
        val arbeidsstedIUtlandet: ArbeidsstedIUtlandetDto? = null
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ArbeidstakersData(
        val arbeidssituasjon: ArbeidssituasjonDto? = null,
        val skatteforholdOgInntekt: SkatteforholdOgInntektDto? = null,
        val familiemedlemmer: FamiliemedlemmerDto? = null
    )
}
