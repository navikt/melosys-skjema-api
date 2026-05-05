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
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::maaOppgiLandSomUtbetalerPengestotte.name)
                ))
                return violations
            }
            belopViolation(
                dto.pengestotteSomMottasFraAndreLandBelop,
                SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBelop.name,
                SkatteforholdOgInntektTranslation::maaOppgiBelopPengestotte.name
            )?.let { violations.add(it); return violations }

            if (dto.pengestotteSomMottasFraAndreLandBeskrivelse.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::pengestotteSomMottasFraAndreLandBeskrivelse.name,
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::maaOppgiBeskrivelsePengestotte.name)
                ))
                return violations
            }
        }

        if (dto.inntektFraNorskEllerUtenlandskVirksomhet?.none { it.value } == true) {
            violations.add(Violation(
                field = SkatteforholdOgInntektDto::inntektFraNorskEllerUtenlandskVirksomhet.name,
                translationKey = translationKey(SkatteforholdOgInntektTranslation::maaVelgeMinsteEnInntektKilde.name)
            ))
        }

        if (dto.hvilkeTyperInntektHarDu?.none { it.value } == true) {
            violations.add(Violation(
                field = SkatteforholdOgInntektDto::hvilkeTyperInntektHarDu.name,
                translationKey = translationKey(SkatteforholdOgInntektTranslation::maaVelgeMinsteEnInntektType.name)
            ))
        }

        if (dto.inntektFraNorskEllerUtenlandskVirksomhet == null || dto.hvilkeTyperInntektHarDu == null) {
            return violations
        }

        if (violations.isNotEmpty()) return violations

        val inntektKilder = dto.inntektFraNorskEllerUtenlandskVirksomhet!!
        val inntektTyper = dto.hvilkeTyperInntektHarDu!!

        val harLoenn = inntektTyper[InntektType.LOENN.name] == true
        val harEgenVirksomhet = inntektTyper[InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET.name] == true
        val harNorskVirksomhet = inntektKilder[ArbeidsinntektKilde.NORSK_VIRKSOMHET.name] == true
        val harUtenlandskVirksomhet = inntektKilder[ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET.name] == true

        if (harLoenn) {
            val ugyldigLonnKombinasjon =
                dto.erSkattepliktigTilNorgeIHeleutsendingsperioden &&
                    harNorskVirksomhet &&
                    !harUtenlandskVirksomhet

            if (ugyldigLonnKombinasjon) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::hvilkeTyperInntektHarDu.name,
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::kannIkkeHaLonnNarKunNorskVirksomhet.name)
                ))
            } else {
                belopViolation(
                    dto.inntekt,
                    SkatteforholdOgInntektDto::inntekt.name,
                    SkatteforholdOgInntektTranslation::maaOppgiInntekt.name
                )?.let { violations.add(it) }
            }
        } else {
            if (!dto.inntekt.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::inntekt.name,
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::inntektSkalIkkeOppgis.name)
                ))
            }
        }

        if (harEgenVirksomhet) {
            belopViolation(
                dto.inntektFraEgenVirksomhet,
                SkatteforholdOgInntektDto::inntektFraEgenVirksomhet.name,
                SkatteforholdOgInntektTranslation::maaOppgiInntektFraEgenVirksomhet.name
            )?.let { violations.add(it) }
        } else {
            if (!dto.inntektFraEgenVirksomhet.isNullOrBlank()) {
                violations.add(Violation(
                    field = SkatteforholdOgInntektDto::inntektFraEgenVirksomhet.name,
                    translationKey = translationKey(SkatteforholdOgInntektTranslation::inntektFraEgenVirksomhetSkalIkkeOppgis.name)
                ))
            }
        }

        return violations
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
