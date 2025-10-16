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

    fun createSkjemaArbeidsgiverDel(request: CreateArbeidsgiverSkjemaRequest): ArbeidsgiversSkjemaDto {
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
        val createdSkjema = skjemaRepository.save(skjema)

        return convertToArbeidsgiversSkjemaDto(createdSkjema)
    }

    fun createSkjemaArbeidstakerDel(request: CreateArbeidstakerSkjemaRequest): ArbeidstakersSkjemaDto {
        val currentUser = subjectHandler.getUserID()
        val skjema = Skjema(
            status = SkjemaStatus.UTKAST,
            fnr = request.fnr,
            opprettetAv = currentUser,
            endretAv = currentUser
        )
        val createdSkjema = skjemaRepository.save(skjema)
        return convertToArbeidstakersSkjemaDto(createdSkjema)
    }

    fun getSkjemaAsArbeidstaker(skjemaId: UUID): Skjema {
        val currentUser = subjectHandler.getUserID()
        return skjemaRepository.findByIdAndFnr(skjemaId, currentUser)
            ?: throw IllegalArgumentException("Skjema with id $skjemaId not found or access denied")
    }

    // TODO: På et punkt i fremtiden så vil muligens ikke denne tilgangsjekken alene være nok
    private fun getSkjemaAsArbeidsgiver(skjemaId: UUID): Skjema = skjemaRepository.findByIdOrNull(skjemaId)
        ?.takeIf { it.orgnr != null && altinnService.harBrukerTilgang(it.orgnr) }
        ?: throw IllegalArgumentException("Skjema with id $skjemaId not found")

    fun getSkjemaDtoAsArbeidsgiver(skjemaId: UUID): ArbeidsgiversSkjemaDto {
        val skjema = getSkjemaAsArbeidsgiver(skjemaId)
        
        return convertToArbeidsgiversSkjemaDto(skjema)
    }

    fun getSkjemaDtoAsArbeidstaker(skjemaId: UUID): ArbeidstakersSkjemaDto {
        val skjema = getSkjemaAsArbeidstaker(skjemaId)

        return convertToArbeidstakersSkjemaDto(skjema)
    }

    fun saveArbeidsgiverInfo(skjemaId: UUID, request: ArbeidsgiverenDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving arbeidsgiver info for skjema: $skjemaId" }
        val skjema = updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidsgiveren = request)
        }
        return convertToArbeidsgiversSkjemaDto(skjema)
    }

    fun saveVirksomhetInfo(skjemaId: UUID, request: ArbeidsgiverensVirksomhetINorgeDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }
        val skjema = updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidsgiverensVirksomhetINorge = request)
        }
        return convertToArbeidsgiversSkjemaDto(skjema)
    }

    fun saveUtenlandsoppdragInfo(skjemaId: UUID, request: UtenlandsoppdragetDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }
        val skjema = updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
        return convertToArbeidsgiversSkjemaDto(skjema)
    }

    fun saveArbeidstakerLonnInfo(skjemaId: UUID, request: ArbeidstakerensLonnDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }
        val skjema = updateArbeidsgiverSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidstakerensLonn = request)
        }
        return convertToArbeidsgiversSkjemaDto(skjema)
    }

    fun submitArbeidsgiver(skjemaId: UUID, request: SubmitSkjemaRequest): ArbeidstakersSkjemaDto {
        log.info { "Submitting arbeidsgiver oppsummering for skjema: $skjemaId" }
        val currentUser = subjectHandler.getUserID()

        val skjema = getSkjemaAsArbeidstaker(skjemaId)
        
        skjema.status = SkjemaStatus.SENDT
        skjema.endretAv = currentUser
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToArbeidstakersSkjemaDto(savedSkjema)
    }

    fun saveArbeidstakerInfo(skjemaId: UUID, request: ArbeidstakerenDto): ArbeidstakersSkjemaDto {
        log.info { "Saving arbeidstaker info for skjema: $skjemaId" }
        val skjema = updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(arbeidstakeren = request)
        }
        return convertToArbeidstakersSkjemaDto(skjema)
    }

    fun saveSkatteforholdOgInntektInfo(skjemaId: UUID, request: SkatteforholdOgInntektDto): ArbeidstakersSkjemaDto {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }
        val skjema = updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(skatteforholdOgInntekt = request)
        }
        return convertToArbeidstakersSkjemaDto(skjema)
    }

    fun saveFamiliemedlemmerInfo(skjemaId: UUID, request: FamiliemedlemmerDto): ArbeidstakersSkjemaDto {
        log.info { "Saving familiemedlemmer info for skjema: $skjemaId" }
        val skjema = updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(familiemedlemmer = request)
        }
        return convertToArbeidstakersSkjemaDto(skjema)
    }

    fun saveTilleggsopplysningerInfo(skjemaId: UUID, request: TilleggsopplysningerDto): ArbeidstakersSkjemaDto {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }
        val skjema = updateArbeidstakerSkjemaData(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
        return convertToArbeidstakersSkjemaDto(skjema)
    }

    fun listSkjemaerByUser(): List<ArbeidstakersSkjemaDto> {
        val currentUser = subjectHandler.getUserID()
        val skjemaer = skjemaRepository.findByFnr(currentUser)
        return skjemaer.map { convertToArbeidstakersSkjemaDto(it) }
    }

    private fun updateArbeidsgiverSkjemaData(
        skjemaId: UUID,
        updateFunction: (ArbeidsgiversSkjemaDataDto) -> ArbeidsgiversSkjemaDataDto
    ): Skjema {
        val skjema = getSkjemaAsArbeidsgiver(skjemaId)

        // Read existing ArbeidsgiversSkjemaDto or create empty one
        val existingDto = convertToArbeidsgiversSkjemaDataDto(skjema.data)

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Convert back to JSON and save
        skjema.data = objectMapper.valueToTree(updatedDto)
        return skjemaRepository.save(skjema)
    }

    private fun updateArbeidstakerSkjemaData(
        skjemaId: UUID,
        updateFunction: (ArbeidstakersSkjemaDataDto) -> ArbeidstakersSkjemaDataDto
    ): Skjema {
        val skjema = getSkjemaAsArbeidstaker(skjemaId)

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

    fun convertToArbeidsgiversSkjemaDto(skjema: Skjema): ArbeidsgiversSkjemaDto {
        val data = convertToArbeidsgiversSkjemaDataDto(skjema.data)
        
        return ArbeidsgiversSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            orgnr = skjema.orgnr ?: error("Skjema orgnr is null"),
            status = skjema.status,
            data = data
        )
    }

    fun convertToArbeidstakersSkjemaDto(skjema: Skjema): ArbeidstakersSkjemaDto {
        val data = convertToArbeidstakersSkjemaDataDto(skjema.data)
        
        return ArbeidstakersSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            fnr = skjema.fnr ?: error("Skjema fnr is null"),
            status = skjema.status,
            data = data
        )
    }


}