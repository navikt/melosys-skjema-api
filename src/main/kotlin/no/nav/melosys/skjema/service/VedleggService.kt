package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.controller.dto.VedleggResponse
import no.nav.melosys.skjema.entity.Vedlegg
import no.nav.melosys.skjema.integrasjon.clamav.ClamAvClient
import no.nav.melosys.skjema.integrasjon.storage.VedleggStorageClient
import no.nav.melosys.skjema.repository.VedleggRepository
import no.nav.melosys.skjema.service.vedlegg.FilValidator
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger { }

@Service
class VedleggService(
    private val utsendtArbeidstakerService: UtsendtArbeidstakerService,
    private val vedleggRepository: VedleggRepository,
    private val filValidator: FilValidator,
    private val clamAvClient: ClamAvClient,
    private val vedleggStorageClient: VedleggStorageClient
) {
    companion object {
        const val MAKS_ANTALL_VEDLEGG = 10
    }

    @Transactional
    fun lastOpp(skjemaId: UUID, fil: MultipartFile): VedleggResponse {
        val skjema = utsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId)

        require(skjema.status == SkjemaStatus.UTKAST) {
            "Kan kun laste opp vedlegg til skjemaer med status UTKAST"
        }

        val antallEksisterende = vedleggRepository.countBySkjemaId(skjemaId)
        require(antallEksisterende < MAKS_ANTALL_VEDLEGG) {
            "Maks antall vedlegg ($MAKS_ANTALL_VEDLEGG) er nådd"
        }

        filValidator.valider(fil)
        val filtype = filValidator.detekterFiltype(fil)

        clamAvClient.scan(fil)

        val sanitisertFilnavn = filValidator.sanitizeFilnavn(fil.originalFilename ?: "vedlegg")
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

        return VedleggResponse.fra(vedlegg)
    }

    fun list(skjemaId: UUID): List<VedleggResponse> {
        utsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId)
        return vedleggRepository.findBySkjemaId(skjemaId).map { VedleggResponse.fra(it) }
    }

    @Transactional
    fun slett(skjemaId: UUID, vedleggId: UUID) {
        val skjema = utsendtArbeidstakerService.hentSkjemaMedTilgangsstyring(skjemaId)

        require(skjema.status == SkjemaStatus.UTKAST) {
            "Kan kun slette vedlegg fra skjemaer med status UTKAST"
        }

        val vedlegg = vedleggRepository.findByIdAndSkjemaId(vedleggId, skjemaId)
            ?: throw NoSuchElementException("Vedlegg med id $vedleggId ikke funnet for skjema $skjemaId")

        vedleggStorageClient.slett(vedlegg.storageReferanse)
        vedleggRepository.delete(vedlegg)

        log.info { "Vedlegg slettet: $vedleggId for skjema $skjemaId" }
    }
}
