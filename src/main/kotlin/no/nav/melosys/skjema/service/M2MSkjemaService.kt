package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.extensions.parseArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseUtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.Skjemadel
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

        val metadata = jsonMapper.parseUtsendtArbeidstakerMetadata(skjema.metadata)
        val erArbeidstakersDel = metadata.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL

        // Bygg hovedskjemaets data med metadata
        val skjemaData = skjema.data!!
        val arbeidstakersDeler = mutableListOf<no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDataDto>()
        val arbeidsgiversDeler = mutableListOf<no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDataDto>()

        if (erArbeidstakersDel) {
            val data = jsonMapper.parseArbeidstakersSkjemaDataDto(skjemaData)
            arbeidstakersDeler.add(data.copy(
                skjemaId = skjema.id,
                innsendtDato = skjema.endretDato,
                erstatterSkjemaId = metadata.erstatterSkjemaId
            ))
        } else {
            val data = jsonMapper.parseArbeidsgiversSkjemaDataDto(skjemaData)
            arbeidsgiversDeler.add(data.copy(
                skjemaId = skjema.id,
                innsendtDato = skjema.endretDato,
                erstatterSkjemaId = metadata.erstatterSkjemaId
            ))
        }

        // Hent koblet motpart via kobletSkjemaId
        val kobletSkjemaId = metadata.kobletSkjemaId
        if (kobletSkjemaId != null) {
            val kobletSkjema = skjemaRepository.findByIdOrNull(kobletSkjemaId)
            if (kobletSkjema != null) {
                val kobletMetadata = jsonMapper.parseUtsendtArbeidstakerMetadata(kobletSkjema.metadata)
                val kobletData = kobletSkjema.data!!

                if (erArbeidstakersDel) {
                    // Motpart er arbeidsgiver
                    val data = jsonMapper.parseArbeidsgiversSkjemaDataDto(kobletData)
                    arbeidsgiversDeler.add(data.copy(
                        skjemaId = kobletSkjema.id,
                        innsendtDato = kobletSkjema.endretDato,
                        erstatterSkjemaId = kobletMetadata.erstatterSkjemaId
                    ))
                } else {
                    // Motpart er arbeidstaker
                    val data = jsonMapper.parseArbeidstakersSkjemaDataDto(kobletData)
                    arbeidstakersDeler.add(data.copy(
                        skjemaId = kobletSkjema.id,
                        innsendtDato = kobletSkjema.endretDato,
                        erstatterSkjemaId = kobletMetadata.erstatterSkjemaId
                    ))
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
}
