package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.toOsloLocalDateTime
import no.nav.melosys.skjema.extensions.toUtsendtArbeidstakerDto
import no.nav.melosys.skjema.integrasjon.pdl.PdlClient
import no.nav.melosys.skjema.pdf.AktørInfo
import no.nav.melosys.skjema.pdf.FullmektigInfo
import no.nav.melosys.skjema.pdf.RadgiverInfo
import no.nav.melosys.skjema.pdf.SkjemaPdfData
import no.nav.melosys.skjema.pdf.genererPdf
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerDokumentTittel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.m2m.BulkOppdaterSaksstatusResultat
import no.nav.melosys.skjema.types.m2m.SaksstatusOppdatering
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.common.Saksstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Service
class M2MSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val vedleggService: VedleggService,
    private val skjemaDefinisjonService: SkjemaDefinisjonService,
    private val pdlClient: PdlClient
) {

    fun hentUtsendtArbeidstakerSkjemaData(id: UUID): UtsendtArbeidstakerSkjemaM2MDto {
        log.info { "Henter skjemadata for id: $id" }
        val skjema = skjemaRepository.findByIdAndStatusSendt(id)
            ?: throw NoSuchElementException("Skjema med id $id ikke funnet")

        val innsending = hentInnsending(skjema.id!!)

        val skjemaDto = skjema.toUtsendtArbeidstakerDto()

        val vedleggListe = vedleggService.listBySkjemaId(skjema.id!!)

        return UtsendtArbeidstakerSkjemaM2MDto(
            skjema = skjemaDto,
            kobletSkjema = hentKobletSkjema(skjemaDto),
            tidligereInnsendteSkjema = hentTidligereInnsendteSkjema(skjema),
            referanseId = innsending.referanseId,
            innsendtTidspunkt = innsending.opprettetDato.toOsloLocalDateTime(),
            innsenderFnr = innsending.innsenderFnr,
            dokumentTittel = UtsendtArbeidstakerDokumentTittel.utled(
                skjema.data as UtsendtArbeidstakerSkjemaData,
                innsending.innsendtSprak
            ),
            vedlegg = vedleggListe
        )
    }

    private fun hentTidligereInnsendteSkjema(skjema: Skjema): List<UtsendtArbeidstakerSkjemaDto> {
        val tidligere = mutableListOf<UtsendtArbeidstakerSkjemaDto>()
        var metadata = skjema.metadata as? UtsendtArbeidstakerMetadata ?: return emptyList()
        val besøkt = mutableSetOf(skjema.id!!)

        while (metadata.erstatterSkjemaId != null) {
            if (!besøkt.add(metadata.erstatterSkjemaId!!)) {
                log.warn { "Sirkulær erstatter-referanse oppdaget ved skjema ${metadata.erstatterSkjemaId}" }
                break
            }
            val forrige = skjemaRepository.findByIdAndStatusSendt(metadata.erstatterSkjemaId!!) ?: break
            tidligere.add(forrige.toUtsendtArbeidstakerDto())
            metadata = forrige.metadata as? UtsendtArbeidstakerMetadata ?: break
            if (tidligere.size >= 50) break
        }
        return tidligere
    }

    private fun hentKobletSkjema(skjemaDto: UtsendtArbeidstakerSkjemaDto): UtsendtArbeidstakerSkjemaDto? {
        val kobletSkjemaId = skjemaDto.metadata.kobletSkjemaId ?: return null

        return skjemaRepository.findByIdAndStatusSendt(kobletSkjemaId)?.toUtsendtArbeidstakerDto() ?: run {
            log.warn { "Koblet skjema $kobletSkjemaId ikke funnet for skjema ${skjemaDto.id}" }
            null
        }
    }

    @Transactional
    fun registrerSaksnummer(skjemaId: UUID, saksnummer: String) {
        val innsending = hentInnsending(skjemaId)
        innsending.saksnummer = saksnummer

        // Kobles innsendingen til en sak som allerede har synket status, arves den – ellers
        // ville den stått som MOTTATT (null) mens søsknene viser noe annet, frem til neste synk.
        innsendingRepository.findBySaksnummer(saksnummer)
            .firstOrNull { it.id != innsending.id && it.saksstatus != null }
            ?.let {
                innsending.saksstatus = it.saksstatus
                innsending.saksstatusOppdatert = it.saksstatusOppdatert
            }
        log.info { "Registrert saksnummer $saksnummer for skjema $skjemaId" }
    }

    /**
     * Oppdaterer saksstatus for innsendingen med gitt skjema-id, og for alle andre innsendinger
     * på samme saksnummer (motpart-deler og nye versjoner skal vise samme status).
     * Setter også saksnummer på innsendingen hvis det mangler (backfill for historiske innsendinger).
     */
    @Transactional
    fun oppdaterSaksstatus(skjemaId: UUID, saksnummer: String, saksstatus: Saksstatus) {
        val innsending = hentInnsending(skjemaId)
        val oppdaterte = oppdaterSaksstatusForInnsending(innsending, saksnummer, saksstatus)
        log.info { "Oppdatert saksstatus til $saksstatus for sak ${innsending.saksnummer} (${oppdaterte.size} innsending(er))" }
    }

    /**
     * Massesynk av saksstatus fra melosys-api. Ukjente skjema-id-er rapporteres i stedet for å
     * feile. Hele batchen kjører i én transaksjon: én uventet feil ruller tilbake alt, og
     * melosys-api reprøver hele batchen (operasjonen er idempotent).
     */
    @Transactional
    fun bulkOppdaterSaksstatus(oppdateringer: List<SaksstatusOppdatering>): BulkOppdaterSaksstatusResultat {
        val ukjenteSkjemaIder = mutableListOf<UUID>()
        val oppdaterteInnsendingIder = mutableSetOf<UUID>()

        oppdateringer.forEach { oppdatering ->
            val innsending = innsendingRepository.findBySkjemaId(oppdatering.skjemaId)
            if (innsending == null) {
                ukjenteSkjemaIder += oppdatering.skjemaId
                return@forEach
            }
            // Rader som deler saksnummer oppdaterer hverandre – hopp over de som allerede er dekket
            if (innsending.id in oppdaterteInnsendingIder && innsending.saksstatus == oppdatering.saksstatus) {
                return@forEach
            }
            oppdaterSaksstatusForInnsending(innsending, oppdatering.saksnummer, oppdatering.saksstatus)
                .forEach { oppdaterteInnsendingIder += it.id!! }
        }

        log.info {
            "Bulk-oppdatert saksstatus: ${oppdateringer.size} rader, ${oppdaterteInnsendingIder.size} innsending(er) oppdatert, " +
                "${ukjenteSkjemaIder.size} ukjente skjema-id-er"
        }
        return BulkOppdaterSaksstatusResultat(
            antallOppdatert = oppdaterteInnsendingIder.size,
            ukjenteSkjemaIder = ukjenteSkjemaIder
        )
    }

    /**
     * Setter saksnummer hvis det mangler, og oppdaterer saksstatus på alle innsendinger som hører
     * til innsendingens AVSTEMTE saksnummer. Ved avvik beholdes eksisterende saksnummer – en
     * statusoppdatering skal ikke overskrive en etablert sakskobling, og skal heller ikke røre
     * innsendinger på det oppgitte (avvikende) saksnummeret.
     *
     * NB: innsendinger på samme sak som selv mangler saksnummer fanges ikke av sweepen – de
     * dekkes av massesynken fra melosys-api, som sender oppdatering per skjemaId.
     */
    private fun oppdaterSaksstatusForInnsending(innsending: Innsending, saksnummer: String, saksstatus: Saksstatus): List<Innsending> {
        if (innsending.saksnummer == null) {
            innsending.saksnummer = saksnummer
        } else if (innsending.saksnummer != saksnummer) {
            log.warn {
                "Saksstatus-oppdatering for skjema ${innsending.skjema.id} oppga saksnummer $saksnummer, " +
                    "men innsendingen har allerede saksnummer ${innsending.saksnummer} - beholder eksisterende"
            }
        }

        val gjeldendeSaksnummer = innsending.saksnummer!!
        val oppdatertTidspunkt = Instant.now()
        val skalOppdateres = (innsendingRepository.findBySaksnummer(gjeldendeSaksnummer) + innsending).distinctBy { it.id }
        skalOppdateres.forEach {
            it.saksstatus = saksstatus
            it.saksstatusOppdatert = oppdatertTidspunkt
        }
        return skalOppdateres
    }

    private fun hentInnsending(skjemaId: UUID): Innsending =
        innsendingRepository.findBySkjemaId(skjemaId)
            ?: throw NoSuchElementException("Innsending for skjema med id $skjemaId ikke funnet")

    fun hentVedleggInnhold(skjemaId: UUID, vedleggId: UUID): VedleggInnhold {
        log.info { "M2M: Henter vedlegg $vedleggId for skjema $skjemaId" }
        skjemaRepository.findByIdAndStatusSendt(skjemaId)
            ?: throw NoSuchElementException("Skjema med id $skjemaId ikke funnet")
        return vedleggService.hentInnhold(skjemaId, vedleggId)
    }

    fun hentPdfForSkjema(skjemaId: UUID): ByteArray {
        log.info { "Genererer PDF for skjema med id: $skjemaId" }
        val skjema = skjemaRepository.findByIdAndStatusSendt(skjemaId)
            ?: throw NoSuchElementException("Skjema med id $skjemaId ikke funnet")

        val innsending = hentInnsending(skjema.id!!)

        return when (skjema.type) {
            SkjemaType.UTSENDT_ARBEIDSTAKER -> {
                genererPdfForUtsendtArbeidstaker(skjema, innsending)
            }
        }
    }

    private fun genererPdfForUtsendtArbeidstaker(skjema: Skjema, innsending: Innsending): ByteArray {
        val skjemaPdfData = byggSkjemaPdfData(skjema, innsending)
        return genererPdf(skjemaPdfData)
    }

    private fun byggSkjemaPdfData(skjema: Skjema, innsending: Innsending): SkjemaPdfData {
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata

        val definisjon = skjemaDefinisjonService.hent(
            type = skjema.type,
            versjon = innsending.skjemaDefinisjonVersjon,
            språk = innsending.innsendtSprak
        )

        val arbeidstakerNavn = pdlClient.hentPerson(skjema.fnr).hentFulltNavn()

        val aktørInfo = AktørInfo(
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            orgnr = skjema.orgnr,
            arbeidstakerNavn = arbeidstakerNavn,
            arbeidstakerFnr = skjema.fnr,
            kontaktpersonNavn = hentKontaktpersonNavn(metadata, innsending)
        )

        val fullmektigInfo = hentFullmektigInfo(metadata)
        val radgiverInfo = hentRadgiverInfo(metadata, innsending)

        return SkjemaPdfData(
            skjemaId = skjema.id!!,
            referanseId = innsending.referanseId,
            innsendtDato = innsending.opprettetDato,
            innsendtSprak = innsending.innsendtSprak,
            aktørInfo = aktørInfo,
            fullmektigInfo = fullmektigInfo,
            radgiverInfo = radgiverInfo,
            skjemaData = skjema.data as UtsendtArbeidstakerSkjemaData,
            vedlegg = vedleggService.listBySkjemaId(skjema.id!!),
            definisjon = definisjon
        )
    }

    private fun hentFullmektigInfo(metadata: UtsendtArbeidstakerMetadata): FullmektigInfo? {
        val fullmektigFnr = when (metadata) {
            is AnnenPersonMetadata -> metadata.fullmektigFnr
            is ArbeidsgiverMedFullmaktMetadata -> metadata.fullmektigFnr
            is RadgiverMedFullmaktMetadata -> metadata.fullmektigFnr
            else -> null
        } ?: return null

        val fullmektig = pdlClient.hentPerson(fullmektigFnr)
        return FullmektigInfo(navn = fullmektig.hentFulltNavn(), foedselsdato = fullmektig.hentFoedselsdato())
    }

    private fun hentKontaktpersonNavn(metadata: UtsendtArbeidstakerMetadata, innsending: Innsending): String? {
        return when (metadata) {
            is ArbeidsgiverMetadata, is ArbeidsgiverMedFullmaktMetadata ->
                pdlClient.hentPerson(innsending.innsenderFnr).hentFulltNavn()
            else -> null
        }
    }

    private fun hentRadgiverInfo(metadata: UtsendtArbeidstakerMetadata, innsending: Innsending): RadgiverInfo? {
        val radgiverfirma = when (metadata) {
            is RadgiverMetadata -> metadata.radgiverfirma
            is RadgiverMedFullmaktMetadata -> metadata.radgiverfirma
            else -> null
        } ?: return null

        val personNavn = pdlClient.hentPerson(innsending.innsenderFnr).hentFulltNavn()
        return RadgiverInfo(
            firmaNavn = radgiverfirma.navn,
            personNavn = personNavn
        )
    }
}
