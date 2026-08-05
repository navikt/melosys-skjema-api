package no.nav.melosys.skjema.types

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel

/**
 * DTO for oversikt over innsendte søknader.
 * Inneholder informasjon som vises i tabellen på oversiktssiden.
 */
data class InnsendtSoknadOversiktDto(
    val id: UUID,
    val referanseId: String?,
    val saksnummer: String?, // Melosys-saksnummer (MEL-<n>), null hvis ikke mottatt fra melosys-api ennå
    val saksstatus: Saksstatus?, // null = ikke synket ennå (behandles som MOTTATT i visning)
    val motpartStatus: MotpartStatus,
    val skjemadel: Skjemadel,
    val arbeidsgiverNavn: String?,
    val arbeidsgiverOrgnr: String,
    val arbeidstakerNavn: String,
    val arbeidstakerFnrMaskert: String?, // Maskert fnr (f.eks. "010190*****")
    val arbeidstakerFodselsdato: LocalDate,
    val innsendtDato: Instant,
    val status: SkjemaStatus,
    val fullmaktAktiv: Boolean? = null // null=ikke relevant, true=aktiv, false=tapt
)

/**
 * Status for motpartens del av søknaden.
 *
 * Merk: motpart som sendte via annen kanal (Fyll-inn/Altinn/papir) gir ikke kobling,
 * og vises som VENTER inntil saken avsluttes i melosys-api (saksstatus er da eneste indikator).
 */
enum class MotpartStatus {
    /** Motparten har sendt inn sin del (koblet skjema med status SENDT) */
    HAR_SENDT,

    /** Skjemadelen har en motpart som ikke har sendt inn sin del ennå */
    VENTER,

    /** Skjemadelen har ingen motpart (kombinert del med fullmakt) */
    IKKE_RELEVANT
}

/**
 * Response-objekt for paginert liste av innsendte søknader.
 */
data class InnsendteSoknaderResponse(
    val soknader: List<InnsendtSoknadOversiktDto>,
    val totaltAntall: Int,
    val side: Int,
    val antallPerSide: Int
)
