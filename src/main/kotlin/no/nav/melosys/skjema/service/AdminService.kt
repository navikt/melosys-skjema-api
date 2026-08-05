package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.melosys.skjema.controller.admin.AdminStatistikkDto
import no.nav.melosys.skjema.controller.admin.BrukStatistikkDto
import no.nav.melosys.skjema.controller.admin.DelStatusDto
import no.nav.melosys.skjema.controller.admin.DobbeltinnsendingDto
import no.nav.melosys.skjema.controller.admin.MotpartCtaStatistikkDto
import no.nav.melosys.skjema.controller.admin.SaksstatusFordelingDto
import no.nav.melosys.skjema.controller.admin.SaksstatusUttrekkDto
import no.nav.melosys.skjema.controller.admin.SaksstatusUttrekkRadDto
import no.nav.melosys.skjema.controller.admin.InnsendingAdminDto
import no.nav.melosys.skjema.controller.admin.ResendVarslerResultatDto
import no.nav.melosys.skjema.controller.admin.RetryResultatDto
import no.nav.melosys.skjema.controller.admin.SaksdekningDto
import no.nav.melosys.skjema.controller.admin.RyddUtkastResultatDto
import no.nav.melosys.skjema.controller.admin.UtkastStatistikkDto
import no.nav.melosys.skjema.controller.admin.VirksomhetSaksnumreDto
import no.nav.melosys.skjema.controller.admin.VirksomhetStatistikkDto
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.extensions.overlapper
import no.nav.melosys.skjema.extensions.utsendelsePeriode
import no.nav.melosys.skjema.integrasjon.storage.VedleggStorageClient
import no.nav.melosys.skjema.repository.AdminStatistikkRepository
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettetVia
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}
private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")

/** MELOSYS-8168: En resend-kandidat – skjema som skal få varsel på nytt, med saksnummer hvis det finnes. */
data class ResendKandidat(
    val skjemaId: UUID,
    val saksnummer: String?
)

/**
 * Administrative operasjoner som eksponeres via [no.nav.melosys.skjema.controller.admin.AdminController]
 * og konsumeres av melosys-console. Gir innsyn i og mulighet til å reprosessere feilede innsendinger.
 */
