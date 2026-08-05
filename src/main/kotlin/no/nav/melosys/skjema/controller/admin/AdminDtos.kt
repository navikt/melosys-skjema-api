package no.nav.melosys.skjema.controller.admin

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.types.common.Saksstatus
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
    val saksnummer: String?,
    val saksstatus: Saksstatus?,
    val saksstatusOppdatert: Instant?
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
 * [saksnumre] lister sakene som fikk et nytt varsel — eller ved [dryRun] ville fått, uten at noe
 * sendes. [antallSendt] er antallet i listen. Saker som mangler saksnummer representeres med
 * skjema-id-en sin.
 */
data class ResendVarslerResultatDto(
    val dryRun: Boolean,
    val antallSendt: Int,
    val saksnumre: List<String>
)

/**
 * Resultat av MIDLERTIDIG opprydding av soft-deletede (SLETTET) utkast (MELOSYS-8157).
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
    /** Innsendte fordelt på synket saksstatus fra melosys-api. */
    val saksstatusFordeling: SaksstatusFordelingDto,
    /** Søknader startet fra motpart-CTA-en («arbeidsgiveren din har sendt inn sin del»). */
    val motpartCta: MotpartCtaStatistikkDto,
    /** Unike personer (fnr) blant innsendte skjema. */
    val antallUnikePersoner: Long,
    /**
     * Unike virksomheter blant innsendte skjema, talt på skjemaets orgnr – altså UNDERENHETER, der
     * flere underenheter av samme juridiske enhet telles hver for seg. Erstattede versjoner er med.
     */
    val antallUnikeVirksomheter: Long,
    /**
     * Unike juridiske enheter blant innsendte skjema – samme grunnlag som [antallUnikeVirksomheter]
     * (inkl. erstattede versjoner), men talt på juridisk enhet. Normalt ≤ [antallUnikeVirksomheter]
     * (kan avvike hvis en underenhet har byttet juridisk enhet i perioden, siden juridisk enhet
     * snapshottes ved opprettelse). Dette er nøkkelen [topplisteVirksomheter] grupperer på, men tallet
     * kan være høyere enn `topplisteVirksomheter.size`, som kun teller gjeldende deler.
     */
    val antallUnikeJuridiskeEnheter: Long = 0,
    /**
     * Anonym toppliste over de mest aktive virksomhetene (juridiske enheter), sortert synkende på
     * antall innsendinger. Inneholder bevisst kun tall (rang 1, 2, 3 ...), ikke orgnr eller navn.
     * Saksnumrene bak en rad kan hentes via `/admin/statistikk/bruk/virksomheter/{rang}/saksnumre`.
     *
     * NB – annet grunnlag enn tallene over: topplisten grupperer på JURIDISK ENHET (underenheter slås
     * sammen) og teller kun GJELDENDE innsendinger (erstattede versjoner er holdt utenfor). Antall rader
     * kan derfor avvike fra både [antallUnikeVirksomheter] og [antallUnikeJuridiskeEnheter], og summen
     * av `antallInnsendinger` fra [totaltInnsendt].
     */
    val topplisteVirksomheter: List<VirksomhetStatistikkDto>
)

/**
 * Saksdekning for utsendt arbeidstaker: en komplett A1-sak trenger BÅDE arbeidstakers del og
 * arbeidsgivers del. Disse kan komme som ett samlet skjema, eller som to separate deler.
 *
 * Tallene regnes ut fra **faktiske verdier** (samme fnr + samme juridiske enhet + overlappende
 * utsendelsesperiode) — samme matching som mottak bruker for å gruppere relaterte deler.
 *
 * To prinsipper gjelder for alle feltene her:
 * 1. **Kohort:** periodefilteret avgjør kun hvilke deler som TELLES (innsendingsdato i vinduet).
 *    Egenskapene deres (motpart, komplett-dekning, erstattet, duplikat-grupper) måles alltid mot
 *    HELE den innsendte populasjonen, så et par som er sendt i hver sin måned ikke brytes.
 * 2. **Gjeldende deler:** rader som er erstattet av en nyere versjon holdes utenfor tellingene.
 */
