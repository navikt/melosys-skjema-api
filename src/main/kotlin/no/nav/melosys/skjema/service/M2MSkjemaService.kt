package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.ZoneId
import java.util.UUID
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.pdf.SkjemaPdfData
import no.nav.melosys.skjema.pdf.genererPdf
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class M2MSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val skjemaDefinisjonService: SkjemaDefinisjonService
) {

    fun hentUtsendtArbeidstakerSkjemaData(id: UUID): UtsendtArbeidstakerM2MSkjemaData {
        log.info { "Henter skjemadata for id: $id" }
        val skjema = skjemaRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("Skjema med id $id ikke funnet")

        val innsending = innsendingRepository.findBySkjemaId(skjema.id!!)
            ?: throw NoSuchElementException("Innsending for skjema med id $id ikke funnet")

        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata

        // Bygg hovedskjemaets data
        val skjemaer = mutableListOf<UtsendtArbeidstakerSkjemaDto>()
        skjemaer.add(byggSkjemaDto(skjema, metadata))

        // Hent koblet motpart via kobletSkjemaId
        val kobletSkjemaId = metadata.kobletSkjemaId
        if (kobletSkjemaId != null) {
            val kobletSkjema = skjemaRepository.findByIdOrNull(kobletSkjemaId)
            if (kobletSkjema != null) {
                val kobletMetadata = kobletSkjema.metadata as UtsendtArbeidstakerMetadata
                skjemaer.add(byggSkjemaDto(kobletSkjema, kobletMetadata))
            } else {
                log.warn { "Koblet skjema $kobletSkjemaId ikke funnet for skjema $id" }
            }
        }

        return UtsendtArbeidstakerM2MSkjemaData(
            skjemaer = skjemaer,
            referanseId = innsending.referanseId,
            innsendtTidspunkt = innsending.opprettetDato.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
            innsenderFnr = innsending.innsenderFnr
        )
    }

    private fun byggSkjemaDto(skjema: Skjema, metadata: UtsendtArbeidstakerMetadata): UtsendtArbeidstakerSkjemaDto {
        val data: UtsendtArbeidstakerSkjemaData = when (metadata.skjemadel) {
            Skjemadel.ARBEIDSTAKERS_DEL -> skjema.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto ?: UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            Skjemadel.ARBEIDSGIVERS_DEL -> skjema.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ?: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        }

        return UtsendtArbeidstakerSkjemaDto(
            id = skjema.id!!,
            status = skjema.status,
            type = skjema.type,
            fnr = skjema.fnr,
            orgnr = skjema.orgnr,
            metadata = metadata,
            data = data
        )
    }

    fun hentPdfForSkjema(skjemaId: UUID): ByteArray {
        log.info { "Genererer PDF for skjema med id: $skjemaId" }
        val skjema = skjemaRepository.findByIdOrNull(skjemaId)
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
        val (arbeidstakerData, arbeidsgiverData) = hentArbeidstakerOgArbeidsgiverData(skjema, metadata)

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
            arbeidstakerData = arbeidstakerData,
            arbeidsgiverData = arbeidsgiverData,
            definisjon = definisjon
        )
    }

    private fun hentArbeidstakerOgArbeidsgiverData(
        skjema: Skjema,
        metadata: UtsendtArbeidstakerMetadata
    ): Pair<UtsendtArbeidstakerArbeidstakersSkjemaDataDto?, UtsendtArbeidstakerArbeidsgiversSkjemaDataDto?> {
        var arbeidstakerData: UtsendtArbeidstakerArbeidstakersSkjemaDataDto? = null
        var arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto? = null

        // Data fra hovedskjema
        when (metadata.skjemadel) {
            Skjemadel.ARBEIDSTAKERS_DEL -> arbeidstakerData = skjema.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto
            Skjemadel.ARBEIDSGIVERS_DEL -> arbeidsgiverData = skjema.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
        }

        // Data fra koblet skjema
        metadata.kobletSkjemaId?.let { kobletId ->
            skjemaRepository.findByIdOrNull(kobletId)?.let { kobletSkjema ->
                val kobletMetadata = kobletSkjema.metadata as UtsendtArbeidstakerMetadata
                when (kobletMetadata.skjemadel) {
                    Skjemadel.ARBEIDSTAKERS_DEL -> arbeidstakerData = kobletSkjema.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto
                    Skjemadel.ARBEIDSGIVERS_DEL -> arbeidsgiverData = kobletSkjema.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
                }
            }
        }

        return Pair(arbeidstakerData, arbeidsgiverData)
    }
}
