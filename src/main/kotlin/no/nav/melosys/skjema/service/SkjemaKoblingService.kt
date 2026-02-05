package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.overlapper
import no.nav.melosys.skjema.extensions.parseArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.extensions.parseUtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger { }

/**
 * Resultat av koblingsforsøk.
 */
data class KoblingsResultat(
    /** ID til matchende skjema, eller null hvis ingen match */
    val kobletSkjemaId: UUID?
)

/**
 * Service for kobling av separate søknader (arbeidsgiver-del og arbeidstaker-del).
 *
 * Når utsendt arbeidstaker-søknad sendes inn av to ulike parter (arbeidsgiver og arbeidstaker),
 * må disse kobles sammen for å havne i samme sak i melosys-api.
 *
 * Matching-kriterier:
 * 1. Status = SENDT (kun innsendte skjemaer)
 * 2. Ikke allerede koblet (kobletSkjemaId er null)
 * 3. Motsatt skjemadel (AG søker AT, AT søker AG)
 * 4. Samme arbeidstaker-fnr
 * 5. Samme juridisk enhet orgnr
 * 6. Overlappende perioder (minst 1 dag felles)
 */
@Service
class SkjemaKoblingService(
    private val skjemaRepository: SkjemaRepository,
    private val jsonMapper: JsonMapper
) {

    /**
     * Finner og kobler matchende skjema for det innsendte skjemaet.
     *
     * @param skjema Det innsendte skjemaet som skal kobles
     * @return KoblingsResultat med kobletSkjemaId hvis match ble funnet
     */
    @Transactional
    fun finnOgKoblMotpart(skjema: Skjema): KoblingsResultat {
        val metadata = jsonMapper.parseUtsendtArbeidstakerMetadata(skjema.metadata)

        log.info { "Søker etter matchende skjema for ${skjema.id}, skjemadel=${metadata.skjemadel})" }

        val matchendeSkjema = finnMatchendeSkjema(skjema, metadata)

        return if (matchendeSkjema != null) {
            log.info { "Fant matchende skjema ${matchendeSkjema.id} for ${skjema.id}" }
            utforKobling(skjema, matchendeSkjema)
            KoblingsResultat(kobletSkjemaId = matchendeSkjema.id)
        } else {
            log.info { "Ingen matchende skjema funnet for ${skjema.id}" }
            KoblingsResultat(kobletSkjemaId = null)
        }
    }

    /**
     * Finner matchende skjema basert på koblingskriteriene.
     */
    private fun finnMatchendeSkjema(skjema: Skjema, metadata: UtsendtArbeidstakerMetadata): Skjema? {
        val motsattSkjemadel = when (metadata.skjemadel) {
            Skjemadel.ARBEIDSTAKERS_DEL -> Skjemadel.ARBEIDSGIVERS_DEL
            Skjemadel.ARBEIDSGIVERS_DEL -> Skjemadel.ARBEIDSTAKERS_DEL
        }

        // Hent kandidater: samme fnr, status=SENDT
        val kandidater = skjemaRepository.findByFnrAndStatus(skjema.fnr, SkjemaStatus.SENDT)
            .filter { it.id != skjema.id }

        log.debug { "Fant ${kandidater.size} kandidater for kobling" }

        // Filtrer på kriteriene
        return kandidater.find { kandidat ->
            val kandidatMetadata = jsonMapper.parseUtsendtArbeidstakerMetadata(kandidat.metadata)

            // 1. Ikke allerede koblet
            val ikkeKoblet = kandidatMetadata.kobletSkjemaId == null
            if (!ikkeKoblet) {
                log.debug { "Kandidat ${kandidat.id} allerede koblet til ${kandidatMetadata.kobletSkjemaId}" }
                return@find false
            }

            // 2. Motsatt skjemadel
            val erMotsattDel = kandidatMetadata.skjemadel == motsattSkjemadel
            if (!erMotsattDel) {
                log.debug { "Kandidat ${kandidat.id} har samme skjemadel (${kandidatMetadata.skjemadel})" }
                return@find false
            }

            // 3. Samme juridisk enhet
            val sammeJuridiskEnhet = kandidatMetadata.juridiskEnhetOrgnr == metadata.juridiskEnhetOrgnr
            if (!sammeJuridiskEnhet) {
                log.debug { "Kandidat ${kandidat.id} har annen juridisk enhet (${kandidatMetadata.juridiskEnhetOrgnr} vs ${metadata.juridiskEnhetOrgnr})" }
                return@find false
            }

            // 4. Overlappende perioder
            val perioderOverlapper = sjekkPerioderOverlapper(skjema, kandidat, metadata.skjemadel)
            if (!perioderOverlapper) {
                log.debug { "Kandidat ${kandidat.id} har ikke overlappende perioder" }
                return@find false
            }

            log.debug { "Kandidat ${kandidat.id} matcher alle kriterier" }
            true
        }
    }

    /**
     * Sjekker om periodene i de to skjemaene overlapper.
     */
    private fun sjekkPerioderOverlapper(
        skjema: Skjema,
        kandidat: Skjema,
        skjemadel: Skjemadel
    ): Boolean {
        return try {
            val (arbeidstakersDel, arbeidsgiversDel) = when (skjemadel) {
                Skjemadel.ARBEIDSTAKERS_DEL -> skjema to kandidat
                Skjemadel.ARBEIDSGIVERS_DEL -> kandidat to skjema
            }

            val arbeidstakerPeriode = jsonMapper.parseArbeidstakersSkjemaDataDto(arbeidstakersDel.data!!)
                .utenlandsoppdraget?.utsendelsePeriode
            val arbeidsgiverPeriode = jsonMapper.parseArbeidsgiversSkjemaDataDto(arbeidsgiversDel.data!!)
                .utenlandsoppdraget?.arbeidstakerUtsendelsePeriode

            if (arbeidstakerPeriode == null || arbeidsgiverPeriode == null) {
                log.warn { "Kunne ikke hente perioder for sammenligning" }
                return false
            }

            arbeidstakerPeriode.overlapper(arbeidsgiverPeriode)
        } catch (e: Exception) {
            log.warn(e) { "Feil ved sammenligning av perioder" }
            false
        }
    }

    /**
     * Utfører selve koblingen ved å oppdatere metadata på begge skjemaer.
     */
    private fun utforKobling(nyttSkjema: Skjema, matchendeSkjema: Skjema) {
        // Oppdater matchende skjema med referanse til nytt skjema
        val matchendeMetadata = jsonMapper.parseUtsendtArbeidstakerMetadata(matchendeSkjema.metadata)
        val oppdatertMatchendeMetadata = matchendeMetadata.medKobletSkjemaId(nyttSkjema.id)
        matchendeSkjema.metadata = jsonMapper.valueToTree(oppdatertMatchendeMetadata)
        skjemaRepository.save(matchendeSkjema)

        log.info { "Koblet skjema ${matchendeSkjema.id} til ${nyttSkjema.id}" }

        // Oppdater nytt skjema med referanse til matchende skjema
        val nyttMetadata = jsonMapper.parseUtsendtArbeidstakerMetadata(nyttSkjema.metadata)
        val oppdatertNyttMetadata = nyttMetadata.medKobletSkjemaId(matchendeSkjema.id)
        nyttSkjema.metadata = jsonMapper.valueToTree(oppdatertNyttMetadata)
        skjemaRepository.save(nyttSkjema)

        log.info { "Koblet skjema ${nyttSkjema.id} til ${matchendeSkjema.id}" }
    }
}
