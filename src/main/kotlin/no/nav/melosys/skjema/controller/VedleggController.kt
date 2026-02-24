package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.melosys.skjema.service.VedleggService
import no.nav.melosys.skjema.types.vedlegg.VedleggDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/skjema/{skjemaId}/vedlegg")
@Tag(name = "Vedlegg", description = "Endepunkter for opplasting og håndtering av vedlegg")
@Protected
class VedleggController(
    private val vedleggService: VedleggService
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun lastOppVedlegg(
        @PathVariable skjemaId: UUID,
        @RequestPart("fil") fil: MultipartFile
    ): VedleggDto {
        return vedleggService.lastOpp(skjemaId, fil)
    }

    @GetMapping
    fun hentVedlegg(@PathVariable skjemaId: UUID): List<VedleggDto> {
        return vedleggService.list(skjemaId)
    }

    @GetMapping("/{vedleggId}/innhold")
    fun hentVedleggInnhold(
        @PathVariable skjemaId: UUID,
        @PathVariable vedleggId: UUID
    ): ResponseEntity<ByteArray> {
        val innhold = vedleggService.hent(skjemaId, vedleggId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(innhold.contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(innhold.filnavn).build().toString()
            )
            .body(innhold.data)
    }

    @DeleteMapping("/{vedleggId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun slettVedlegg(
        @PathVariable skjemaId: UUID,
        @PathVariable vedleggId: UUID
    ) {
        vedleggService.slett(skjemaId, vedleggId)
    }
}