@Service
class AdminService(
    private val innsendingRepository: InnsendingRepository,
    private val skjemaRepository: SkjemaRepository,
    private val adminStatistikkRepository: AdminStatistikkRepository,
    private val innsendingService: InnsendingService,
    private val arbeidstakerVarslingService: ArbeidstakerVarslingService,
    private val vedleggStorageClient: VedleggStorageClient
) {

    @Transactional(readOnly = true)
    fun hentStatistikk(): AdminStatistikkDto {
        val innsendingPerStatus = InnsendingStatus.entries.associateWith { innsendingRepository.countByStatus(it) }
        return AdminStatistikkDto(
            skjemaPerStatus = SkjemaStatus.entries.associateWith { skjemaRepository.countByStatus(it) },
            innsendingPerStatus = innsendingPerStatus,
            antallFeiledeInnsendinger = innsendingPerStatus[InnsendingStatus.KAFKA_FEILET] ?: 0L
        )
    }

    /**
     * Bruksstatistikk. Innsendt-statistikken (fordelinger, saksdekning, toppliste, unike) regnes i
     * minnet fra innsendinger med skjema, filtrert på innsendingsdato [fraOgMed]–[tilOgMed] (begge
     * valgfrie; null = ingen grense). Utkast og innsendt-trend er nåtilstand og påvirkes ikke av perioden.
     *
     * Periodefilteret avgjør kun hvilke deler som TELLES; egenskapene deres (motpart, komplett-dekning,
     * erstattet-markering, duplikat-grupper) måles alltid mot hele den innsendte populasjonen.
     */
    @Transactional(readOnly = true)
    fun hentBruksstatistikk(fraOgMed: LocalDate?, tilOgMed: LocalDate?): BrukStatistikkDto {
        val naa = Instant.now()
        val populasjon = hentInnsendtPopulasjon()
        val kohort = filtrerPaaPeriode(populasjon, fraOgMed, tilOgMed)

        val utkastSkjemaer = adminStatistikkRepository.finnAlleUtkast()
        val utkast = utkastSkjemaer.mapNotNull { skjema ->
            val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@mapNotNull null
            UtkastSkjema(skjema.fnr, metadata.juridiskEnhetOrgnr, metadata.skjemadel, skjema.utsendelsePeriode())
        }

        val trend = adminStatistikkRepository.innsendtTrend(
            grense1d = naa.minus(1, ChronoUnit.DAYS),
            grense7d = naa.minus(7, ChronoUnit.DAYS),
            grense30d = naa.minus(30, ChronoUnit.DAYS)
        )

        val indeks = SaksIndeks(populasjon)
        return BrukStatistikkDto(
            tidspunkt = naa,
            periodeFraOgMed = fraOgMed,
            periodeTilOgMed = tilOgMed,
            utkast = hentUtkastStatistikk(naa, utkast),
            totaltInnsendt = kohort.size.toLong(),
            innsendtSisteDoegn = trend.sisteDoegn,
            innsendtSiste7Dager = trend.siste7Dager,
            innsendtSiste30Dager = trend.siste30Dager,
            innsendtPerSkjemadel = Skjemadel.entries.associateWith { sd -> kohort.count { it.skjemadel == sd }.toLong() },
            innsendtPerFlyt = Representasjonstype.entries.associateWith { f -> kohort.count { it.flyt == f }.toLong() },
            innsendtPerSprak = Språk.entries.associateWith { sp -> kohort.count { it.sprak == sp }.toLong() },
            saksdekning = beregnSaksdekning(kohort, populasjon, indeks, utkast),
            saksstatusFordeling = SaksstatusFordelingDto(
                mottatt = kohort.count { it.saksstatus == Saksstatus.MOTTATT }.toLong(),
                avsluttet = kohort.count { it.saksstatus == Saksstatus.AVSLUTTET }.toLong(),
                ukjent = kohort.count { it.saksstatus == null }.toLong()
            ),
            motpartCta = MotpartCtaStatistikkDto(
                antallUtkastViaCta = utkastSkjemaer.count { it.opprettetVia == OpprettetVia.MOTPART_CTA }.toLong(),
                antallInnsendtViaCta = kohort.count { it.opprettetVia == OpprettetVia.MOTPART_CTA }.toLong()
            ),
            antallUnikePersoner = kohort.mapTo(mutableSetOf()) { it.fnr }.size.toLong(),
            antallUnikeVirksomheter = kohort.mapTo(mutableSetOf()) { it.orgnr }.size.toLong(),
            antallUnikeJuridiskeEnheter = kohort.mapTo(mutableSetOf()) { it.juridiskEnhet }.size.toLong(),
            topplisteVirksomheter = grupperVirksomheter(kohort).map { gruppe -> gruppe.tilDto(indeks) }
        )
    }

    /**
     * Saksnumrene bak én rad i topplisten (1-basert [rang], samme gruppering/sortering/periode som
     * `topplisteVirksomheter`). Returnerer kun saksnumre – ingen direkte identifikatorer (fnr/orgnr/navn).
     *
     * @throws NoSuchElementException hvis [rang] er utenfor topplisten for perioden
     */
    @Transactional(readOnly = true)
    fun hentVirksomhetSaksnumre(rang: Int, fraOgMed: LocalDate?, tilOgMed: LocalDate?): VirksomhetSaksnumreDto {
        val kohort = filtrerPaaPeriode(hentInnsendtPopulasjon(), fraOgMed, tilOgMed)
        val virksomheter = grupperVirksomheter(kohort)
        val gruppe = virksomheter.getOrNull(rang - 1)
            ?: throw NoSuchElementException("Ingen virksomhet med rang $rang i topplisten (${virksomheter.size} virksomheter i perioden)")
        return VirksomhetSaksnumreDto(
            rang = rang,
            antallInnsendinger = gruppe.deler.size.toLong(),
            // Innsendinger uten saksnummer representeres med skjema-id-en, så ingen blir usynlig.
            saksnumre = gruppe.deler.map { it.saksnummer ?: it.id.toString() }.sorted()
        )
    }

    /** Alle innsendte utsendt-arbeidstaker-deler med versjonslenken satt fra hele populasjonen. */
    private fun hentInnsendtPopulasjon(): List<InnsendtSkjema> {
        val alle = innsendingRepository.finnAlleInnsendteMedSkjema()
        // Versjonslenken snus: erstattet skjema -> versjonen som erstattet det.
        val erstattetAv: Map<UUID, UUID> = alle
            .mapNotNull { innsending ->
                val erstatter = (innsending.skjema.metadata as? UtsendtArbeidstakerMetadata)?.erstatterSkjemaId
                erstatter?.let { it to innsending.skjema.id!! }
            }
            .toMap()
        return alle.mapNotNull { innsending ->
            val metadata = innsending.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@mapNotNull null
            InnsendtSkjema(
                id = innsending.skjema.id!!,
                fnr = innsending.skjema.fnr,
                orgnr = innsending.skjema.orgnr,
                innsenderFnr = innsending.innsenderFnr,
                juridiskEnhet = metadata.juridiskEnhetOrgnr,
                skjemadel = metadata.skjemadel,
                flyt = metadata.representasjonstype,
                sprak = innsending.innsendtSprak,
                periode = innsending.skjema.utsendelsePeriode(),
                erstattetAv = erstattetAv[innsending.skjema.id],
                saksstatus = innsending.saksstatus,
                saksnummer = innsending.saksnummer,
                opprettetVia = innsending.skjema.opprettetVia,
                skjemaOpprettetDato = innsending.skjema.opprettetDato,
                innsendtDato = innsending.opprettetDato
            )
        }
    }

    private fun filtrerPaaPeriode(
        populasjon: List<InnsendtSkjema>,
        fraOgMed: LocalDate?,
        tilOgMed: LocalDate?
    ): List<InnsendtSkjema> {
        val fraGrense = fraOgMed?.atStartOfDay(OSLO)?.toInstant()
        val tilGrenseEksklusiv = tilOgMed?.plusDays(1)?.atStartOfDay(OSLO)?.toInstant()
        return populasjon.filter { innenfor(it.innsendtDato, fraGrense, tilGrenseEksklusiv) }
    }

    /**
     * Backup-uttrekk av synk-tilstanden før massesynk: feltene synken kan endre, per skjema-id.
     * Ingen personopplysninger — se [SaksstatusUttrekkDto].
     */
    @Transactional(readOnly = true)
    fun hentSaksstatusUttrekk(): SaksstatusUttrekkDto {
        val rader = innsendingRepository.finnSaksstatusUttrekk().map { rad ->
            SaksstatusUttrekkRadDto(
                skjemaId = rad.skjemaId,
                referanseId = rad.referanseId,
                saksnummer = rad.saksnummer,
                saksstatus = rad.saksstatus,
                saksstatusOppdatert = rad.saksstatusOppdatert
            )
        }
        return SaksstatusUttrekkDto(tidspunkt = Instant.now(), antall = rader.size, rader = rader)
    }

    private fun innenfor(tidspunkt: Instant, fra: Instant?, tilEksklusiv: Instant?): Boolean =
        (fra == null || !tidspunkt.isBefore(fra)) && (tilEksklusiv == null || tidspunkt.isBefore(tilEksklusiv))

    /**
     * Beregner saksdekning ut fra faktiske verdier: to deler hører til samme sak når de har samme
     * fnr + samme juridiske enhet + overlappende utsendelsesperiode — samme matching som mottak
     * bruker for å gruppere relaterte deler. For ventende deler ses det også mot påbegynte utkast.
     *
     * [kohort] er delene som telles (innenfor periodefilteret), [populasjon] er alle innsendte deler
     * og avgjør egenskapene deres via [indeks]. Erstattede versjoner holdes utenfor tellingene.
     */
    private fun beregnSaksdekning(
        kohort: List<InnsendtSkjema>,
        populasjon: List<InnsendtSkjema>,
        indeks: SaksIndeks,
        utkast: List<UtkastSkjema>
    ): SaksdekningDto {
        val utkastArbeidstakerPerSak = utkast.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }.groupBy { it.sakNokkel() }
        val utkastArbeidsgiverPerSak = utkast.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }.groupBy { it.sakNokkel() }

        val arbeidstakerStatus = delStatus(
            kohort.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL },
            indeks.arbeidsgiverePerSak,
            indeks,
            utkastArbeidsgiverPerSak
        )
        val arbeidsgiverStatus = delStatus(
            kohort.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL },
            indeks.arbeidstakerePerSak,
            indeks,
            utkastArbeidstakerPerSak
        )

        val gjeldendeKohort = kohort.filter { !it.erstattet }
        val komplette = gjeldendeKohort.filter { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }
        val separateKohort = gjeldendeKohort.filter { it.skjemadel != Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }

        val kohortNokler = gjeldendeKohort.mapTo(mutableSetOf()) { it.sakNokkel() }
        val medKomplett = kohortNokler.filterTo(mutableSetOf()) { indeks.harKomplett(it) }
        val medSeparate = kohortNokler.filterTo(mutableSetOf()) { indeks.harMatchendeSeparateDeler(it) }
        val initiativ = beregnInitiativ(medSeparate, populasjon)

        // Gruppene bygges over hele populasjonen, men telles kun når de berører kohorten.
        val kohortIder = gjeldendeKohort.mapTo(mutableSetOf()) { it.id }
        val dobbeltinnsendinger = indeks.dobbeltinnsendinger
            .filter { gruppe -> gruppe.any { it.id in kohortIder } }
            .map { gruppe ->
                DobbeltinnsendingDto(
                    antallInnsendinger = gruppe.size,
                    saksnumre = gruppe.map { it.saksnummer ?: it.id.toString() }.sorted()
                )
            }
            .sortedWith(compareByDescending<DobbeltinnsendingDto> { it.antallInnsendinger }.thenBy { it.saksnumre.first() })

        return SaksdekningDto(
            antallKomplette = komplette.size.toLong(),
            antallErstattedeKomplette = kohort.count {
                it.erstattet && it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
            }.toLong(),
            antallSakerMedBeggeDeler = (medKomplett + medSeparate).size.toLong(),
            antallSakerMedKomplett = medKomplett.size.toLong(),
            antallSakerMedMatchendeSeparateDeler = medSeparate.size.toLong(),
            antallSakerMedBaadeKomplettOgSeparate = medKomplett.intersect(medSeparate).size.toLong(),
            arbeidstakerDeler = arbeidstakerStatus,
            arbeidsgiverDeler = arbeidsgiverStatus,
            antallMuligeDobbeltinnsendinger = dobbeltinnsendinger.size.toLong(),
            muligeDobbeltinnsendinger = dobbeltinnsendinger,
            // Egen nøkkelmengde: en sak kan ha bare den erstattede versjonen i vinduet, og skal likevel telles.
            antallSakerMedFlereVersjoner = kohort.mapTo(mutableSetOf()) { it.sakNokkel() }
                .count { indeks.harFlereVersjoner(it) }.toLong(),
            antallVentendeMedAvsluttetSak = listOf(arbeidstakerStatus, arbeidsgiverStatus).sumOf {
                it.venterMotpartHarUtkastAvsluttetSak + it.venterIngenMotpartAvsluttetSak
            },
            parInitiertAvArbeidsgiver = initiativ.arbeidsgiver,
            parInitiertAvArbeidstaker = initiativ.arbeidstaker,
            parUavhengigStartet = initiativ.uavhengig,
            komplettPerFlyt = Representasjonstype.entries.associateWith { f -> komplette.count { it.flyt == f }.toLong() },
            antallDelerUtenPeriode = separateKohort.count { it.periode == null }.toLong()
        )
    }

    /**
     * Status for en gjeldende del: har en innsendt separat motpart, er dekket av et komplett skjema,
     * eller venter. For de som venter skilles det på om motparten har påbegynt et utkast eller ikke.
     * Kategoriene er gjensidig utelukkende og prioriteres i den rekkefølgen.
     *
     * Merk – med vilje to ulike matchekriterier:
     * - innsendt motpart / komplett skjema krever overlappende periode (en reell, fullført sak).
     * - utkast-motpart matcher kun på samme person + juridisk enhet (ikke periode), fordi et utkast
     *   under arbeid ofte ikke har fylt inn periode ennå. Hensikten er «har motparten startet noe».
     */
    private fun delStatus(
        kohortDeler: List<InnsendtSkjema>,
        motpartSendtPerSak: Map<Pair<String, String>, List<InnsendtSkjema>>,
        indeks: SaksIndeks,
        motpartUtkastPerSak: Map<Pair<String, String>, List<UtkastSkjema>>
    ): DelStatusDto {
        var medMotpart = 0L
        var medMotpartAvsluttet = 0L
        var dekketAvKomplett = 0L
        var dekketAvKomplettAvsluttet = 0L
        var venterMotpartHarUtkast = 0L
        var venterMotpartHarUtkastAvsluttet = 0L
        var venterIngenMotpart = 0L
        var venterIngenMotpartAvsluttet = 0L
        val gjeldende = kohortDeler.filter { !it.erstattet }
        for (del in gjeldende) {
            val avsluttet = del.saksstatus == Saksstatus.AVSLUTTET
            when {
                motpartSendtPerSak[del.sakNokkel()]?.any { del.matcher(it) } == true -> {
                    medMotpart++
                    if (avsluttet) medMotpartAvsluttet++
                }
                indeks.komplettePerSak[del.sakNokkel()]?.any { del.matcher(it) } == true -> {
                    dekketAvKomplett++
                    if (avsluttet) dekketAvKomplettAvsluttet++
                }
                // Bevisst kun person + juridisk enhet (ikke periode): se kommentar over.
                motpartUtkastPerSak[del.sakNokkel()]?.isNotEmpty() == true -> {
                    venterMotpartHarUtkast++
                    if (avsluttet) venterMotpartHarUtkastAvsluttet++
                }
                else -> {
                    venterIngenMotpart++
                    if (avsluttet) venterIngenMotpartAvsluttet++
                }
            }
        }
        return DelStatusDto(
            totalt = gjeldende.size.toLong(),
            antallErstattedeVersjoner = (kohortDeler.size - gjeldende.size).toLong(),
            medMotpart = medMotpart,
            medMotpartAktivSak = medMotpart - medMotpartAvsluttet,
            medMotpartAvsluttetSak = medMotpartAvsluttet,
            dekketAvKomplettSkjema = dekketAvKomplett,
            dekketAvKomplettSkjemaAktivSak = dekketAvKomplett - dekketAvKomplettAvsluttet,
            dekketAvKomplettSkjemaAvsluttetSak = dekketAvKomplettAvsluttet,
            venterMotpartHarUtkast = venterMotpartHarUtkast,
            venterIngenMotpart = venterIngenMotpart,
            venterMotpartHarUtkastAktivSak = venterMotpartHarUtkast - venterMotpartHarUtkastAvsluttet,
            venterMotpartHarUtkastAvsluttetSak = venterMotpartHarUtkastAvsluttet,
            venterIngenMotpartAktivSak = venterIngenMotpart - venterIngenMotpartAvsluttet,
            venterIngenMotpartAvsluttetSak = venterIngenMotpartAvsluttet
        )
    }

    private data class InitiativFordeling(val arbeidsgiver: Long, val arbeidstaker: Long, val uavhengig: Long)

    /**
     * Fordeler saker med matchende separate deler på hvem som utløste paret. Signalet er om den ene
     * sidens skjema ble PÅBEGYNT (skjema.opprettetDato) etter at den andre sidens del var SENDT INN
     * (innsending.opprettetDato) – da fikk den siden trolig beskjed om at motparten hadde levert.
     * Startet begge sider før noen av delene var innsendt, er de uavhengige initiativ.
     *
     * Tidspunktene måles innenfor ÉN utsendelse: de GJELDENDE separate delene på nøkkelen grupperes i
     * klynger av overlappende perioder (samme transitive lukning som duplikat-grupperingen), og
     * initiativet regnes på den tidligste klyngen som har et faktisk par. Dermed blandes ikke tidspunkter
     * på tvers av to ulike utsendelser for samme person + enhet. Hver sak klassifiseres kun én gang.
     *
     * Se [initiativPar] for hvilke deler i klyngen som utgjør tidspunkt-grunnlaget, og hvordan tidligere
     * versjoner knyttes til klyngen via versjonslenken.
     */
    private fun beregnInitiativ(saker: Set<Pair<String, String>>, populasjon: List<InnsendtSkjema>): InitiativFordeling {
        val separateDelerPerSak = populasjon
            .filter { it.skjemadel != Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }
            .groupBy { it.sakNokkel() }
        val gjeldendeVersjon = gjeldendeVersjonPerErstattetDel(populasjon)
        var arbeidsgiver = 0L
        var arbeidstaker = 0L
        var uavhengig = 0L
        var utenPar = 0
        for (sak in saker) {
            val (gjeldende, erstattede) = separateDelerPerSak[sak].orEmpty().partition { !it.erstattet }
            // Klyngegrensene forankres i gjeldende data: en erstattet versjon med feil periode skal ikke
            // kunne lime to reelle utsendelser sammen. Erstattede deler knyttes til klyngen i [initiativPar].
            val par = overlappendeGrupper(gjeldende)
                .mapNotNull { klynge -> initiativPar(klynge, erstattede, gjeldendeVersjon) }
                // Tidligste utsendelse vinner; id-en bryter likhet, siden innsendingene ikke har noen annen
                // deterministisk rekkefølge.
                .minWithOrNull(compareBy({ it.valgtEtterInnsending }, { it.valgtEtterId }))
            if (par == null) {
                // Uoppnåelig så lenge [saker] og [populasjon] er utledet av samme datasett: nøkkelen er i
                // medSeparate, altså finnes det en direkte AT↔AG-kant mellom to gjeldende deler, og de to
                // havner alltid i samme klynge. Telles og logges, så saken ikke forsvinner stille ut av
                // kontrollsummen mot antallSakerMedMatchendeSeparateDeler.
                utenPar++
                continue
            }
            when {
                par.atStartet.isAfter(par.agInnsendt) -> arbeidsgiver++
                par.agStartet.isAfter(par.atInnsendt) -> arbeidstaker++
                else -> uavhengig++
            }
        }
        if (utenPar > 0) {
            log.warn {
                "Bruksstatistikk: $utenPar sak(er) med matchende separate deler manglet en klynge med " +
                    "faktisk par – initiativ-tallene summerer ikke til antallSakerMedMatchendeSeparateDeler"
            }
        }
        return InitiativFordeling(arbeidsgiver, arbeidstaker, uavhengig)
    }

    /**
     * Kobler hver erstattede del til den gjeldende versjonen den til slutt ble erstattet av, ved å følge
     * versjonslenken (erstatterSkjemaId) transitivt gjennom mellomliggende versjoner. Lenken er eksakt og
     * uavhengig av hvilke perioder versjonene er utfylt med. En sykel i lenken stopper gjennomgangen.
     */
    private fun gjeldendeVersjonPerErstattetDel(populasjon: List<InnsendtSkjema>): Map<UUID, UUID> {
        val nesteVersjon = populasjon.mapNotNull { del -> del.erstattetAv?.let { del.id to it } }.toMap()
        return nesteVersjon.keys.associateWith { erstattetId ->
            var versjon = nesteVersjon.getValue(erstattetId)
            val besokt = mutableSetOf(erstattetId, versjon)
            while (true) {
                val neste = nesteVersjon[versjon] ?: break
                if (!besokt.add(neste)) break
                versjon = neste
            }
            versjon
        }
    }

    /** Tidspunktene ett par klassifiseres på, og nøkkelen paret velges på når saken har flere utsendelser. */
    private data class InitiativPar(
        val atStartet: Instant,
        val agStartet: Instant,
        val atInnsendt: Instant,
        val agInnsendt: Instant,
        val valgtEtterInnsending: Instant,
        val valgtEtterId: UUID
    )

    /**
     * Tidspunkt-grunnlaget for én klynge av gjeldende deler, eller null hvis klyngen ikke har et faktisk par.
     *
     * Kun deler som matcher minst én del på MOTSATT side teller: en del kan være transitivt koblet inn i
     * klyngen via en del på samme side uten selv å ha en motpart, og skal da ikke dra tidspunktene med seg.
     *
     * [erstattede] versjoner tas med i tidspunktene (den tidligste versjonen sier når siden faktisk startet),
     * men kobles via VERSJONSLENKEN [gjeldendeVersjon] – ikke via periodeoverlapp – slik at en erstattet
     * versjon med feil periode ikke kan forgifte tidsstemplene til en annen utsendelse. En erstattet del
     * teller kun når versjonskjeden ender i en av klyngens matchende deler. Erstattede deler påvirker ikke
     * hvilken klynge som velges.
     */
    private fun initiativPar(
        klynge: List<InnsendtSkjema>,
        erstattede: List<InnsendtSkjema>,
        gjeldendeVersjon: Map<UUID, UUID>
    ): InitiativPar? {
        val atIKlynge = klynge.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }
        val agIKlynge = klynge.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }
        val atMatchende = atIKlynge.filter { at -> agIKlynge.any { at.matcher(it) } }
        val agMatchende = agIKlynge.filter { ag -> atIKlynge.any { ag.matcher(it) } }
        if (atMatchende.isEmpty() || agMatchende.isEmpty()) return null

        val matchendeIder = (atMatchende + agMatchende).mapTo(mutableSetOf()) { it.id }
        val tidligereVersjoner = erstattede.filter { gjeldendeVersjon[it.id] in matchendeIder }
        val atDeler = atMatchende + tidligereVersjoner.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }
        val agDeler = agMatchende + tidligereVersjoner.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }
        val tidligsteMatchende = (atMatchende + agMatchende).minWith(compareBy({ it.innsendtDato }, { it.id }))
        return InitiativPar(
            atStartet = atDeler.minOf { it.skjemaOpprettetDato },
            agStartet = agDeler.minOf { it.skjemaOpprettetDato },
            atInnsendt = atDeler.minOf { it.innsendtDato },
            agInnsendt = agDeler.minOf { it.innsendtDato },
            valgtEtterInnsending = tidligsteMatchende.innsendtDato,
            valgtEtterId = tidligsteMatchende.id
        )
    }

    /**
     * Grupperer deler som overlapper hverandre i tid til sammenhengende grupper (transitiv lukning av
     * overlapp), slik at ett tilfelle av dobbeltinnsending telles én gang uansett antall versjoner.
     */
    private fun overlappendeGrupper(deler: List<InnsendtSkjema>): List<List<InnsendtSkjema>> {
        val gjenstaaende = ArrayDeque(deler)
        val grupper = mutableListOf<List<InnsendtSkjema>>()
        while (gjenstaaende.isNotEmpty()) {
            val gruppe = mutableListOf(gjenstaaende.removeFirst())
            var vokste = true
            while (vokste) {
                vokste = false
                val iterator = gjenstaaende.iterator()
                while (iterator.hasNext()) {
                    val kandidat = iterator.next()
                    if (gruppe.any { it.matcher(kandidat) }) {
                        gruppe += kandidat
                        iterator.remove()
                        vokste = true
                    }
                }
            }
            grupper += gruppe
        }
        return grupper
    }

    /** Én virksomhet (juridisk enhet) i topplisten med de gjeldende delene som ligger bak tallene. */
    private data class Virksomhet(val juridiskEnhet: String, val deler: List<InnsendtSkjema>)

    /**
     * Grupperer kohorten på juridisk enhet – samme nøkkel som saksdekningen bruker, så underenheter
     * ikke splitter en sak i to rader. Sorteringen er deterministisk (antall, deretter enhet) slik at
     * rang-oppslaget i [hentVirksomhetSaksnumre] treffer samme rad som topplisten.
     */
    private fun grupperVirksomheter(kohort: List<InnsendtSkjema>): List<Virksomhet> =
        kohort.filter { !it.erstattet }
            .groupBy { it.juridiskEnhet }
            .map { (enhet, deler) -> Virksomhet(enhet, deler) }
            .sortedWith(compareByDescending<Virksomhet> { it.deler.size }.thenBy { it.juridiskEnhet })

    private fun Virksomhet.tilDto(indeks: SaksIndeks) = VirksomhetStatistikkDto(
        antallInnsendinger = deler.size.toLong(),
        antallUnikeInnsendere = deler.mapTo(mutableSetOf()) { it.innsenderFnr }.size.toLong(),
        antallArbeidstakerDel = deler.count { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }.toLong(),
        antallArbeidsgiverDel = deler.count { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }.toLong(),
        antallKomplett = deler.count { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }.toLong(),
        antallSakerMedBeggeDeler = deler.mapTo(mutableSetOf()) { it.sakNokkel() }.count { indeks.erDekket(it) }.toLong(),
        antallMottatt = deler.count { it.saksstatus == Saksstatus.MOTTATT }.toLong(),
        antallAvsluttet = deler.count { it.saksstatus == Saksstatus.AVSLUTTET }.toLong(),
        antallUkjent = deler.count { it.saksstatus == null }.toLong()
    )

    /**
     * Oppslag på hele den innsendte populasjonen. Alle egenskaps-spørsmål i statistikken stilles hit,
     * slik at et periodefilter aldri bryter et par. Indeksen har to grunnlag:
     * - gjeldende (ikke erstattede) deler for dekning og motpart-match ([harKomplett],
     *   [harMatchendeSeparateDeler], [dobbeltinnsendinger])
     * - hele populasjonen, inkl. erstattede versjoner, for versjonstellingen ([harFlereVersjoner])
     */
    private inner class SaksIndeks(populasjon: List<InnsendtSkjema>) {
        private val gjeldende = populasjon.filter { !it.erstattet }

        /** Alle versjoner per sak, også de erstattede – grunnlaget for [harFlereVersjoner]. */
        private val alleDelerPerSak = populasjon.groupBy { it.sakNokkel() }

        val komplettePerSak = gjeldende.filter { it.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }
            .groupBy { it.sakNokkel() }
        val arbeidstakerePerSak = gjeldende.filter { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }.groupBy { it.sakNokkel() }
        val arbeidsgiverePerSak = gjeldende.filter { it.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }.groupBy { it.sakNokkel() }

        /** Grupper av gjeldende deler av samme type/sak med overlappende perioder (mulige dobbeltinnsendinger). */
        val dobbeltinnsendinger: List<List<InnsendtSkjema>> =
            (arbeidstakerePerSak.values + arbeidsgiverePerSak.values)
                .flatMap { overlappendeGrupper(it) }
                .filter { it.size > 1 }

        fun harKomplett(nokkel: Pair<String, String>): Boolean = komplettePerSak.containsKey(nokkel)

        fun harMatchendeSeparateDeler(nokkel: Pair<String, String>): Boolean {
            val arbeidstakere = arbeidstakerePerSak[nokkel] ?: return false
            val arbeidsgivere = arbeidsgiverePerSak[nokkel] ?: return false
            return arbeidstakere.any { at -> arbeidsgivere.any { at.matcher(it) } }
        }

        fun erDekket(nokkel: Pair<String, String>): Boolean = harKomplett(nokkel) || harMatchendeSeparateDeler(nokkel)

        /**
         * Minst én deltype på saken er sendt i flere versjoner med overlappende periode – både
         * erstatninger og mulige dobbeltinnsendinger. Måles over hele populasjonen, inkl. erstattede.
         */
        fun harFlereVersjoner(nokkel: Pair<String, String>): Boolean =
            alleDelerPerSak[nokkel].orEmpty().groupBy { it.skjemadel }.values.any { sammeDel ->
                sammeDel.any { del -> sammeDel.any { it.id != del.id && del.matcher(it) } }
            }
    }

    private interface SakDel {
        val fnr: String
        val juridiskEnhet: String
        val periode: PeriodeDto?
    }

    private fun SakDel.sakNokkel(): Pair<String, String> = fnr to juridiskEnhet
    private fun SakDel.matcher(annen: SakDel): Boolean {
        val minPeriode = periode ?: return false
        val annenPeriode = annen.periode ?: return false
        return fnr == annen.fnr && juridiskEnhet == annen.juridiskEnhet && minPeriode.overlapper(annenPeriode)
    }

    private data class InnsendtSkjema(
        val id: UUID,
        override val fnr: String,
        val orgnr: String,
        val innsenderFnr: String,
        override val juridiskEnhet: String,
        val skjemadel: Skjemadel,
        val flyt: Representasjonstype,
        val sprak: Språk,
        override val periode: PeriodeDto?,
        /** Skjema-id-en til versjonen som erstattet denne, eller null hvis denne er gjeldende. */
        val erstattetAv: UUID?,
        val saksstatus: Saksstatus?,
        val saksnummer: String?,
        val opprettetVia: OpprettetVia?,
        /** Da skjemaet ble opprettet (utkastet startet) – brukes til å avgjøre hvem som initierte et par. */
        val skjemaOpprettetDato: Instant,
        /** Da skjemaet ble sendt inn – styrer periodefilteret. */
        val innsendtDato: Instant
    ) : SakDel {
        val erstattet: Boolean get() = erstattetAv != null
    }

    private data class UtkastSkjema(
        override val fnr: String,
        override val juridiskEnhet: String,
        val skjemadel: Skjemadel,
        override val periode: PeriodeDto?
    ) : SakDel

    private fun hentUtkastStatistikk(naa: Instant, utkast: List<UtkastSkjema>): UtkastStatistikkDto {
        val fordeling = adminStatistikkRepository.utkastAldersfordeling(
            grense1d = naa.minus(1, ChronoUnit.DAYS),
            grense7d = naa.minus(7, ChronoUnit.DAYS),
            grense30d = naa.minus(30, ChronoUnit.DAYS)
        )
        return UtkastStatistikkDto(
            antall = fordeling.totalt,
            under1Dag = fordeling.under1Dag,
            mellom1Og7Dager = fordeling.mellom1Og7Dager,
            mellom7Og30Dager = fordeling.mellom7Og30Dager,
            over30Dager = fordeling.over30Dager,
            eldsteOpprettetDato = adminStatistikkRepository.eldsteUtkastOpprettetDato(),
            perSkjemadel = Skjemadel.entries.associateWith { sd -> utkast.count { it.skjemadel == sd }.toLong() }
        )
    }

    @Transactional(readOnly = true)
    fun hentFeiledeInnsendinger(): List<InnsendingAdminDto> =
        innsendingRepository.findByStatusMedSkjema(InnsendingStatus.KAFKA_FEILET)
            .map { it.tilAdminDto() }
            .sortedByDescending { it.opprettetDato }

    @Transactional(readOnly = true)
    fun antallFeiledeInnsendinger(): Long =
        innsendingRepository.countByStatus(InnsendingStatus.KAFKA_FEILET)

    @Transactional(readOnly = true)
    fun hentInnsending(innsendingId: UUID): InnsendingAdminDto =
        finnInnsending(innsendingId).tilAdminDto()

    /**
     * Tvinger en ny prosessering (Kafka-sending) av en enkelt innsending.
     */
    fun retryInnsending(innsendingId: UUID): InnsendingAdminDto {
        val skjemaId = finnSkjemaId(innsendingId)
        log.info { "Admin: Tvinger retry av innsending $innsendingId (skjema $skjemaId)" }
        innsendingService.prosesserInnsending(skjemaId)
        return hentInnsending(innsendingId)
    }

    /**
     * Tvinger ny prosessering av alle innsendinger med status KAFKA_FEILET.
     */
    fun retryAlleFeilede(): RetryResultatDto {
        val skjemaIder = hentFeiledeSkjemaIder()
        log.info { "Admin: Tvinger retry av ${skjemaIder.size} feilede innsendinger" }

        var feilet = 0
        skjemaIder.forEach { skjemaId ->
            try {
                innsendingService.prosesserInnsending(skjemaId)
            } catch (e: Exception) {
                feilet++
                log.error(e) { "Admin: Retry feilet for skjema $skjemaId" }
            }
        }
        return RetryResultatDto(antallForsoekt = skjemaIder.size, antallFeilet = feilet)
    }

    /**
     * MELOSYS-8168 (midlertidig): Resender brukervarsel (nå med korrekt skjema-lenke) til arbeidstakere som
     * fikk et varsel med feil lenke. Kandidatene finnes i koden – ingen kuratert liste – og er bevisst litt
     * over-inkluderende (kan treffe et par ekstra), men ingen som faktisk fikk feil lenke skal utelates:
     *
     * Kandidat = handlingspliktig AG-del (arbeidsgiver/rådgiver uten fullmakt) som ble sendt inn FØR
     * [VARSEL_LENKE_FIKSET_TIDSPUNKT] og som fortsatt venter på arbeidstakers del (ingen innsendt arbeidstaker-/
     * kombinert-del matcher på samme fnr + juridisk enhet + overlappende periode). Innsendinger der saken er
     * AVSLUTTET i melosys-api ekskluderes – der er det ingenting å varsle om (motpart-delen kom typisk via
     * en annen kanal). NB: filteret forutsetter at saksstatus-massesynken fra melosys-api er kjørt først;
     * innsendinger uten synket status (null) behandles som aktive og kan få varsel.
     *
     * Selve sendingen delegeres til [ArbeidstakerVarslingService.resendVarselTilArbeidstaker], som i tillegg
     * hopper over arbeidstakere med påbegynt utkast. Sendingen skjer utenfor lese-transaksjonen (jf.
     * [retryAlleFeilede]) så vi ikke holder en DB-connection åpen gjennom Kafka-sendingen. Returnerer antall
     * sendte varsler og saksnumrene som faktisk fikk et nytt varsel (for sporbarhet på fagsiden).
     */
    fun resendVarsler(dryRun: Boolean): ResendVarslerResultatDto {
        val kandidater = finnResendKandidater()
        log.info { "Admin: Resend (dryRun=$dryRun) – fant ${kandidater.size} kandidat(er) (handlingspliktig AG-del før $VARSEL_LENKE_FIKSET_TIDSPUNKT som venter på AT-del)" }

        val sendteSaksnumre = mutableListOf<String>()
        kandidater.forEach { kandidat ->
            try {
                if (arbeidstakerVarslingService.resendVarselTilArbeidstaker(kandidat.skjemaId, dryRun)) {
                    // Saker uten saksnummer representeres med skjema-id-en, så ingen sending blir usynlig.
                    sendteSaksnumre += kandidat.saksnummer ?: kandidat.skjemaId.toString()
                }
            } catch (e: Exception) {
                log.error(e) { "Admin: Resend feilet for skjema ${kandidat.skjemaId}" }
            }
        }
        log.info { "Admin: Resend ferdig (dryRun=$dryRun) – ${sendteSaksnumre.size} varsler ${if (dryRun) "ville blitt sendt" else "sendt"}" }
        return ResendVarslerResultatDto(dryRun = dryRun, antallSendt = sendteSaksnumre.size, saksnumre = sendteSaksnumre)
    }

    /** Finner resend-kandidatene med skjema-id og saksnummer (se [resendVarsler] for kriteriene). */
    @Transactional(readOnly = true)
    fun finnResendKandidater(): List<ResendKandidat> {
        val alleInnsendte = innsendingRepository.finnAlleInnsendteMedSkjema()
        val arbeidstakerDeler = alleInnsendte.filter { erArbeidstakerDel(it) }
        val erstattedeIder: Set<UUID> = alleInnsendte
            .mapNotNull { (it.skjema.metadata as? UtsendtArbeidstakerMetadata)?.erstatterSkjemaId }
            .toSet()
        val kandidater = alleInnsendte
            .filter { innsending ->
                innsending.skjema.id !in erstattedeIder &&
                    innsending.saksstatus != Saksstatus.AVSLUTTET &&
                    erHandlingspliktigAgDel(innsending) &&
                    innsending.opprettetDato.isBefore(VARSEL_LENKE_FIKSET_TIDSPUNKT) &&
                    venterPaaArbeidstakerDel(innsending, arbeidstakerDeler)
            }
        val antallUtenStatus = kandidater.count { it.saksstatus == null }
        if (antallUtenStatus > 0) {
            log.warn {
                "Admin: Resend – $antallUtenStatus av ${kandidater.size} kandidat(er) mangler synket saksstatus " +
                    "og kan gjelde avsluttede saker. Er saksstatus-massesynken fra melosys-api kjørt?"
            }
        }
        return kandidater.map { ResendKandidat(skjemaId = it.skjema.id!!, saksnummer = it.saksnummer) }
    }

    /** Handlingspliktig AG/rådgiver-del uten fullmakt – arbeidstaker må sende inn sin egen del. */
    private fun erHandlingspliktigAgDel(innsending: Innsending): Boolean {
        val metadata = innsending.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return false
        return metadata.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL &&
            metadata.representasjonstype in HANDLINGSPLIKTIGE_REPRESENTASJONSTYPER
    }

    /** Innsendt arbeidstaker-del – enten egen del eller kombinert skjema som dekker arbeidstakers del. */
    private fun erArbeidstakerDel(innsending: Innsending): Boolean {
        val skjemadel = (innsending.skjema.metadata as? UtsendtArbeidstakerMetadata)?.skjemadel
        return skjemadel == Skjemadel.ARBEIDSTAKERS_DEL || skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
    }

    /**
     * AT-del mangler hvis ingen innsendt arbeidstaker-del matcher på samme fnr + juridisk enhet +
     * overlappende periode (samme matching som mottak/saksdekning bruker). Mangler periode på AG-delen
     * regnes som "venter" (vi sender da heller én ekstra enn å utelate noen som fikk feil lenke).
     */
    private fun venterPaaArbeidstakerDel(agDel: Innsending, arbeidstakerDeler: List<Innsending>): Boolean {
        val agMeta = agDel.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return false
        val agPeriode = agDel.skjema.utsendelsePeriode() ?: return true
        return arbeidstakerDeler.none { atDel ->
            val atMeta = atDel.skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@none false
            val atPeriode = atDel.skjema.utsendelsePeriode() ?: return@none false
            agDel.skjema.fnr == atDel.skjema.fnr &&
                agMeta.juridiskEnhetOrgnr == atMeta.juridiskEnhetOrgnr &&
                agPeriode.overlapper(atPeriode)
        }
    }

    /**
     * MIDLERTIDIG: hard-sletter alle gjenværende soft-deletede (SLETTET) utkast for GDPR-opprydding.
     *
     * Sletter både vedlegg-blobs i bucket (ingen foreldreløse filer) og selve skjema-radene
     * (DB-cascade fjerner vedlegg-/innsending-/fullmakt-rader). Blob-sletting er best-effort:
     * en feilet blob stopper ikke radslettingen, men telles og logges.
     *
     * Bevisst IKKE `@Transactional`: de eksterne bucket-kallene gjøres utenfor DB-transaksjon for å
     * unngå lange transaksjoner / lock-holdetid ved nettverkslatens. Selve DELETE-en kjøres i sin egen
     * korte transaksjon ([SkjemaRepository.slettAlleSletteSkjema]).
     *
     * Fjernes når prod er ryddet (MELOSYS-8157).
     */
    fun ryddSletteUtkast(): RyddUtkastResultatDto {
        val storageReferanser = skjemaRepository.finnVedleggStorageReferanserForSletteSkjema()

        var slettedeBlober = 0
        var feiledeBlober = 0
        storageReferanser.forEach { referanse ->
            try {
                vedleggStorageClient.slett(referanse)
                slettedeBlober++
            } catch (e: Exception) {
                feiledeBlober++
                log.error(e) { "Admin: Klarte ikke slette vedlegg-blob $referanse under opprydding av slettede utkast" }
            }
        }

        val antallSkjema = skjemaRepository.slettAlleSletteSkjema()
        log.info {
            "Admin: Ryddet $antallSkjema soft-deletede utkast " +
                "(vedlegg-blobs slettet=$slettedeBlober, feilet=$feiledeBlober)"
        }
        return RyddUtkastResultatDto(
            antallSkjema = antallSkjema,
            antallVedleggSlettet = slettedeBlober,
            antallVedleggFeilet = feiledeBlober
        )
    }

    @Transactional(readOnly = true)
    fun hentFeiledeSkjemaIder(): List<UUID> =
        innsendingRepository.findByStatusMedSkjema(InnsendingStatus.KAFKA_FEILET).map { it.skjema.id!! }

    @Transactional(readOnly = true)
    fun finnSkjemaId(innsendingId: UUID): UUID = finnInnsending(innsendingId).skjema.id!!

    private fun finnInnsending(innsendingId: UUID): Innsending =
        innsendingRepository.findById(innsendingId)
            .orElseThrow { NoSuchElementException("Fant ingen innsending med id $innsendingId") }

    private fun Innsending.tilAdminDto() = InnsendingAdminDto(
        innsendingId = id!!,
        skjemaId = skjema.id!!,
        referanseId = referanseId,
        status = status,
        skjemaStatus = skjema.status,
        orgnr = skjema.orgnr,
        antallForsok = antallForsok,
        feilmelding = feilmelding,
        sisteForsoekTidspunkt = sisteForsoekTidspunkt,
        opprettetDato = opprettetDato,
        saksnummer = saksnummer,
        saksstatus = saksstatus,
        saksstatusOppdatert = saksstatusOppdatert
    )

    companion object {
        /**
         * MELOSYS-8168: Tidspunktet skjema-lenken i det handlingspliktige varselet ble fikset. Handlingspliktige
         * AG-deler innsendt FØR dette fikk et varsel med feil lenke, og er kandidater for resend med korrekt lenke.
         */
        private val VARSEL_LENKE_FIKSET_TIDSPUNKT: Instant = Instant.parse("2026-07-03T12:10:38Z")

        /** Representasjonstyper der arbeidstaker selv må sende inn sin del (uten fullmakt). */
        private val HANDLINGSPLIKTIGE_REPRESENTASJONSTYPER = setOf(
            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.RADGIVER
        )
    }
}