data class SaksdekningDto(
    /** Gjeldende skjema sendt som ett samlet skjema (begge deler i én innsending). */
    val antallKomplette: Long,
    /**
     * Komplette skjema i perioden som er erstattet av en nyere versjon. Holdt utenfor [antallKomplette],
     * men tatt med her slik at tallene kan avstemmes mot fordelingen:
     * `innsendtPerSkjemadel[ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL] = antallKomplette + antallErstattedeKomplette`.
     */
    val antallErstattedeKomplette: Long = 0,
    /**
     * Antall unike saker der begge deler er dekket – enten via et komplett skjema, eller via en separat
     * arbeidstaker-del og arbeidsgiver-del som matcher (overlappende periode). Samme sak telles kun én
     * gang selv om den har både komplett skjema og separate deler.
     *
     * NB – «sak» er her PERSON + JURIDISK ENHET, uansett periode. Er saken først dekket, telles den som
     * dekket for alle periodene til den personen i den enheten. Se [antallSakerMedKomplett] for hva det
     * betyr mot del-nivå-tallene.
     *
     * Dekomponeres av [antallSakerMedKomplett], [antallSakerMedMatchendeSeparateDeler] og
     * [antallSakerMedBaadeKomplettOgSeparate]:
     * `antallSakerMedBeggeDeler = antallSakerMedKomplett + antallSakerMedMatchendeSeparateDeler
     * - antallSakerMedBaadeKomplettOgSeparate`.
     */
    val antallSakerMedBeggeDeler: Long,
    /**
     * Del av [antallSakerMedBeggeDeler]: saker som er dekket av et gjeldende komplett skjema.
     *
     * Nøkkelen er PERSON + JURIDISK ENHET uten periode: finnes det ett komplett skjema for personen i
     * enheten, teller saken som dekket uavhengig av om periodene overlapper. Del-nivå-tallet
     * [DelStatusDto.dekketAvKomplettSkjema] krever derimot periodeoverlapp, så en enkelt del kan stå
     * som ventende samtidig som saken telles her.
     */
    val antallSakerMedKomplett: Long = 0,
    /** Del av [antallSakerMedBeggeDeler]: saker som er dekket av to matchende separate deler. */
    val antallSakerMedMatchendeSeparateDeler: Long = 0,
    /** Overlappet mellom [antallSakerMedKomplett] og [antallSakerMedMatchendeSeparateDeler] – saker som har begge. */
    val antallSakerMedBaadeKomplettOgSeparate: Long = 0,
    /** Status for separate arbeidstaker-deler (med motpart / dekket av komplett / venter). */
    val arbeidstakerDeler: DelStatusDto,
    /** Status for separate arbeidsgiver-deler (med motpart / dekket av komplett / venter). */
    val arbeidsgiverDeler: DelStatusDto,
    /**
     * Antall TILFELLER (grupper) av mulig dobbeltinnsending – ikke antall rader. Ett tilfelle er to
     * eller flere gjeldende deler av samme type for samme person + juridiske enhet med overlappende
     * periode. Versjons-erstatninger (erstatterSkjemaId) er holdt utenfor, så dette er ekte mulige
     * duplikater – ikke nye versjoner av samme søknad. Lik `muligeDobbeltinnsendinger.size`.
     */
    val antallMuligeDobbeltinnsendinger: Long,
    /** Ett element per tilfelle i [antallMuligeDobbeltinnsendinger], med saksnummer-kontekst for oppfølging. */
    val muligeDobbeltinnsendinger: List<DobbeltinnsendingDto> = emptyList(),
    /**
     * Antall saker i perioden der minst én deltype er sendt mer enn én gang med overlappende periode
     * (samme person + juridisk enhet). Dekker BÅDE versjons-erstatninger og mulige dobbeltinnsendinger.
     *
     * Selve egenskapen «flere versjoner» måles mot HELE den innsendte populasjonen (også erstattede
     * versjoner og versjoner sendt utenfor periodevinduet). I motsetning til de andre sak-tallene teller
     * en sak også når kun den erstattede versjonen ligger i vinduet – ellers ville feltet vist 0 samtidig
     * som `antallErstattedeVersjoner` viste 1. Versjoner med ikke-overlappende perioder telles ikke.
     */
    val antallSakerMedFlereVersjoner: Long,
    /**
     * Deler som står som ventende hos oss (mangler innsendt motpart), men der saken er AVSLUTTET
     * i Melosys – motpartens del kom trolig via en annen kanal. Disse skal ikke purres.
     * Summen av venter-avsluttet for begge deltypene.
     */
    val antallVentendeMedAvsluttetSak: Long,
    /**
     * Par der arbeidsgiversiden tok initiativet: arbeidstakerens skjema ble PÅBEGYNT (utkast startet)
     * etter at arbeidsgiverens del var SENDT INN. Måles kun for saker med matchende separate deler,
     * per unik sak (ikke per rad), og kun på delene som faktisk inngår i et match – deler på samme sak
     * som aldri matchet en motpart påvirker ikke tallet. Har samme person + enhet flere separate
     * utsendelser (ikke-overlappende perioder), måles initiativet på den TIDLIGSTE utsendelsen som har
     * et matchende par. Valget ser kun på GJELDENDE deler, så en sak der den første utsendelsen ble
     * korrigert sent, klassifiseres på den gjeldende tilstanden.
     *
     * Erstattede versjoner teller med i tidspunktene når de ligger i samme VERSJONSKJEDE som en av
     * utsendelsens matchende deler (koblet via erstatterSkjemaId, ikke via periode) – da brukes tidligste
     * utkast-start og tidligste innsending i kjeden på hver side, siden den tidligste versjonen sier når
     * siden faktisk startet. [parInitiertAvArbeidsgiver] + [parInitiertAvArbeidstaker] +
     * [parUavhengigStartet] = [antallSakerMedMatchendeSeparateDeler].
     */
    val parInitiertAvArbeidsgiver: Long = 0,
    /** Par der arbeidsgiverens skjema ble påbegynt etter at arbeidstakerens del var sendt inn – se [parInitiertAvArbeidsgiver]. */
    val parInitiertAvArbeidstaker: Long = 0,
    /** Par der begge sider startet utkastet sitt før noen av delene var sendt inn – ingen av dem utløste den andre. */
    val parUavhengigStartet: Long = 0,
    /**
     * Gjeldende komplette skjema fordelt på representasjonstype – skiller arbeidsgiver med fullmakt
     * fra rådgiver med fullmakt. Summerer til [antallKomplette].
     */
    val komplettPerFlyt: Map<Representasjonstype, Long> = emptyMap(),
    /**
     * Gjeldende separate deler uten utfylt utsendelsesperiode. Disse kan aldri matche en motpart og
     * havner alltid i venter-kategoriene – et datakvalitetsmål, ikke et reelt etterslep.
     */
    val antallDelerUtenPeriode: Long = 0
)

