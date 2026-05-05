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
        if (dto == null) return listOf(Violation(
            field = "skatteforholdOgInntekt",
            translationKey = FELT_ER_PAAKREVD
        ))

        val violations = mutableListOf<Violation>()

        if (dto.mottarPengestotteFraAnnetEosLandEllerSveits) {
            if (dto.landSomUtbetalerPengestotte.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::landSomUtbetalerPengestotte.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiLandSomUtbetalerPengestotte.name)
                ))
                return violations
            }
            if (dto.pengestotteSomMottasFraAndreLandBelop.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBelop.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiBelopPengestotte.name)
                ))
                return violations
            }
            if (!erGyldigBelop(dto.pengestotteSomMottasFraAndreLandBelop!!)) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBelop.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::ugyldigBelopFormat.name)
                ))
                return violations
            }
            if (dto.pengestotteSomMottasFraAndreLandBeskrivelse.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBeskrivelse.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiBeskrivelsePengestotte.name)
                ))
                return violations
            }
        }

        val harArbeidsinntektKilde = dto.arbeidsinntektFraNorskEllerUtenlandskVirksomhet?.any { it.value } ?: true
        if (dto.arbeidsinntektFraNorskEllerUtenlandskVirksomhet != null && !harArbeidsinntektKilde) {
            violations.add(Violation(
                field = SkatteforholdOgInntektDto::arbeidsinntektFraNorskEllerUtenlandskVirksomhet.name,
                translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaVelgeMinsteEnArbeidsinntektKilde.name)
            ))
        }

        val harInntektType = dto.hvilkeTyperInntektHarDu?.any { it.value } ?: true
        if (dto.hvilkeTyperInntektHarDu != null && !harInntektType) {
            violations.add(Violation(
                field = SkatteforholdOgInntektDto::hvilkeTyperInntektHarDu.name,
                translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaVelgeMinsteEnInntektType.name)
            ))
        }

        if (dto.arbeidsinntektFraNorskEllerUtenlandskVirksomhet == null || dto.hvilkeTyperInntektHarDu == null) {
            return violations
        }

        if (violations.isNotEmpty()) return violations

        val arbeidsinntektKilder = dto.arbeidsinntektFraNorskEllerUtenlandskVirksomhet!!
        val inntektTyper = dto.hvilkeTyperInntektHarDu!!

        val harLoenn = inntektTyper[InntektType.LOENN.name] == true
        val harEgenVirksomhet = inntektTyper[InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET.name] == true
        val harNorskVirksomhet = arbeidsinntektKilder[ArbeidsinntektKilde.NORSK_VIRKSOMHET.name] == true
        val harUtenlandskVirksomhet = arbeidsinntektKilder[ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET.name] == true

        if (harLoenn) {
            val ugyldigLonnKombinasjon =
                dto.erSkattepliktigTilNorgeIHeleutsendingsperioden &&
                    harNorskVirksomhet &&
                    !harUtenlandskVirksomhet

            if (ugyldigLonnKombinasjon) {
                violations.add(
                    Violation(
                        field = SkatteforholdOgInntektDto::hvilkeTyperInntektHarDu.name,
                        translationKey = translationFieldName(
                            SkatteforholdOgInntektTranslation::kannIkkeHaLonnNarKunNorskVirksomhet.name
                        )
                    )
                )
            } else if (dto.inntekterFraUtenlandskVirksomhet.isNullOrBlank()) {
                violations.add(
                    Violation(
                        field = SkatteforholdOgInntektDto::inntekterFraUtenlandskVirksomhet.name,
                        translationKey = translationFieldName(
                            SkatteforholdOgInntektTranslation::maaOppgiInntekterFraUtenlandskVirksomhet.name
                        )
                    )
                )
            } else if (!erGyldigBelop(dto.inntekterFraUtenlandskVirksomhet!!)) {
                violations.add(
                    Violation(
                        field = SkatteforholdOgInntektDto::inntekterFraUtenlandskVirksomhet.name,
                        translationKey = translationFieldName(SkatteforholdOgInntektTranslation::ugyldigBelopFormat.name)
                    )
                )
            }
        } else {
            if (!dto.inntekterFraUtenlandskVirksomhet.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::inntekterFraUtenlandskVirksomhet.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::inntekterFraUtenlandskVirksomhetSkalIkkeOppgis.name)
                ))
            }
        }

        if (harEgenVirksomhet) {
            if (dto.inntekterFraEgenVirksomhet.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::inntekterFraEgenVirksomhet.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::maaOppgiInntekterFraEgenVirksomhet.name)
                ))
            } else if (!erGyldigBelop(dto.inntekterFraEgenVirksomhet!!)) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::inntekterFraEgenVirksomhet.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::ugyldigBelopFormat.name)
                ))
            }
        } else {
            if (!dto.inntekterFraEgenVirksomhet.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::inntekterFraEgenVirksomhet.name,
                    translationKey = translationFieldName(SkatteforholdOgInntektTranslation::inntekterFraEgenVirksomhetSkalIkkeOppgis.name)
                ))
            }
        }

        return violations
    }

    companion object {
        /** Gyldig beløp: positive kroner med øre (2 desimal plasser), eksempel: 1000,00 */
        private val BELOP_REGEX = Regex("""^\d+,\d{2}$""")

        fun erGyldigBelop(belop: String): Boolean = BELOP_REGEX.matches(belop.trim())

        private fun translationFieldName(fieldName: String): String {
            return "${ErrorMessageTranslation::skatteforholdOgInntektTranslation.name}.$fieldName"
        }
    }
}
