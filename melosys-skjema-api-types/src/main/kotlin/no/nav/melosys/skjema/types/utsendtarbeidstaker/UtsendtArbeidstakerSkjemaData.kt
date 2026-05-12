package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.SkjemaData
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.felles.VedleggValgDto

sealed interface UtsendtArbeidstakerSkjemaData : SkjemaData {
    val tilleggsopplysninger: TilleggsopplysningerDto?
    val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto?
    val vedlegg: VedleggValgDto?
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
    override val type: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVERS_DEL",
    val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
    val utenlandsoppdraget: UtenlandsoppdragetDto? = null,
    val arbeidstakerensLonn: ArbeidstakerensLonnDto? = null,
    val arbeidsstedIUtlandet: ArbeidsstedIUtlandetDto? = null,
    override val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
    override val tilleggsopplysninger: TilleggsopplysningerDto? = null,
    override val vedlegg: VedleggValgDto? = null
) : UtsendtArbeidstakerSkjemaData

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
    override val type: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL",
    override val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
    val arbeidssituasjon: ArbeidssituasjonDto? = null,
    val skatteforholdOgInntekt: SkatteforholdOgInntektDto? = null,
    val familiemedlemmer: FamiliemedlemmerDto? = null,
    override val tilleggsopplysninger: TilleggsopplysningerDto? = null,
    override val vedlegg: VedleggValgDto? = null
) : UtsendtArbeidstakerSkjemaData

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(
    override val type: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL",
    val arbeidsgiversData: ArbeidsgiversData = ArbeidsgiversData(),
    val arbeidstakersData: ArbeidstakersData = ArbeidstakersData(),
    override val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
    override val tilleggsopplysninger: TilleggsopplysningerDto? = null,
    override val vedlegg: VedleggValgDto? = null
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
