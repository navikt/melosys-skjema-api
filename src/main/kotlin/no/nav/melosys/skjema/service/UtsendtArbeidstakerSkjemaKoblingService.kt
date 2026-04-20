package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.overlapper
import no.nav.melosys.skjema.extensions.utsendelsePeriode
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
class UtsendtArbeidstakerSkjemaKoblingService(
    private val skjemaRepository: SkjemaRepository
) {

    @Transactional
    fun finnOgKobl(skjema: Skjema): KoblingsResultat {
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        val kandidater = skjemaRepository.findByFnrAndTypeAndStatus(skjema.fnr, SkjemaType.UTSENDT_ARBEIDSTAKER, SkjemaStatus.SENDT)
            .filter { it.id != skjema.id }

        // Erstatter-kobling: finner tidligere versjon av samme skjemadel
        val erstatter = finnMatch(skjema, kandidater, sammeDel = true)
        val arvetKobletSkjemaId = erstatter?.let { utforErstatterKobling(skjema, it) }

        // Motpart-kobling: kobler arbeidsgiver-del og arbeidstaker-del.
        // Kombinert skjemadel (ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL) har ingen motpart.
        val motpart = if (arvetKobletSkjemaId == null && metadata.skjemadel != Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL) {
            finnMatch(skjema, kandidater, sammeDel = false)?.also { utforMotpartKobling(skjema, it) }
        } else null

        val kobletSkjemaId = arvetKobletSkjemaId ?: motpart?.id
        log.info { "Kobling for ${skjema.id}: koblet=$kobletSkjemaId, erstatter=${erstatter?.id}" }
        return KoblingsResultat(kobletSkjemaId = kobletSkjemaId, erstatterSkjemaId = erstatter?.id)
    }

    private fun finnMatch(
        skjema: Skjema,
        kandidater: List<Skjema>,
        sammeDel: Boolean
    ): Skjema? {
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        val ønsketDel = if (sammeDel) metadata.skjemadel else metadata.skjemadel.motpart()
        val matchendeKandidater = kandidater.filter { kandidat ->
            val km = kandidat.metadata as UtsendtArbeidstakerMetadata
            km.skjemadel == ønsketDel
                && km.juridiskEnhetOrgnr == metadata.juridiskEnhetOrgnr
                && (sammeDel || km.kobletSkjemaId == null)
        }
        if (matchendeKandidater.isEmpty()) return null

        val samletPeriode = samletPeriode(matchendeKandidater) ?: return null
        val skjemaPeriode = skjema.utsendelsePeriode() ?: return null

        if (!skjemaPeriode.overlapper(samletPeriode)) return null

        return matchendeKandidater.first()
    }

    private fun Skjemadel.motpart() = when (this) {
        Skjemadel.ARBEIDSTAKERS_DEL -> Skjemadel.ARBEIDSGIVERS_DEL
        Skjemadel.ARBEIDSGIVERS_DEL -> Skjemadel.ARBEIDSTAKERS_DEL
        Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> error("Kombinert skjemadel har ingen motpart")
    }

    private fun samletPeriode(skjemaer: List<Skjema>): PeriodeDto? {
        val perioder = skjemaer.mapNotNull { it.utsendelsePeriode() }
        if (perioder.isEmpty()) return null
        return PeriodeDto(
            fraDato = perioder.minOf { it.fraDato },
            tilDato = perioder.maxOf { it.tilDato }
        )
    }

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
