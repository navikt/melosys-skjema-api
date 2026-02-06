package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.parseArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDto
import no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger { }

@Service
class M2MSkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val jsonMapper: JsonMapper
) {

    fun hentUtsendtArbeidstakerSkjemaData(id: UUID): UtsendtArbeidstakerM2MSkjemaData {
        log.info { "Henter skjemadata for id: $id" }
        val skjema = skjemaRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("Skjema med id $id ikke funnet")

        val innsending = innsendingRepository.findBySkjemaId(skjema.id!!)
            ?: throw NoSuchElementException("Innsending for skjema med id $id ikke funnet")

        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        val erArbeidstakersDel = metadata.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL

        // Bygg hovedskjemaets data med metadata
        val arbeidstakersDeler = mutableListOf<ArbeidstakersSkjemaDto>()
        val arbeidsgiversDeler = mutableListOf<ArbeidsgiversSkjemaDto>()

        if (erArbeidstakersDel) {
            arbeidstakersDeler.add(byggArbeidstakersDto(skjema, metadata))
        } else {
            arbeidsgiversDeler.add(byggArbeidsgiversDto(skjema, metadata))
        }

        // Hent koblet motpart via kobletSkjemaId
        val kobletSkjemaId = metadata.kobletSkjemaId
        if (kobletSkjemaId != null) {
            val kobletSkjema = skjemaRepository.findByIdOrNull(kobletSkjemaId)
            if (kobletSkjema != null) {
                val kobletMetadata = kobletSkjema.metadata as UtsendtArbeidstakerMetadata

                if (erArbeidstakersDel) {
                    arbeidsgiversDeler.add(byggArbeidsgiversDto(kobletSkjema, kobletMetadata))
                } else {
                    arbeidstakersDeler.add(byggArbeidstakersDto(kobletSkjema, kobletMetadata))
                }
            } else {
                log.warn { "Koblet skjema $kobletSkjemaId ikke funnet for skjema $id" }
            }
        }

        return UtsendtArbeidstakerM2MSkjemaData(
            arbeidstakersDeler = arbeidstakersDeler,
            arbeidsgiversDeler = arbeidsgiversDeler,
            referanseId = innsending.referanseId
        )
    }

    private fun byggArbeidstakersDto(skjema: Skjema, metadata: UtsendtArbeidstakerMetadata): ArbeidstakersSkjemaDto {
        val data = jsonMapper.parseArbeidstakersSkjemaDataDto(skjema.data!!)
        return ArbeidstakersSkjemaDto(
            id = skjema.id!!,
            fnr = skjema.fnr,
            status = skjema.status,
            innsendtDato = skjema.endretDato,
            erstatterSkjemaId = metadata.erstatterSkjemaId,
            data = data
        )
    }

    private fun byggArbeidsgiversDto(skjema: Skjema, metadata: UtsendtArbeidstakerMetadata): ArbeidsgiversSkjemaDto {
        val data = jsonMapper.parseArbeidsgiversSkjemaDataDto(skjema.data!!)
        return ArbeidsgiversSkjemaDto(
            id = skjema.id!!,
            orgnr = skjema.orgnr,
            status = skjema.status,
            innsendtDato = skjema.endretDato,
            erstatterSkjemaId = metadata.erstatterSkjemaId,
            data = data
        )
    }
}