/**
 * Ett tilfelle av mulig dobbeltinnsending: en gruppe gjeldende deler av samme type for samme person
 * og juridiske enhet med overlappende perioder. Inneholder bevisst ingen direkte identifikatorer
 * (fnr/orgnr/navn); saksnumrene kan slås opp i Melosys av autorisert personell.
 */
data class DobbeltinnsendingDto(
    /** Antall innsendinger i gruppen (alltid minst 2). */
    val antallInnsendinger: Int,
    /**
     * Saksnumrene til innsendingene i gruppen, slik at tilfellet kan slås opp i Melosys.
     * Innsendinger uten saksnummer representeres med skjema-id-en sin.
     */
    val saksnumre: List<String>
)

/**
 * Backup-uttrekk av synk-tilstanden FØR massesynk/migrering: nøyaktig feltene synken kan endre,
 * per skjema-id. Inneholder bevisst ingen personopplysninger (ingen fnr, navn eller skjemadata) —
 * kan lastes ned fra melosys-console og brukes til gjenoppretting eller diff.
 */
data class SaksstatusUttrekkDto(
    val tidspunkt: Instant,
    val antall: Int,
    val rader: List<SaksstatusUttrekkRadDto>
)

data class SaksstatusUttrekkRadDto(
    val skjemaId: UUID,
    val referanseId: String,
    val saksnummer: String?,
    val saksstatus: Saksstatus?,
    val saksstatusOppdatert: Instant?
)

/**
 * Innsendte fordelt på saksstatus synket fra melosys-api.
 * [ukjent] = ikke synket ennå – skal gå mot 0 etter at massesynken er kjørt, og er dermed
 * også et mål på synk-dekningen.
 */
data class SaksstatusFordelingDto(
    val mottatt: Long,
    val avsluttet: Long,
    val ukjent: Long
)

/**
 * Effektmåling for motpart-CTA-en. Utkast er nåtilstand; innsendt følger periodefilteret.
 * Nevneren (antall ventende motpart-søknader totalt) finnes ikke her – konvertering leses
 * indirekte mot venter-tallene i [SaksdekningDto].
 */
data class MotpartCtaStatistikkDto(
    /** Påbegynte utkast startet fra CTA-en. */
    val antallUtkastViaCta: Long,
    /** Innsendte søknader startet fra CTA-en. */
    val antallInnsendtViaCta: Long
)

/**
 * Status for en deltype (arbeidstaker- eller arbeidsgiver-deler) som er sendt hver for seg.
 *
 * Kategoriene [medMotpart], [dekketAvKomplettSkjema], [venterMotpartHarUtkast] og
 * [venterIngenMotpart] er gjensidig utelukkende og prioriteres i den rekkefølgen:
 * `totalt = medMotpart + dekketAvKomplettSkjema + venterMotpartHarUtkast + venterIngenMotpart`.
 *
 * Kontrollsum mot fordelingen: `innsendtPerSkjemadel[del] = totalt + antallErstattedeVersjoner`.
 */
