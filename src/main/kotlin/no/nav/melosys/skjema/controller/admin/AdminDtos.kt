package no.nav.melosys.skjema.controller.admin

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel

/**
 * Administrativ visning av en innsending. Inneholder bevisst ingen personopplysninger
 * (fødselsnummer/navn) – kun orgnr og prosesseringsmetadata.
 */
data class InnsendingAdminDto(
    val innsendingId: UUID,
    val skjemaId: UUID,
    val referanseId: String,
    val status: InnsendingStatus,
    val skjemaStatus: SkjemaStatus,
    val orgnr: String,
    val antallForsok: Int,
    val feilmelding: String?,
    val sisteForsoekTidspunkt: Instant?,
    val opprettetDato: Instant,
    val saksnummer: String?
)

/**
 * Aggregert statistikk for skjema og innsendinger – nyttig som operasjonelt dashbord.
 */
data class AdminStatistikkDto(
    val skjemaPerStatus: Map<SkjemaStatus, Long>,
    val innsendingPerStatus: Map<InnsendingStatus, Long>,
    val antallFeiledeInnsendinger: Long
)

data class AntallDto(
    val antall: Long
)

data class RetryResultatDto(
    val antallForsoekt: Int,
    val antallFeilet: Int
)

/**
 * Resultat av MIDLERTIDIG opprydding av soft-deletede (SLETTET) utkast.
 *
 * @property antallSkjema antall skjema-rader som ble hard-slettet (cascade fjernet vedlegg/innsending)
 * @property antallVedleggSlettet antall vedlegg-blobs som ble slettet fra bucket
 * @property antallVedleggFeilet antall vedlegg-blobs som ikke lot seg slette (rad er likevel borte)
 */
data class RyddUtkastResultatDto(
    val antallSkjema: Int,
    val antallVedleggSlettet: Int,
    val antallVedleggFeilet: Int
)

/**
 * Bruksstatistikk for skjemaene – ment for overvåking av bruk via melosys-console.
 * Inneholder kun aggregerte tall, ingen personopplysninger.
 */
data class BrukStatistikkDto(
    /** Tidspunkt statistikken ble beregnet (aldersgrenser regnes fra dette). */
    val tidspunkt: Instant,
    val utkast: UtkastStatistikkDto,
    /** Totalt antall innsendte skjema (status SENDT). */
    val totaltInnsendt: Long,
    val innsendtSisteDoegn: Long,
    val innsendtSiste7Dager: Long,
    val innsendtSiste30Dager: Long,
    /** Innsendte fordelt på skjemadel (arbeidsgivers del, arbeidstakers del, komplett). */
    val innsendtPerSkjemadel: Map<Skjemadel, Long>,
    /** Innsendte fordelt på flyt/representasjonstype (deg selv, arbeidsgiver, rådgiver, ...). */
    val innsendtPerFlyt: Map<Representasjonstype, Long>,
    /** Innsendte fordelt på valgt språk ved innsending. */
    val innsendtPerSprak: Map<Språk, Long>,
    /** Antall innsendte komplette skjema (begge deler i én innsending). */
    val antallKomplettInnsendt: Long,
    /** Antall koblede par der arbeidsgivers del og arbeidstakers del er sendt hver for seg. */
    val antallKobledePar: Long,
    /** Saker der begge deler er dekket = komplette + koblede par. */
    val antallSakerMedBeggeDeler: Long,
    /** Unike personer (fnr) blant innsendte skjema. */
    val antallUnikePersoner: Long,
    /** Unike virksomheter (orgnr) blant innsendte skjema. */
    val antallUnikeVirksomheter: Long
)

/**
 * Antall utkast med aldersfordeling. Bøttene er gjensidig utelukkende og summerer til [antall].
 * Alder regnes fra opprettelsestidspunkt.
 */
data class UtkastStatistikkDto(
    val antall: Long,
    val under1Dag: Long,
    val mellom1Og7Dager: Long,
    val mellom7Og30Dager: Long,
    val over30Dager: Long,
    /** Opprettelsestidspunkt for det eldste utkastet, eller null hvis ingen utkast finnes. */
    val eldsteOpprettetDato: Instant?
)
