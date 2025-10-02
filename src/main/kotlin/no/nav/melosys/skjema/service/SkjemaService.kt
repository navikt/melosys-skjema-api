package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.controller.*
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
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
        val skjema = Skjema(
            status = SkjemaStatus.UTKAST,
            fnr = fnr,
            orgnr = orgnr,
            opprettetAv = currentUser,
            endretAv = currentUser
        )
        return skjemaRepository.save(skjema)
    }

    fun getSkjema(id: UUID): Skjema {
        val currentUser = subjectHandler.getUserID()
        return skjemaRepository.findByIdAndFnr(id, currentUser)
    }

    private fun updateJsonData(id: UUID, data: Any, dataType: DataType): Skjema {
        val currentUser = subjectHandler.getUserID()
        
        val skjema = skjemaRepository.findByIdAndFnr(id, currentUser)
        
        // Read existing JSON or create empty object
        val existingData = if (!skjema.data.isNullOrBlank()) {
            objectMapper.readTree(skjema.data) as ObjectNode
        } else {
            objectMapper.createObjectNode()
        }
        
        // Ensure the specific data type section exists
        val dataTypeKey = when (dataType) {
            DataType.ARBEIDSGIVER -> "arbeidsgiver"
            DataType.ARBEIDSTAKER -> "arbeidstaker"
        }
        
        if (!existingData.has(dataTypeKey)) {
            existingData.set<JsonNode>(dataTypeKey, objectMapper.createObjectNode())
        }
        
        // Merge new data into the appropriate section
        val newData = objectMapper.valueToTree<JsonNode>(data) as ObjectNode
        val sectionData = existingData.get(dataTypeKey) as ObjectNode
        sectionData.setAll<JsonNode>(newData)
        
        // Update the data field with merged JSON
        skjema.data = objectMapper.writeValueAsString(existingData)

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
        val currentUser = subjectHandler.getUserID()

        val skjema = getSkjema(skjemaId)
        
        skjema.status = SkjemaStatus.SENDT
        skjema.endretAv = currentUser
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
        val currentUser = subjectHandler.getUserID()
        return skjemaRepository.findByFnr(currentUser)
    }

    fun deleteSkjema(id: UUID): Boolean {
        val currentUser = subjectHandler.getUserID()
        
        val deletedCount = skjemaRepository.deleteByIdAndFnr(id, currentUser)
        return deletedCount > 0
    }
}