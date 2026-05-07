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
        if (inntektKilder == null || inntektTyper == null || valgViolations.isNotEmpty()) {
            return valgViolations
        }

        return validateInntektBelop(dto, inntektKilder, inntektTyper)
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
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::maaOppgiLandSomUtbetalerPengestotte.name)
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
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::maaOppgiBeskrivelsePengestotte.name)
                )
            )
        }

        return emptyList()
    }

    /**
     * Validerer at minst én inntektkilde og minst én inntekttype er valgt.
     */
    private fun validateInntektValg(
        inntektKilder: Map<ArbeidsinntektKilde, Boolean>?,
        inntektTyper: Map<InntektType, Boolean>?
    ): List<Violation> = buildList {
        if (inntektKilder?.none { it.value } == true) {
            add(
                Violation(
                    field = SkatteforholdOgInntektDto::inntektFraNorskEllerUtenlandskVirksomhet.name,
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::maaVelgeMinstEnInntektKilde.name)
                )
            )
        }
        if (inntektTyper?.none { it.value } == true) {
            add(
                Violation(
                    field = SkatteforholdOgInntektDto::hvilkeTyperInntektHarDu.name,
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::maaVelgeMinstEnInntektType.name)
                )
            )
        }
    }

    /**
     * Validerer inntektsbeløp basert på valgte inntektkilder og -typer.
     * Forutsetter at [inntektKilder] og [inntektTyper] ikke er null.
     */
    private fun validateInntektBelop(
        dto: SkatteforholdOgInntektDto,
        inntektKilder: Map<ArbeidsinntektKilde, Boolean>,
        inntektTyper: Map<InntektType, Boolean>
    ): List<Violation> {
        val harLoenn = inntektTyper[InntektType.LOENN] == true
        val harEgenVirksomhet = inntektTyper[InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET] == true
        val harNorskVirksomhet = inntektKilder[ArbeidsinntektKilde.NORSK_VIRKSOMHET] == true
        val harUtenlandskVirksomhet = inntektKilder[ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET] == true

        return buildList {
            if (harLoenn) {
                val ugyldigLonnKombinasjon =
                    dto.erSkattepliktigTilNorgeIHeleutsendingsperioden &&
                        harNorskVirksomhet &&
                        !harUtenlandskVirksomhet

                if (ugyldigLonnKombinasjon) {
                    add(
                        Violation(
                            field = SkatteforholdOgInntektDto::hvilkeTyperInntektHarDu.name,
                            translationKey = translationKey(SkatteforholdOgInntektTranslation::kanIkkeHaLonnNarKunNorskVirksomhet.name)
                        )
                    )
                } else {
                    belopViolation(
                        dto.inntekt,
                        SkatteforholdOgInntektDto::inntekt.name,
                        SkatteforholdOgInntektTranslation::maaOppgiInntekt.name
                    )?.let(::add)
                }
            } else if (!dto.inntekt.isNullOrBlank()) {
                add(
                    Violation(
                        field = SkatteforholdOgInntektDto::inntekt.name,
                        translationKey = translationKey(SkatteforholdOgInntektTranslation::inntektSkalIkkeOppgis.name)
                    )
                )
            }

            if (harEgenVirksomhet) {
                belopViolation(
                    dto.inntektFraEgenVirksomhet,
                    SkatteforholdOgInntektDto::inntektFraEgenVirksomhet.name,
                    SkatteforholdOgInntektTranslation::maaOppgiInntektFraEgenVirksomhet.name
                )?.let(::add)
            } else if (!dto.inntektFraEgenVirksomhet.isNullOrBlank()) {
                add(
                    Violation(
                        field = SkatteforholdOgInntektDto::inntektFraEgenVirksomhet.name,
                        translationKey = translationKey(SkatteforholdOgInntektTranslation::inntektFraEgenVirksomhetSkalIkkeOppgis.name)
                    )
                )
            }
        }
    }

    /**
     * Returnerer en Violation hvis [verdi] mangler eller har ugyldig beløpsformat,
     * ellers null.
     */
    private fun belopViolation(verdi: String?, fieldName: String, paakrevdKey: String): Violation? = when {
        verdi.isNullOrBlank() -> Violation(field = fieldName, translationKey = translationKey(paakrevdKey))
        !erGyldigBelop(verdi) -> Violation(field = fieldName, translationKey = translationKey(SkatteforholdOgInntektTranslation::ugyldigBelopFormat.name))
        else -> null
    }

    companion object {
        /** Gyldig beløp: positive kroner med øre (2 desimaler), eksempel: 1000,00 */
        private val BELOP_REGEX = Regex("""^\d+,\d{2}$""")

        fun erGyldigBelop(belop: String?): Boolean =
            belop != null && BELOP_REGEX.matches(belop.trim())

        private fun translationKey(fieldName: String) =
            "${ErrorMessageTranslation::skatteforholdOgInntektTranslation.name}.$fieldName"
    }
}
