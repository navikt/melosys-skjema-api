package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Vedlegg
import no.nav.melosys.skjema.exception.SkjemaErIkkeRedigerbartException
import no.nav.melosys.skjema.extensions.toVedleggDto
import no.nav.melosys.skjema.integrasjon.clamav.ClamAvClient
import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import no.nav.melosys.skjema.integrasjon.storage.VedleggStorageClient
import no.nav.melosys.skjema.repository.VedleggRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.vedlegg.FilValidator
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.vedlegg.VedleggDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger { }

@Service
class VedleggService(
    private val utsendtArbeidstakerService: UtsendtArbeidstakerService,
    private val vedleggRepository: VedleggRepository,
    private val clamAvClient: ClamAvClient,
    private val vedleggStorageClient: VedleggStorageClient
) {
    companion object {
        const val MAKS_ANTALL_VEDLEGG = 10
    }

    @Transactional
    fun lastOpp(skjemaId: UUID, fil: MultipartFile): VedleggDto {
        val skjema = utsendtArbeidstakerService.hentSkjemaMedLesetilgang(skjemaId)

        if (skjema.status != SkjemaStatus.UTKAST) {
            throw SkjemaErIkkeRedigerbartException()
        }

        val antallEksisterende = vedleggRepository.countBySkjemaId(skjemaId)
        require(antallEksisterende < MAKS_ANTALL_VEDLEGG) {
            "Maks antall vedlegg ($MAKS_ANTALL_VEDLEGG) er nådd"
        }

        val filtype = FilValidator.validerOgDetekterFiltype(fil)

        clamAvClient.scan(fil)

        val sanitisertFilnavn = FilValidator.sanitizeFilnavn(fil.originalFilename ?: "vedlegg")
        val vedleggId = UUID.randomUUID()
        val storageReferanse = "skjemaer/$skjemaId/vedlegg/$vedleggId/$sanitisertFilnavn"

        vedleggStorageClient.lastOpp(storageReferanse, fil.bytes, fil.contentType ?: "application/octet-stream")

        val vedlegg = vedleggRepository.save(
            Vedlegg(
                id = vedleggId,
                skjema = skjema,
                filnavn = sanitisertFilnavn,
                originalFilnavn = fil.originalFilename ?: "vedlegg",
                filtype = filtype,
                filstorrelse = fil.size,
                storageReferanse = storageReferanse,
                opprettetAv = SubjectHandler.getInstance().getUserID()
            )
        )

        log.info { "Vedlegg lastet opp: ${vedlegg.id} for skjema $skjemaId" }

        return vedlegg.toVedleggDto()
    }

    fun list(skjemaId: UUID): List<VedleggDto> {
        utsendtArbeidstakerService.hentSkjemaMedLesetilgang(skjemaId)
        return listBySkjemaId(skjemaId)
    }

    fun listBySkjemaId(skjemaId: UUID): List<VedleggDto> {
        return vedleggRepository.findBySkjemaId(skjemaId).map { it.toVedleggDto() }
    }

    fun hent(skjemaId: UUID, vedleggId: UUID): VedleggInnhold {
        utsendtArbeidstakerService.hentSkjemaMedLesetilgang(skjemaId)
        return hentInnhold(skjemaId, vedleggId)
    }

    fun hentInnhold(skjemaId: UUID, vedleggId: UUID): VedleggInnhold {
        val vedlegg = vedleggRepository.findByIdAndSkjemaId(vedleggId, skjemaId)
            ?: throw NoSuchElementException("Vedlegg med id $vedleggId ikke funnet for skjema $skjemaId")

        val data = vedleggStorageClient.hent(vedlegg.storageReferanse)
        return VedleggInnhold(
            data = data,
            filnavn = vedlegg.originalFilnavn,
            contentType = vedlegg.filtype.toContentType()
        )
    }

    @Transactional
    fun slett(skjemaId: UUID, vedleggId: UUID) {
        val skjema = utsendtArbeidstakerService.hentSkjemaMedLesetilgang(skjemaId)

        if (skjema.status != SkjemaStatus.UTKAST) {
            throw SkjemaErIkkeRedigerbartException()
        }

        val vedlegg = vedleggRepository.findByIdAndSkjemaId(vedleggId, skjemaId)
            ?: throw NoSuchElementException("Vedlegg med id $vedleggId ikke funnet for skjema $skjemaId")

        vedleggStorageClient.slett(vedlegg.storageReferanse)
        vedleggRepository.delete(vedlegg)

        log.info { "Vedlegg slettet: $vedleggId for skjema $skjemaId" }
    }
}

class VedleggInnhold(
    val data: ByteArray,
    val filnavn: String,
    val contentType: String
)

private fun VedleggFiltype.toContentType(): String = when (this) {
    VedleggFiltype.PDF -> "application/pdf"
    VedleggFiltype.JPEG -> "image/jpeg"
    VedleggFiltype.PNG -> "image/png"
}
