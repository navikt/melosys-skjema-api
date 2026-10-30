package no.nav.melosys.skjema.validators.felles

import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslation
import no.nav.melosys.skjema.translations.dto.FellesTranslation
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhetBase
import no.nav.melosys.skjema.validators.Violation
import no.nav.melosys.skjema.validators.validerPaakrevdeTekstfelter

/** Feltene som kreves her, kreves også av vedtakskontrollene i melosys-api. */
object UtenlandskVirksomhetValidator {

    /** Samme grense som saksbehandler har for registreringsnummer i melosys-web. */
    const val MAKS_LENGDE_ORGANISASJONSNUMMER = 25

    fun validate(virksomheter: List<UtenlandskVirksomhetBase>?): List<Violation> =
        virksomheter.orEmpty()
            .map(::validate)
            .firstOrNull { it.isNotEmpty() }
            ?: emptyList()

    private fun validate(virksomhet: UtenlandskVirksomhetBase): List<Violation> {
        val paakrevdeFeltViolations = validerPaakrevdeTekstfelter(
            UtenlandskVirksomhetBase::navn.name to virksomhet.navn,
            UtenlandskVirksomhetBase::vegnavnOgHusnummer.name to virksomhet.vegnavnOgHusnummer,
            UtenlandskVirksomhetBase::postkode.name to virksomhet.postkode,
            UtenlandskVirksomhetBase::byStedsnavn.name to virksomhet.byStedsnavn,
            UtenlandskVirksomhetBase::land.name to virksomhet.land,
        )
        if (paakrevdeFeltViolations.isNotEmpty()) return paakrevdeFeltViolations

        if ((virksomhet.organisasjonsnummer?.length ?: 0) > MAKS_LENGDE_ORGANISASJONSNUMMER) {
            return listOf(Violation(
                field = UtenlandskVirksomhetBase::organisasjonsnummer.name,
                translationKey = translationFieldName(FellesTranslation::utenlandskOrganisasjonsnummerErForLangt.name)
            ))
        }

        return emptyList()
    }

    private fun translationFieldName(fieldName: String): String {
        return "${ErrorMessageTranslation::fellesTranslation.name}.$fieldName"
    }
}
