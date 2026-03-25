package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.toOsloLocalDateTime
import no.nav.melosys.skjema.extensions.toUtsendtArbeidstakerDto
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

private val log = KotlinLogging.logger { }

@Service
class M2MSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val vedleggService: VedleggService,
    private val skjemaDefinisjonService: SkjemaDefinisjonService
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
            tidligereInnsendteSkjema = emptyList(),
            referanseId = innsending.referanseId,
            innsendtTidspunkt = innsending.opprettetDato.toOsloLocalDateTime(),
            innsenderFnr = innsending.innsenderFnr,
            vedlegg = vedleggListe
        )
    }

    private fun hentKobletSkjema(skjemaDto: UtsendtArbeidstakerSkjemaDto): UtsendtArbeidstakerSkjemaDto? {
        val kobletSkjemaId = skjemaDto.metadata.kobletSkjemaId ?: return null

        return skjemaRepository.findByIdAndStatusSendt(kobletSkjemaId)?.toUtsendtArbeidstakerDto() ?: run {
            log.warn { "Koblet skjema $kobletSkjemaId ikke funnet for skjema ${skjemaDto.id}" }
            null
        }
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

        return SkjemaPdfData(
            skjemaId = skjema.id!!,
            referanseId = innsending.referanseId,
            innsendtDato = innsending.opprettetDato,
            innsendtSprak = innsending.innsendtSprak,
            skjemaData = skjema.data as UtsendtArbeidstakerSkjemaData,
            kobletSkjemaData = kobletSkjemaData,
            definisjon = definisjon
        )
    }
}
