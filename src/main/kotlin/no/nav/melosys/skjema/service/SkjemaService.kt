package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.*
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import java.util.*
import org.springframework.data.repository.findByIdOrNull

private val log = KotlinLogging.logger { }

private enum class DataType {
    ARBEIDSGIVER,
    ARBEIDSTAKER
}

@Service
class SkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val objectMapper: ObjectMapper,
    private val subjectHandler: SubjectHandler,
    private val altinnService: AltinnService
) {

    fun createSkjemaArbeidsgiverDel(request: CreateArbeidsgiverSkjemaRequest): Skjema {
        val currentUser = subjectHandler.getUserID()

        if (!altinnService.harBrukerTilgang(request.orgnr)) {
            throw IllegalArgumentException("User does not have access to orgnr ${request.orgnr}")
        }

        val skjema = Skjema(
            status = SkjemaStatus.UTKAST,
            orgnr = request.orgnr,
            opprettetAv = currentUser,
            endretAv = currentUser
        )
        return skjemaRepository.save(skjema)
    }

    fun createSkjemaArbeidstakerDel(request: CreateArbeidstakerSkjemaRequest): Skjema {
        val currentUser = subjectHandler.getUserID()
        val skjema = Skjema(
            status = SkjemaStatus.UTKAST,
            fnr = request.fnr,
            opprettetAv = currentUser,
            endretAv = currentUser
        )
        return skjemaRepository.save(skjema)
    }

    fun getSkjemaAsArbeidstaker(id: UUID): Skjema {
        val currentUser = subjectHandler.getUserID()
        return skjemaRepository.findByIdAndFnr(id, currentUser)
            ?: throw IllegalArgumentException("Skjema with id $id not found or access denied")
    }

    // TODO: På et punkt i fremtiden så vil muligens ikke denne tilgangsjekken alene være nok
    fun getSkjemaAsArbeidsgiver(id: UUID): Skjema = skjemaRepository.findByIdOrNull(id)
        ?.takeIf { it.orgnr != null && altinnService.harBrukerTilgang(it.orgnr) }
        ?: throw IllegalArgumentException("Skjema with id $id not found")

    fun saveArbeidsgiverInfo(skjemaId: UUID, request: ArbeidsgiverenDto): Skjema {
        log.info { "Saving arbeidsgiver info for skjema: $skjemaId" }
        return updateJsonData(skjemaId, request, DataType.ARBEIDSGIVER, ::getSkjemaAsArbeidsgiver)
    }

    fun saveVirksomhetInfo(skjemaId: UUID, request: ArbeidsgiverensVirksomhetINorgeDto): Skjema {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }
        return updateJsonData(skjemaId, request, DataType.ARBEIDSGIVER, ::getSkjemaAsArbeidsgiver)
    }

    fun saveUtenlandsoppdragInfo(skjemaId: UUID, request: UtenlandsoppdragetDto): Skjema {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }
        return updateJsonData(skjemaId, request, DataType.ARBEIDSGIVER, ::getSkjemaAsArbeidsgiver)
    }

    fun saveArbeidstakerLonnInfo(skjemaId: UUID, request: ArbeidstakerensLonnDto): Skjema {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }
        return updateJsonData(skjemaId, request, DataType.ARBEIDSGIVER, ::getSkjemaAsArbeidsgiver)
    }

    fun submitArbeidsgiver(skjemaId: UUID, request: SubmitSkjemaRequest): Skjema {
        log.info { "Submitting arbeidsgiver oppsummering for skjema: $skjemaId" }
        val currentUser = subjectHandler.getUserID()

        val skjema = getSkjemaAsArbeidstaker(skjemaId)
        
        skjema.status = SkjemaStatus.SENDT
        skjema.endretAv = currentUser
        return skjemaRepository.save(skjema)
    }

    fun saveArbeidstakerInfo(skjemaId: UUID, request: ArbeidstakerenDto): Skjema {
        log.info { "Saving arbeidstaker info for skjema: $skjemaId" }
        return updateJsonData(skjemaId, request, DataType.ARBEIDSTAKER, ::getSkjemaAsArbeidstaker)
    }

    fun saveSkatteforholdOgInntektInfo(skjemaId: UUID, request: SkatteforholdOgInntektDto): Skjema {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }
        return updateJsonData(skjemaId, request, DataType.ARBEIDSTAKER, ::getSkjemaAsArbeidstaker)
    }

    fun listSkjemaerByUser(): List<Skjema> {
        val currentUser = subjectHandler.getUserID()
        return skjemaRepository.findByFnr(currentUser)
    }


    private fun updateJsonData(id: UUID, data: Any, dataType: DataType, skjemaRetriever: (UUID) -> Skjema): Skjema {
        val skjema = skjemaRetriever(id)

        // Read existing JSON or create empty object
        val existingData = if (skjema.data != null && !skjema.data!!.isNull && skjema.data!!.isObject) {
            skjema.data!!.deepCopy() as ObjectNode
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
        skjema.data = existingData

        return skjemaRepository.save(skjema)
    }

}