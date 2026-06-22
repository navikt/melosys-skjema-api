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
    /** Saksdekning – om begge deler (arbeidstaker + arbeidsgiver) er dekket, regnet fra faktiske verdier. */
    val saksdekning: SaksdekningDto,
    /** Unike personer (fnr) blant innsendte skjema. */
    val antallUnikePersoner: Long,
    /** Unike virksomheter (orgnr) blant innsendte skjema. */
    val antallUnikeVirksomheter: Long
)

/**
 * Saksdekning for utsendt arbeidstaker: en komplett A1-sak trenger BÅDE arbeidstakers del og
 * arbeidsgivers del. Disse kan komme som ett samlet skjema, eller som to separate deler.
 *
 * Tallene regnes ut fra **faktiske verdier** (samme fnr + samme juridiske enhet + overlappende
 * utsendelsesperiode) — samme matching som mottak bruker for å gruppere relaterte deler.
 */
data class SaksdekningDto(
    /** Skjema sendt som ett samlet skjema (begge deler i én innsending). */
    val antallKomplette: Long,
    /**
     * Saker der begge deler er dekket = komplette skjema + saker der en separat arbeidstaker-del og
     * en separat arbeidsgiver-del matcher (samme fnr + juridisk enhet + overlappende periode).
     * En «sak» = en unik (person, juridisk enhet) med begge deler i overlappende periode.
     */
    val antallSakerMedBeggeDeler: Long,
    /** Separate arbeidstaker-deler som har en matchende arbeidsgiver-del. */
    val antallArbeidstakerDelMedMotpart: Long,
    /** Separate arbeidsgiver-deler som har en matchende arbeidstaker-del. */
    val antallArbeidsgiverDelMedMotpart: Long,
    /** Arbeidstaker-deler som (ennå) mangler en matchende arbeidsgiver-del. */
    val antallArbeidstakerDelUtenMotpart: Long,
    /** Arbeidsgiver-deler som (ennå) mangler en matchende arbeidstaker-del. */
    val antallArbeidsgiverDelUtenMotpart: Long,
    /**
     * Mulige dobbeltinnsendinger: samme person har sendt samme del for samme juridiske enhet med
     * overlappende periode flere ganger. Versjons-erstatninger (erstatterSkjemaId) er holdt utenfor,
     * så dette er ekte mulige duplikater – ikke nye versjoner av samme søknad.
     */
    val antallMuligeDobbeltinnsendinger: Long
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