data class DelStatusDto(
    /** Antall GJELDENDE (ikke erstattede) separate deler av denne typen. */
    val totalt: Long,
    /**
     * Deler av denne typen i perioden som er erstattet av en nyere versjon. Holdt utenfor [totalt]
     * og alle kategoriene, men tatt med her slik at tallene kan avstemmes mot `innsendtPerSkjemadel`.
     */
    val antallErstattedeVersjoner: Long = 0,
    /** Har en matchende, innsendt motpartsdel (samme person + juridisk enhet + overlappende periode). */
    val medMotpart: Long,
    /** Del av [medMotpart] der saken IKKE er avsluttet i Melosys (inkl. ikke synket). */
    val medMotpartAktivSak: Long = 0,
    /** Del av [medMotpart] der saken er AVSLUTTET i Melosys. */
    val medMotpartAvsluttetSak: Long = 0,
    /**
     * Har ingen separat motpartsdel, men saken er likevel dekket av et komplett skjema (begge deler i
     * én innsending) for samme person + juridiske enhet med overlappende periode. Disse venter ikke
     * på noe, selv om delen isolert sett mangler en motpart.
     */
    val dekketAvKomplettSkjema: Long = 0,
    /** Del av [dekketAvKomplettSkjema] der saken IKKE er avsluttet i Melosys (inkl. ikke synket). */
    val dekketAvKomplettSkjemaAktivSak: Long = 0,
    /** Del av [dekketAvKomplettSkjema] der saken er AVSLUTTET i Melosys. */
    val dekketAvKomplettSkjemaAvsluttetSak: Long = 0,
    /** Udekket, men motparten har påbegynt et utkast (under arbeid). */
    val venterMotpartHarUtkast: Long,
    /** Udekket, og motparten har ikke startet noe – verken innsendt del eller påbegynt utkast. */
    val venterIngenMotpart: Long,
    /** Del av [venterMotpartHarUtkast] der saken IKKE er avsluttet i Melosys (inkl. ikke synket). */
    val venterMotpartHarUtkastAktivSak: Long,
    /** Del av [venterMotpartHarUtkast] der saken er AVSLUTTET i Melosys. */
    val venterMotpartHarUtkastAvsluttetSak: Long,
    /** Del av [venterIngenMotpart] der saken IKKE er avsluttet i Melosys (inkl. ikke synket) – reelt ventende. */
    val venterIngenMotpartAktivSak: Long,
    /** Del av [venterIngenMotpart] der saken er AVSLUTTET i Melosys. */
    val venterIngenMotpartAvsluttetSak: Long
)

/**
 * Anonym statistikk for én virksomhet i topplisten – kun tall, ingen orgnr eller navn. Virksomheten
 * er den JURIDISKE ENHETEN (samme nøkkel som saksdekningen bruker), så underenheter er slått sammen.
 * Brukerne (innsendere) kan være flere personer som jobber for samme virksomhet.
 * Tallene dekker virksomhetens gjeldende innsendinger i perioden.
 */
data class VirksomhetStatistikkDto(
    val antallInnsendinger: Long,
    /** Antall unike innsendere (personer som har sendt inn) for virksomheten. */
    val antallUnikeInnsendere: Long,
    val antallArbeidstakerDel: Long,
    val antallArbeidsgiverDel: Long,
    val antallKomplett: Long,
    /** Saker (person + juridisk enhet) i virksomheten der begge deler er dekket. */
    val antallSakerMedBeggeDeler: Long,
    /** Virksomhetens innsendinger med saksstatus MOTTATT i Melosys. */
    val antallMottatt: Long = 0,
    /** Virksomhetens innsendinger med saksstatus AVSLUTTET i Melosys. */
    val antallAvsluttet: Long = 0,
    /** Virksomhetens innsendinger uten synket saksstatus. */
    val antallUkjent: Long = 0
)

/**
 * Saksnumrene bak én rad i topplisten, slik at en aktiv virksomhet kan følges opp i Melosys.
 * Inneholder bevisst KUN saksnumre – ingen direkte identifikatorer (fnr/orgnr/navn); saksnumrene kan
 * slås opp i Melosys av autorisert personell.
 */
data class VirksomhetSaksnumreDto(
    /** Plasseringen i topplisten som ble slått opp (1-basert, samme sortering som `topplisteVirksomheter`). */
    val rang: Int,
    /** Antall innsendinger for virksomheten – identisk med `antallInnsendinger` på samme rad i topplisten. */
    val antallInnsendinger: Long,
    /**
     * Saksnumrene til virksomhetens innsendinger i perioden. Innsendinger uten saksnummer
     * representeres med skjema-id-en sin, så ingen innsending blir usynlig.
     */
    val saksnumre: List<String>
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
