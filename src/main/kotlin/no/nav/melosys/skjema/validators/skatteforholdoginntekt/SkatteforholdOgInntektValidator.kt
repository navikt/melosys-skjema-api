package no.nav.melosys.skjema.validators.skatteforholdoginntekt

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.SkatteforholdOgInntektTranslation
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsinntektKilde
import no.nav.melosys.skjema.types.utsendtarbeidstaker.InntektType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.springframework.stereotype.Component

@Component
class SkatteforholdOgInntektValidator {

    fun validate(dto: SkatteforholdOgInntektDto?): List<Violation> {
        if (dto == null) return listOf(
            Violation(field = "skatteforholdOgInntekt", translationKey = FELT_ER_PAAKREVD)
        )

        val pengestotteViolations = validatePengestotte(dto)
        if (pengestotteViolations.isNotEmpty()) return pengestotteViolations

        val inntektKilder = dto.inntektFraNorskEllerUtenlandskVirksomhet
        val inntektTyper = dto.hvilkeTyperInntektHarDu

        val valgViolations = validateInntektValg(inntektKilder, inntektTyper)
        if (valgViolations.isNotEmpty()) {
            return valgViolations
        }

        // Etter validateInntektValg vet vi at begge har minst én valgt verdi
        return validateInntektBelop(dto, inntektKilder!!, inntektTyper!!)
    }

    /**
     * Validerer pengestøtte-feltene sekvensielt.
     * Returnerer ved første feil, slik at bruker får én feilmelding om gangen.
     */
    private fun validatePengestotte(dto: SkatteforholdOgInntektDto): List<Violation> {
        if (!dto.mottarPengestotteFraAnnetEosLandEllerSveits) return emptyList()

        if (dto.landSomUtbetalerPengestotte.isNullOrBlank()) {
            return listOf(
                Violation(
                    field = SkatteforholdOgInntektDto::landSomUtbetalerPengestotte.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiLandSomUtbetalerPengestotte.name)
                )
            )
        }

        belopViolation(
            dto.pengestotteSomMottasFraAndreLandBelop,
            SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBelop.name,
            SkatteforholdOgInntektTranslation::maaOppgiBelopPengestotte.name
        )?.let { return listOf(it) }

