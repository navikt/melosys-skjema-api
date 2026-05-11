package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.toOsloLocalDateTime
import no.nav.melosys.skjema.extensions.toUtsendtArbeidstakerDto
import no.nav.melosys.skjema.integrasjon.pdl.PdlConsumer
import no.nav.melosys.skjema.pdf.AktørInfo
import no.nav.melosys.skjema.pdf.SkjemaPdfData
import no.nav.melosys.skjema.pdf.genererPdf
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Service
class M2MSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val vedleggService: VedleggService,
    private val skjemaDefinisjonService: SkjemaDefinisjonService,
    private val pdlConsumer: PdlConsumer
) {

    fun hentUtsendtArbeidstakerSkjemaData(id: UUID): UtsendtArbeidstakerSkjemaM2MDto {
        log.info { "Henter skjemadata for id: $id" }
        val skjema = skjemaRepository.findByIdAndStatusSendt(id)
            ?: throw NoSuchElementException("Skjema med id $id ikke funnet")

        val innsending = innsendingRepository.findBySkjemaId(skjema.id!!)
            ?: throw NoSuchElementException("Innsending for skjema med id $id ikke funnet")

        val skjemaDto = skjema.toUtsendtArbeidstakerDto()

        val vedleggListe = vedleggService.listBySkjemaId(skjema.id!!)

        return UtsendtArbeidstakerSkjemaM2MDto(
            skjema = skjemaDto,
            kobletSkjema = hentKobletSkjema(skjemaDto),
            tidligereInnsendteSkjema = hentTidligereInnsendteSkjema(skjema),
            referanseId = innsending.referanseId,
            innsendtTidspunkt = innsending.opprettetDato.toOsloLocalDateTime(),
            innsenderFnr = innsending.innsenderFnr,
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
        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: throw NoSuchElementException("Innsending for skjema med id $skjemaId ikke funnet")

        innsending.saksnummer = saksnummer
        innsendingRepository.save(innsending)
        log.info { "Registrert saksnummer $saksnummer for skjema $skjemaId" }
    }

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

        val innsending = innsendingRepository.findBySkjemaId(skjema.id!!)
            ?: throw NoSuchElementException("Innsending for skjema med id $skjemaId ikke funnet")

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

        val kobletSkjemaData = metadata.kobletSkjemaId?.let { kobletId ->
            skjemaRepository.findByIdAndStatusSendt(kobletId)?.data as? UtsendtArbeidstakerSkjemaData
        }

        val definisjon = skjemaDefinisjonService.hent(
            type = skjema.type,
            versjon = innsending.skjemaDefinisjonVersjon,
            språk = innsending.innsendtSprak
        )

        val arbeidstakerNavn = pdlConsumer.hentPerson(skjema.fnr)
            .navn.first().fulltNavn()

        val aktørInfo = AktørInfo(
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            orgnr = skjema.orgnr,
            arbeidstakerNavn = arbeidstakerNavn,
            arbeidstakerFnr = skjema.fnr
        )

        return SkjemaPdfData(
            skjemaId = skjema.id!!,
            referanseId = innsending.referanseId,
            innsendtDato = innsending.opprettetDato,
            innsendtSprak = innsending.innsendtSprak,
            aktørInfo = aktørInfo,
            skjemaData = skjema.data as UtsendtArbeidstakerSkjemaData,
            kobletSkjemaData = kobletSkjemaData,
            definisjon = definisjon
        )
    }
}
