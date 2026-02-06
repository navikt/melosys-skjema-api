package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.overlapper
import no.nav.melosys.skjema.extensions.parseArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger { }

data class KoblingsResultat(
    val kobletSkjemaId: UUID?,
    val erstatterSkjemaId: UUID?
)

/**
 * Kobling av separate søknader (arbeidsgiver-del og arbeidstaker-del).
 *
 * Håndterer to typer koblinger:
 * - **Motpart-kobling**: Kobler arbeidsgiver-del og arbeidstaker-del som hører til samme søknad.
 * - **Erstatter-kobling**: Når samme del sendes inn på nytt, arver det nye skjemaet koblingen fra forrige versjon.
 *
 * Matching-kriterier:
 * 1. Samme arbeidstaker-fnr
 * 2. Status = SENDT
 * 3. Samme juridisk enhet (orgnr)
 * 4. Overlappende perioder (minst 1 dag felles)
 * 5. For motpart: motsatt skjemadel og ikke allerede koblet
 * 6. For erstatter: samme skjemadel
 */
@Service
class SkjemaKoblingService(
    private val skjemaRepository: SkjemaRepository,
    private val jsonMapper: JsonMapper
) {

    @Transactional
    fun finnOgKobl(skjema: Skjema): KoblingsResultat {
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        val kandidater = skjemaRepository.findByFnrAndStatus(skjema.fnr, SkjemaStatus.SENDT)
            .filter { it.id != skjema.id }

        val erstatter = finnMatch(skjema, kandidater, metadata, sammeDel = true)
        val arvetKobletSkjemaId = erstatter?.let { utforErstatterKobling(skjema, it) }

        val motpart = if (arvetKobletSkjemaId == null) {
            finnMatch(skjema, kandidater, metadata, sammeDel = false)?.also { utforMotpartKobling(skjema, it) }
        } else null

        val kobletSkjemaId = arvetKobletSkjemaId ?: motpart?.id
        log.info { "Kobling for ${skjema.id}: koblet=$kobletSkjemaId, erstatter=${erstatter?.id}" }
        return KoblingsResultat(kobletSkjemaId = kobletSkjemaId, erstatterSkjemaId = erstatter?.id)
    }

    private fun finnMatch(
        skjema: Skjema,
        kandidater: List<Skjema>,
        metadata: UtsendtArbeidstakerMetadata,
        sammeDel: Boolean
    ): Skjema? {
        val ønsketDel = if (sammeDel) metadata.skjemadel else metadata.skjemadel.motpart()
        return kandidater.find { kandidat ->
            val km = kandidat.metadata as UtsendtArbeidstakerMetadata
            km.skjemadel == ønsketDel
                && km.juridiskEnhetOrgnr == metadata.juridiskEnhetOrgnr
                && (sammeDel || km.kobletSkjemaId == null)
                && perioderOverlapper(skjema, kandidat)
        }
    }

    private fun Skjemadel.motpart() = when (this) {
        Skjemadel.ARBEIDSTAKERS_DEL -> Skjemadel.ARBEIDSGIVERS_DEL
        Skjemadel.ARBEIDSGIVERS_DEL -> Skjemadel.ARBEIDSTAKERS_DEL
    }

    private fun perioderOverlapper(skjema1: Skjema, skjema2: Skjema): Boolean {
        val p1 = hentPeriode(skjema1) ?: return false
        val p2 = hentPeriode(skjema2) ?: return false
        return p1.overlapper(p2)
    }

    private fun hentPeriode(skjema: Skjema): PeriodeDto? = try {
        when ((skjema.metadata as UtsendtArbeidstakerMetadata).skjemadel) {
            Skjemadel.ARBEIDSTAKERS_DEL ->
                jsonMapper.parseArbeidstakersSkjemaDataDto(skjema.data!!).utenlandsoppdraget?.utsendelsePeriode
            Skjemadel.ARBEIDSGIVERS_DEL ->
                jsonMapper.parseArbeidsgiversSkjemaDataDto(skjema.data!!).utenlandsoppdraget?.arbeidstakerUtsendelsePeriode
        }
    } catch (_: Exception) { null }

    private fun utforErstatterKobling(nyttSkjema: Skjema, gammelSkjema: Skjema): UUID? {
        val arvetKobletSkjemaId = (gammelSkjema.metadata as UtsendtArbeidstakerMetadata).kobletSkjemaId

        oppdaterMetadata(nyttSkjema) {
            it.medErstatterSkjemaId(gammelSkjema.id).medKobletSkjemaId(arvetKobletSkjemaId)
        }

        if (arvetKobletSkjemaId != null) {
            skjemaRepository.findById(arvetKobletSkjemaId).ifPresent { motpart ->
                oppdaterMetadata(motpart) { it.medKobletSkjemaId(nyttSkjema.id) }
            }
            oppdaterMetadata(gammelSkjema) { it.medKobletSkjemaId(null) }
        }

        return arvetKobletSkjemaId
    }

    private fun utforMotpartKobling(nyttSkjema: Skjema, matchendeSkjema: Skjema) {
        oppdaterMetadata(matchendeSkjema) { it.medKobletSkjemaId(nyttSkjema.id) }
        oppdaterMetadata(nyttSkjema) { it.medKobletSkjemaId(matchendeSkjema.id) }
    }

    private fun oppdaterMetadata(skjema: Skjema, transform: (UtsendtArbeidstakerMetadata) -> UtsendtArbeidstakerMetadata) {
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        skjema.metadata = transform(metadata)
        skjemaRepository.save(skjema)
    }
}
