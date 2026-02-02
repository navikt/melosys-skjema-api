package no.nav.melosys.skjema.types

import jakarta.validation.constraints.NotNull

/**
 * Request for å hente utkast basert på representasjonskontekst.
 *
 * Filtreringen gjøres i backend basert på:
 * - DEG_SELV: Innlogget brukers egne utkast
 * - ARBEIDSGIVER: Alle arbeidsgivere bruker har tilgang til i Altinn
 * - RADGIVER: Kun utkast for det spesifikke rådgiverfirmaet (må oppgi radgiverfirmaOrgnr)
 * - ANNEN_PERSON: Alle personer bruker har fullmakt for
 */
data class HentUtkastRequest(
    @field:NotNull
    val representasjonstype: Representasjonstype,
    val radgiverfirmaOrgnr: String? = null // Påkrevd for RADGIVER
)
