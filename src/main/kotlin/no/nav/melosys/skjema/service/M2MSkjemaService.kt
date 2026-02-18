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
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class M2MSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val skjemaDefinisjonService: SkjemaDefinisjonService
) {

    fun hentUtsendtArbeidstakerSkjemaData(id: UUID): UtsendtArbeidstakerSkjemaM2MDto {
        log.info { "Henter skjemadata for id: $id" }
        val skjema = skjemaRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("Skjema med id $id ikke funnet")

        val innsending = innsendingRepository.findBySkjemaId(skjema.id!!)
            ?: throw NoSuchElementException("Innsending for skjema med id $id ikke funnet")

        val skjemaDto = skjema.toUtsendtArbeidstakerDto()

        return UtsendtArbeidstakerSkjemaM2MDto(
            skjema = skjemaDto,
            relaterteSkjemaer = hentKobledeSkjemaer(skjemaDto),
            referanseId = innsending.referanseId,
            innsendtTidspunkt = innsending.opprettetDato.toOsloLocalDateTime(),
            innsenderFnr = innsending.innsenderFnr
        )
    }

    private tailrec fun hentKobledeSkjemaer(
        skjemaDto: UtsendtArbeidstakerSkjemaDto,
        akkumulator: List<UtsendtArbeidstakerSkjemaDto> = emptyList()
    ): List<UtsendtArbeidstakerSkjemaDto> {
        val kobletSkjemaId = skjemaDto.metadata.kobletSkjemaId ?: return akkumulator

        val kobletDto = skjemaRepository.findByIdOrNull(kobletSkjemaId)?.toUtsendtArbeidstakerDto() ?: run {
            log.warn { "Koblet skjema $kobletSkjemaId ikke funnet for skjema ${skjemaDto.id}" }
            return akkumulator
        }

        return hentKobledeSkjemaer(kobletDto, akkumulator + kobletDto)
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
