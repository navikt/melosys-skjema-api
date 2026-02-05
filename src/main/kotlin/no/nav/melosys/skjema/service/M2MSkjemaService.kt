package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.overlapper
import no.nav.melosys.skjema.extensions.parseArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseUtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
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
        val skjema =  skjemaRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("Skjema med id $id ikke funnet")

        val innsending = innsendingRepository.findBySkjemaId(skjema.id!!)
            ?: throw NoSuchElementException("Skjema med id $id ikke funnet")

       val (arbeidstakersDel, arbeidsgiversDel) = utledArbeidstakerOgArbeidsgiversDel(skjema)

        return UtsendtArbeidstakerM2MSkjemaData(
            arbeidstakersDel = arbeidstakersDel,
            arbeidsgiversDel = arbeidsgiversDel,
            referanseId = innsending.referanseId
        )

    }


    private fun utledArbeidstakerOgArbeidsgiversDel(skjema: Skjema): Pair<ArbeidstakersSkjemaDataDto?, ArbeidsgiversSkjemaDataDto?> {

        val skjemaErArbeidstakersDel = erArbeidstakersDel(skjema)

        val innsendtSoknadForSammeFnrOgOrgISammeTidsrom = skjemaRepository.findByFnrAndOrgnrAndStatus(
            skjema.fnr!!,
            skjema.orgnr!!,
            status = SkjemaStatus.SENDT
        )
            .filter { it.id != skjema.id }
            .filter { erArbeidstakersDel(it) != skjemaErArbeidstakersDel }
            .find {
                if (skjemaErArbeidstakersDel) {
                    utsendtArbeidstakerSkjemaPerioderOverlapper(skjema, it)
                } else {
                    utsendtArbeidstakerSkjemaPerioderOverlapper(it, skjema)
                }
            }

        return if (skjemaErArbeidstakersDel) {
            Pair(
                jsonMapper.parseArbeidstakersSkjemaDataDto(skjema.data!!),
                innsendtSoknadForSammeFnrOgOrgISammeTidsrom?.let {
                    jsonMapper.parseArbeidsgiversSkjemaDataDto(it.data!!)
                }
            )
        } else {
            Pair(
                innsendtSoknadForSammeFnrOgOrgISammeTidsrom?.let {
                    jsonMapper.parseArbeidstakersSkjemaDataDto(it.data!!)
                },
                jsonMapper.parseArbeidsgiversSkjemaDataDto(skjema.data!!)
            )
        }
    }

    private fun utsendtArbeidstakerSkjemaPerioderOverlapper(
        arbeidstakersDel: Skjema,
        arbeidsgiversDel: Skjema
    ): Boolean =
        jsonMapper.parseArbeidstakersSkjemaDataDto(arbeidstakersDel.data!!).utenlandsoppdraget!!.utsendelsePeriode.overlapper(
            jsonMapper.parseArbeidsgiversSkjemaDataDto(arbeidsgiversDel.data!!).utenlandsoppdraget!!.arbeidstakerUtsendelsePeriode
        )

    // TODO: Vi må innføre et nytt felt i metadata for å kunne utlede enklere
    // Denne blir redundant når vi har lagt til eget metadatafelt for denne informasjonen
    fun erArbeidstakersDel(skjema: Skjema): Boolean =
        jsonMapper.parseUtsendtArbeidstakerMetadata(skjema.metadata!!).representasjonstype in listOf(
            Representasjonstype.DEG_SELV,
            Representasjonstype.ANNEN_PERSON
        )


}
