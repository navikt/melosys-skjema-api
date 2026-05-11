package no.nav.melosys.skjema.integrasjon.pdl.dto

import java.time.LocalDateTime

/**
 * PDL kan ha flere parallelle verdier (f.eks. ulik master = FREG vs PDL),
 * og kan også returnere historiske verdier. Konsumenter må selv velge hvilken
 * de skal bruke. Vi velger siste ikke-historiske registrering.
 */
data class PdlMetadata(
    val historisk: Boolean? = null,
    val endringer: List<PdlEndring> = emptyList()
) {
    fun erIkkeHistorisk(): Boolean = historisk != true

    fun datoSistRegistrert(): LocalDateTime =
        endringer.filter { it.erIkkeOpphor() }
            .maxOfOrNull { it.registrert }
            ?: LocalDateTime.MIN
}

data class PdlEndring(
    val type: PdlEndringstype,
    val registrert: LocalDateTime
) {
    fun erIkkeOpphor(): Boolean = type != PdlEndringstype.OPPHOER
}

enum class PdlEndringstype {
    OPPRETT,
    KORRIGER,
    OPPHOER
}