        if (dto.pengestotteSomMottasFraAndreLandBeskrivelse.isNullOrBlank()) {
            return listOf(
                Violation(
                    field = SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBeskrivelse.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiBeskrivelsePengestotte.name)
                )
            )
        }

        return emptyList()
    }

    /**
     * Validerer at minst én inntektkilde og minst én inntekttype er valgt.
     * null eller tomt sett behandles som "ingen valgt" (manglende input).
     */
    private fun validateInntektValg(
        inntektKilder: Set<ArbeidsinntektKilde>?,
        inntektTyper: Set<InntektType>?
    ): List<Violation> {
        if (inntektKilder.isNullOrEmpty()) {
            return listOf(
                Violation(
                    field = SkatteforholdOgInntektDto::inntektFraNorskEllerUtenlandskVirksomhet.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaVelgeMinstEnInntektKilde.name)
                )
            )
        }
        if (inntektTyper.isNullOrEmpty()) {
            return listOf(
                Violation(
                    field = SkatteforholdOgInntektDto::hvilkeTyperInntektHarDu.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaVelgeMinstEnInntektType.name)
                )
            )
        }
        return emptyList()
    }

    /**
     * Validerer inntektsbeløp basert på valgte inntektkilder og -typer.
     * Delegerer til spesialiserte metoder per inntekttype.
     */
    private fun validateInntektBelop(
        dto: SkatteforholdOgInntektDto,
        inntektKilder: Set<ArbeidsinntektKilde>,
        inntektTyper: Set<InntektType>
    ): List<Violation> {
        val harNorskVirksomhet = ArbeidsinntektKilde.NORSK_VIRKSOMHET in inntektKilder
        val harUtenlandskVirksomhet = ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET in inntektKilder

        val loennViolations = validateLoennsinntekt(dto, inntektTyper, harNorskVirksomhet, harUtenlandskVirksomhet)
        if (loennViolations.isNotEmpty()) return loennViolations

        return validateEgenVirksomhetsInntekt(dto, inntektTyper)
    }

    private fun validateLoennsinntekt(
        dto: SkatteforholdOgInntektDto,
        inntektTyper: Set<InntektType>,
        harNorskVirksomhet: Boolean,
        harUtenlandskVirksomhet: Boolean
    ): List<Violation> {
        val harLoenn = InntektType.LOENN in inntektTyper

        if (!harLoenn) {
            if (!dto.inntekt.isNullOrBlank()) {
                return listOf(
                    Violation(
                        field = SkatteforholdOgInntektDto::inntekt.name,
                        translationKey = translationFieldName(SkatteforholdOgInntektTranslation::inntektSkalIkkeOppgis.name)
                    )
                )
            }
            return emptyList()
        }

        val loennIkkeTillatt =
            dto.erSkattepliktigTilNorgeIHeleutsendingsperioden &&
                harNorskVirksomhet &&
                !harUtenlandskVirksomhet

        if (loennIkkeTillatt) {
            if (!dto.inntekt.isNullOrBlank()) {
                return listOf(
                    Violation(
                        field = SkatteforholdOgInntektDto::inntekt.name,
                        translationKey = translationFieldName(SkatteforholdOgInntektTranslation::inntektSkalIkkeOppgis.name)
                    )
                )
            }
            return emptyList()
        }

        belopViolation(
            dto.inntekt,
            SkatteforholdOgInntektDto::inntekt.name,
            SkatteforholdOgInntektTranslation::maaOppgiInntekt.name
        )?.let { return listOf(it) }

        return emptyList()
    }

    private fun validateEgenVirksomhetsInntekt(
        dto: SkatteforholdOgInntektDto,
        inntektTyper: Set<InntektType>
    ): List<Violation> {
        val harEgenVirksomhet = InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET in inntektTyper

        if (harEgenVirksomhet) {
            belopViolation(
                dto.inntektFraEgenVirksomhet,
                SkatteforholdOgInntektDto::inntektFraEgenVirksomhet.name,
                SkatteforholdOgInntektTranslation::maaOppgiInntektFraEgenVirksomhet.name
            )?.let { return listOf(it) }
        } else if (!dto.inntektFraEgenVirksomhet.isNullOrBlank()) {
            return listOf(
                Violation(
                    field = SkatteforholdOgInntektDto::inntektFraEgenVirksomhet.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::inntektFraEgenVirksomhetSkalIkkeOppgis.name)
                )
            )
        }

        return emptyList()
    }

    /**
     * Returnerer en [Violation] hvis [verdi] mangler eller ikke er et gyldig positivt heltall,
     * ellers null.
     */
    private fun belopViolation(verdi: String?, fieldName: String, paakrevdKey: String): Violation? = when {
        verdi.isNullOrBlank() -> Violation(field = fieldName, translationKey = translationFieldName(paakrevdKey))
        !erGyldigBelop(verdi) -> Violation(field = fieldName, translationKey = translationFieldName(SkatteforholdOgInntektTranslation::duMaOppgiEtGyldigBelopSomErStorreEnn0.name))
        else -> null
    }

    companion object {
        private val GYLDIG_BELOP_REGEX = Regex("^[1-9]\\d*$")

        /** Gyldig beløp: kun sifre, heltall større enn 0, eksempel: "1000", "500" */
        internal fun erGyldigBelop(belop: String?): Boolean =
            belop?.trim()?.let { GYLDIG_BELOP_REGEX.matches(it) } == true

        private fun translationFieldName(fieldName: String) =
            "${ErrorMessageTranslation::skatteforholdOgInntektTranslation.name}.$fieldName"
    }
}
