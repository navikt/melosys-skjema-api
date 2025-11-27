package no.nav.melosys.skjema.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.dto.SubmitSkjemaRequest
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDto
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class SkjemaService(
    private val skjemaRepository: SkjemaRepository,
    private val objectMapper: ObjectMapper,
    private val subjectHandler: SubjectHandler,
    private val altinnService: AltinnService
) {


    fun getSkjemaAsArbeidstaker(skjemaId: UUID): Skjema {
        val currentUser = subjectHandler.getUserID()
        return skjemaRepository.findByIdAndFnr(skjemaId, currentUser)
            ?: throw IllegalArgumentException("Skjema with id $skjemaId not found or access denied")
    }

    // TODO: På et punkt i fremtiden så vil muligens ikke denne tilgangsjekken alene være nok
    private fun getSkjemaAsArbeidsgiver(skjemaId: UUID): Skjema = skjemaRepository.findByIdOrNull(skjemaId)
        ?.takeIf { it.orgnr != null && altinnService.harBrukerTilgang(it.orgnr) }
        ?: throw IllegalArgumentException("Skjema with id $skjemaId not found")

    fun saveVirksomhetInfo(skjemaId: UUID, request: ArbeidsgiverensVirksomhetINorgeDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving virksomhet info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidsgiverensVirksomhetINorge = request)
        }
    }

    fun saveUtenlandsoppdragInfo(skjemaId: UUID, request: UtenlandsoppdragetDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving utenlandsoppdrag info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
    }

    fun saveArbeidstakerLonnInfo(skjemaId: UUID, request: ArbeidstakerensLonnDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving arbeidstaker lønn info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidstakerensLonn = request)
        }
    }

    fun saveArbeidsstedIUtlandetInfo(skjemaId: UUID, request: ArbeidsstedIUtlandetDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving arbeidssted i utlandet info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidsstedIUtlandet = request)
        }
    }

    fun saveTilleggsopplysningerInfoAsArbeidsgiver(skjemaId: UUID, request: TilleggsopplysningerDto): ArbeidsgiversSkjemaDto {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }

        return updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
    }

    fun submitArbeidsgiver(skjemaId: UUID, request: SubmitSkjemaRequest): ArbeidstakersSkjemaDto {
        log.info { "Submitting arbeidsgiver oppsummering for skjema: $skjemaId" }
        val currentUser = subjectHandler.getUserID()

        val skjema = getSkjemaAsArbeidsgiver(skjemaId)

        skjema.status = SkjemaStatus.SENDT
        skjema.endretAv = currentUser
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToArbeidstakersSkjemaDto(savedSkjema)
    }

    fun saveUtenlandsoppdragetInfoAsArbeidstaker(skjemaId: UUID, request: UtenlandsoppdragetArbeidstakersDelDto): ArbeidstakersSkjemaDto {
        log.info { "Saving utenlandsoppdraget info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(utenlandsoppdraget = request)
        }
    }

    fun saveArbeidssituasjonInfo(skjemaId: UUID, request: ArbeidssituasjonDto): ArbeidstakersSkjemaDto {
        log.info { "Saving arbeidssituasjon info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(arbeidssituasjon = request)
        }
    }

    fun saveSkatteforholdOgInntektInfo(skjemaId: UUID, request: SkatteforholdOgInntektDto): ArbeidstakersSkjemaDto {
        log.info { "Saving skatteforhold og inntekt info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(skatteforholdOgInntekt = request)
        }
    }

    fun saveFamiliemedlemmerInfo(skjemaId: UUID, request: FamiliemedlemmerDto): ArbeidstakersSkjemaDto {
        log.info { "Saving familiemedlemmer info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(familiemedlemmer = request)
        }
    }

    fun saveTilleggsopplysningerInfo(skjemaId: UUID, request: TilleggsopplysningerDto): ArbeidstakersSkjemaDto {
        log.info { "Saving tilleggsopplysninger info for skjema: $skjemaId" }

        return updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(skjemaId) { dto ->
            dto.copy(tilleggsopplysninger = request)
        }
    }

    private fun updateArbeidsgiverSkjemaDataAndConvertToArbeidsgiversSkjemaDto(
        skjemaId: UUID,
        updateFunction: (ArbeidsgiversSkjemaDataDto) -> ArbeidsgiversSkjemaDataDto
    ): ArbeidsgiversSkjemaDto {
        val skjema = getSkjemaAsArbeidsgiver(skjemaId)

        // Read existing ArbeidsgiversSkjemaDto or create empty one
        val existingDto = convertToArbeidsgiversSkjemaDataDto(skjema.data)

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Convert back to JSON and save
        skjema.data = objectMapper.valueToTree(updatedDto)
        return saveAndConvertToArbeidsgiversSkjemaDto(skjema)
    }

    private fun updateArbeidstakerSkjemaDataAndConvertToArbeidstakersSkjemaDto(
        skjemaId: UUID,
        updateFunction: (ArbeidstakersSkjemaDataDto) -> ArbeidstakersSkjemaDataDto
    ): ArbeidstakersSkjemaDto {
        val skjema = getSkjemaAsArbeidstaker(skjemaId)

        // Read existing ArbeidstakersSkjemaDataDto or create empty one
        val existingDto = convertToArbeidstakersSkjemaDataDto(skjema.data)

        // Apply the update function
        val updatedDto = updateFunction(existingDto)

        // Convert back to JSON and save
        skjema.data = objectMapper.valueToTree(updatedDto)
        return saveAndConvertToArbeidstakersSkjemaDto(skjema)
    }

    private fun saveAndConvertToArbeidsgiversSkjemaDto(skjema: Skjema): ArbeidsgiversSkjemaDto {
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToArbeidsgiversSkjemaDto(savedSkjema)
    }

    private fun saveAndConvertToArbeidstakersSkjemaDto(skjema: Skjema): ArbeidstakersSkjemaDto {
        val savedSkjema = skjemaRepository.save(skjema)
        return convertToArbeidstakersSkjemaDto(savedSkjema)
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

    private fun convertToArbeidsgiversSkjemaDto(skjema: Skjema): ArbeidsgiversSkjemaDto {
        val data = convertToArbeidsgiversSkjemaDataDto(skjema.data)
        
        return ArbeidsgiversSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            orgnr = skjema.orgnr ?: error("Skjema orgnr is null"),
            status = skjema.status,
            data = data
        )
    }

    private fun convertToArbeidstakersSkjemaDto(skjema: Skjema): ArbeidstakersSkjemaDto {
        val data = convertToArbeidstakersSkjemaDataDto(skjema.data)
        
        return ArbeidstakersSkjemaDto(
            id = skjema.id ?: error("Skjema ID is null"),
            fnr = skjema.fnr ?: error("Skjema fnr is null"),
            status = skjema.status,
            data = data
        )
    }


}