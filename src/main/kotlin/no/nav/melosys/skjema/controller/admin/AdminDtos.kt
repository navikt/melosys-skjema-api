package no.nav.melosys.skjema.controller.admin

import java.time.Instant
import java.time.LocalDate
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
 * MELOSYS-8168 (midlertidig): Resultat av resending. Kandidatene finnes i koden (AG-del innsendt før
 * SMS ble aktivert, som fortsatt venter på AT-del), så endepunktet trenger ingen request-body.
 *
 * [saksnumre] lister sakene som faktisk fikk et nytt varsel (for sporbarhet på fagsiden), og
 * [antallSendt] er antallet i listen. Saker som mangler saksnummer representeres med skjema-id-en sin.
 */
data class ResendVarslerResultatDto(
    val antallSendt: Int,
    val saksnumre: List<String>
)

/**
 * Bruksstatistikk for skjemaene – ment for overvåking av bruk via melosys-console.
 * Inneholder kun aggregerte tall, ingen personopplysninger.
 */
data class BrukStatistikkDto(
    /** Tidspunkt statistikken ble beregnet (aldersgrenser regnes fra dette). */
    val tidspunkt: Instant,
    /**
     * Periodefilteret som ble brukt (innsendingsdato). Null = ingen grense (alt).
     * Gjelder alle innsendt-feltene (totalt, fordelinger, saksdekning, toppliste, unike).
     * [utkast] og innsendt-trenden er nåtilstand og påvirkes ikke av perioden.
     */
    val periodeFraOgMed: LocalDate?,
    val periodeTilOgMed: LocalDate?,
    val utkast: UtkastStatistikkDto,
    /** Totalt antall innsendte skjema (status SENDT) i perioden. */
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
    val antallUnikeVirksomheter: Long,
    /**
     * Anonym toppliste over de mest aktive virksomhetene, sortert synkende på antall innsendinger.
     * Inneholder bevisst kun tall (rang 1, 2, 3 ...), ikke orgnr eller navn.
     */
    val topplisteVirksomheter: List<VirksomhetStatistikkDto>
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
     * Antall unike saker (person + juridisk enhet) der begge deler er dekket – enten via et komplett
     * skjema, eller via en separat arbeidstaker-del og arbeidsgiver-del som matcher (overlappende
     * periode). Samme sak telles kun én gang selv om den har både komplett skjema og separate deler.
     */
    val antallSakerMedBeggeDeler: Long,
    /** Status for separate arbeidstaker-deler (med motpart / venter). */
    val arbeidstakerDeler: DelStatusDto,
    /** Status for separate arbeidsgiver-deler (med motpart / venter). */
    val arbeidsgiverDeler: DelStatusDto,
    /**
     * Mulige dobbeltinnsendinger: samme person har sendt samme del for samme juridiske enhet med
     * overlappende periode flere ganger. Versjons-erstatninger (erstatterSkjemaId) er holdt utenfor,
     * så dette er ekte mulige duplikater – ikke nye versjoner av samme søknad.
     */
    val antallMuligeDobbeltinnsendinger: Long,
    /**
     * Antall saker der minst én del er sendt i flere versjoner (samme person + juridisk enhet + del
     * sendt mer enn én gang). Inkluderer versjons-erstatninger – altså saker med «mer enn to deler».
     */
    val antallSakerMedFlereVersjoner: Long
)

/**
 * Status for en deltype (arbeidstaker- eller arbeidsgiver-deler) som er sendt hver for seg. Viser om
 * delen har en matchende motpart, og for de som venter: om motparten har påbegynt et utkast eller ikke.
 */
data class DelStatusDto(
    /** Totalt antall separate deler av denne typen. */
    val totalt: Long,
    /** Har en matchende, innsendt motpart (samme person + juridisk enhet + overlappende periode). */
    val medMotpart: Long,
    /** Mangler innsendt motpart, men motparten har påbegynt et utkast (under arbeid). */
    val venterMotpartHarUtkast: Long,
    /** Mangler motpart helt – verken innsendt eller påbegynt utkast. */
    val venterIngenMotpart: Long
)

/**
 * Anonym statistikk for én virksomhet i topplisten – kun tall, ingen orgnr eller navn. Brukerne
 * (innsendere) kan være flere personer som jobber for samme virksomhet.
 */
data class VirksomhetStatistikkDto(
    val antallInnsendinger: Long,
    /** Antall unike innsendere (personer som har sendt inn) for virksomheten. */
    val antallUnikeInnsendere: Long,
    val antallArbeidstakerDel: Long,
    val antallArbeidsgiverDel: Long,
    val antallKomplett: Long,
    /** Saker (person + juridisk enhet) i virksomheten der begge deler er dekket. */
    val antallSakerMedBeggeDeler: Long
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
    val eldsteOpprettetDato: Instant?,
    /** Påbegynte utkast fordelt på del – viser hvor folk starter, men (ennå) ikke har sendt inn. */
    val perSkjemadel: Map<Skjemadel, Long>
)
