package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
    private fun getSkjemaAsArbeidsgiver(id: UUID): Skjema = skjemaRepository.findByIdOrNull(id)
        ?.takeIf { it.orgnr != null && altinnService.harBrukerTilgang(it.orgnr) }
        ?: throw IllegalArgumentException("Skjema with id $id not found")

    fun getSkjemaDtoAsArbeidsgiver(id: UUID): ArbeidsgiversSkjemaDto {
        val skjema = getSkjemaAsArbeidsgiver(id)
        val data = convertToArbeidsgiversSkjemaDataDto(skjema.data)
        
        return ArbeidsgiversSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            orgnr = skjema.orgnr ?: error("Skjema orgnr is null"),
            status = skjema.status,
            data = data
        )
    }

    fun getSkjemaDtoAsArbeidstaker(id: UUID): ArbeidstakersSkjemaDto {
        val skjema = getSkjemaAsArbeidstaker(id)
        val data = convertToArbeidstakersSkjemaDataDto(skjema.data)
        
        return ArbeidstakersSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            fnr = skjema.fnr ?: error("Skjema fnr is null"),
            status = skjema.status,
            data = data
        )
    }

    fun saveArbeidsgiverInfo(skjemaId: UUID, request: ArbeidsgiverenDto): Skjema {
        log.info { "Saving arbeidsgiver info for skjema: $skjemaId" }
        return updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidsgiveren = request)
        }
    }

    fun saveVirksomhetInfo(skjemaId: UUID, request: ArbeidsgiverensVirksomhetINorgeDto): Skjema {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }
        return updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidsgiverensVirksomhetINorge = request)
        }
    }

    fun saveUtenlandsoppdragInfo(skjemaId: UUID, request: UtenlandsoppdragetDto): Skjema {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }
        return updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
    }

    fun saveArbeidstakerLonnInfo(skjemaId: UUID, request: ArbeidstakerensLonnDto): Skjema {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }
        return updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidstakerensLonn = request)
        }
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
        return updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidstakeren = request)
        }
    }

    fun saveSkatteforholdOgInntektInfo(skjemaId: UUID, request: SkatteforholdOgInntektDto): Skjema {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }
        return updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(skatteforholdOgInntekt = request)
        }
    }

    fun saveFamiliemedlemmerInfo(skjemaId: UUID, request: FamiliemedlemmerDto): Skjema {
        log.info { "Saving familiemedlemmer info for skjema: $skjemaId" }
        return updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(familiemedlemmer = request)
        }
    }

    fun saveTilleggsopplysningerInfo(skjemaId: UUID, request: TilleggsopplysningerDto): Skjema {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }
        return updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
    }

    fun listSkjemaerByUser(): List<Skjema> {
        val currentUser = subjectHandler.getUserID()
        return skjemaRepository.findByFnr(currentUser)
    }

    private fun updateArbeidsgiverSkjemaData(
        id: UUID,
        updateFunction: (ArbeidsgiversSkjemaDataDto) -> ArbeidsgiversSkjemaDataDto
    ): Skjema {
        val skjema = getSkjemaAsArbeidsgiver(id)

        // Read existing ArbeidsgiversSkjemaDto or create empty one
        val existingDto = convertToArbeidsgiversSkjemaDataDto(skjema.data)

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Convert back to JSON and save
        skjema.data = objectMapper.valueToTree(updatedDto)
        return skjemaRepository.save(skjema)
    }

    private fun updateArbeidstakerSkjemaData(
        id: UUID,
        updateFunction: (ArbeidstakersSkjemaDataDto) -> ArbeidstakersSkjemaDataDto
    ): Skjema {
        val skjema = getSkjemaAsArbeidstaker(id)

        // Read existing ArbeidstakersSkjemaDataDto or create empty one
        val existingDto = convertToArbeidstakersSkjemaDataDto(skjema.data)

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Convert back to JSON and save
        skjema.data = objectMapper.valueToTree(updatedDto)
        return skjemaRepository.save(skjema)
    }

    private fun convertToArbeidsgiversSkjemaDataDto(data: JsonNode?): ArbeidsgiversSkjemaDataDto {
        return convertDataToDto(data, ArbeidsgiversSkjemaDataDto())
    }

    private fun convertToArbeidstakersSkjemaDataDto(data: JsonNode?): ArbeidstakersSkjemaDataDto {
        return convertDataToDto(data, ArbeidstakersSkjemaDataDto())
    }

    private inline fun <reified T> convertDataToDto(data: JsonNode?, defaultValue: T): T {
        return if (data == null) {
            defaultValue
        } else {
            objectMapper.treeToValue(data, T::class.java)
        }
    }


}