package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.controller.*
import no.nav.melosys.skjema.domain.Skjema
import no.nav.melosys.skjema.domain.SkjemaStatus
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger { }

private enum class DataType {
    ARBEIDSGIVER,
    ARBEIDSTAKER
}

@Service
class SkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val objectMapper: ObjectMapper,
    private val subjectHandler: SubjectHandler
) {

    fun createSkjema(fnr: String, orgnr: String): Skjema {
        val currentUser = subjectHandler.getUserID() 
            ?: throw IllegalStateException("Unable to determine current user")
        val skjema = Skjema(
            status = SkjemaStatus.UTKAST,
            fnr = fnr,
            orgnr = orgnr,
            opprettetAv = currentUser,
            endretAv = currentUser
        )
        return skjemaRepository.save(skjema)
    }

    fun getSkjema(id: UUID): Skjema? {
        return skjemaRepository.findById(id).orElse(null)
    }

    private fun updateJsonData(id: UUID, data: Any, dataType: DataType): Skjema {
        val skjema = skjemaRepository.findById(id).orElseThrow {
            IllegalArgumentException("Skjema with id $id not found")
        }
        
        // Get current JSON data based on type
        val currentJsonData = when (dataType) {
            DataType.ARBEIDSGIVER -> skjema.arbeidsgiverData
            DataType.ARBEIDSTAKER -> skjema.arbeidstakerData
        }
        
        // Read existing JSON or create empty object
        val existingData = if (!currentJsonData.isNullOrBlank()) {
            objectMapper.readTree(currentJsonData) as ObjectNode
        } else {
            objectMapper.createObjectNode()
        }
        
        // Merge new data
        val newData = objectMapper.valueToTree<JsonNode>(data) as ObjectNode
        existingData.setAll<JsonNode>(newData)
        
        // Update the appropriate field
        val mergedJson = objectMapper.writeValueAsString(existingData)
        when (dataType) {
            DataType.ARBEIDSGIVER -> skjema.arbeidsgiverData = mergedJson
            DataType.ARBEIDSTAKER -> skjema.arbeidstakerData = mergedJson
        }
        
        skjema.endretAv = subjectHandler.getUserID() 
            ?: throw IllegalStateException("Unable to determine current user")
        return skjemaRepository.save(skjema)
    }

    fun updateArbeidsgiverData(id: UUID, data: Any): Skjema {
        return updateJsonData(id, data, DataType.ARBEIDSGIVER)
    }

    fun updateArbeidstakerData(id: UUID, data: Any): Skjema {
        return updateJsonData(id, data, DataType.ARBEIDSTAKER)
    }

    fun saveArbeidsgiverInfo(skjemaId: UUID, request: ArbeidsgiverRequest): Skjema {
        log.info { "Saving arbeidsgiver info for skjema: $skjemaId" }
        return updateArbeidsgiverData(skjemaId, request)
    }

    fun saveVirksomhetInfo(skjemaId: UUID, request: VirksomhetRequest): Skjema {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }
        return updateArbeidsgiverData(skjemaId, request)
    }

    fun saveUtenlandsoppdragInfo(skjemaId: UUID, request: UtenlandsoppdragRequest): Skjema {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }
        return updateArbeidsgiverData(skjemaId, request)
    }

    fun saveArbeidstakerLonnInfo(skjemaId: UUID, request: ArbeidstakerLonnRequest): Skjema {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }
        return updateArbeidsgiverData(skjemaId, request)
    }

    fun submitArbeidsgiverOppsummering(skjemaId: UUID, request: OppsummeringRequest): Skjema {
        log.info { "Submitting arbeidsgiver oppsummering for skjema: $skjemaId" }
        val skjema = skjemaRepository.findById(skjemaId).orElseThrow {
            IllegalArgumentException("Skjema with id $skjemaId not found")
        }
        skjema.status = SkjemaStatus.SENDT
        skjema.endretAv = subjectHandler.getUserID() 
            ?: throw IllegalStateException("Unable to determine current user")
        return skjemaRepository.save(skjema)
    }

    fun saveArbeidstakerInfo(skjemaId: UUID, request: ArbeidstakerRequest): Skjema {
        log.info { "Saving arbeidstaker info for skjema: $skjemaId" }
        return updateArbeidstakerData(skjemaId, request)
    }

    fun saveSkatteforholdOgInntektInfo(skjemaId: UUID, request: SkatteforholdOgInntektRequest): Skjema {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }
        return updateArbeidstakerData(skjemaId, request)
    }

    fun listSkjemaerByUser(): List<Skjema> {
        val currentUser = subjectHandler.getUserID() //TODO Sjekk om vi faktisk kan bruke getUserID
            ?: throw IllegalStateException("Unable to determine current user")
        return skjemaRepository.findByFnr(currentUser)
    }

    fun deleteSkjema(id: UUID): Boolean {
        return if (skjemaRepository.existsById(id)) {
            skjemaRepository.deleteById(id)
            true
        } else {
            false
        }
    }
}