package no.nav.melosys.skjema.service

import io.getunleash.Unleash
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.extensions.utsendelsePeriode
import no.nav.melosys.skjema.featuretoggle.ToggleNavn
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.VentendeMotpartSoknadDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.VentendeMotpartSoknaderResponse
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

/**
 * Finner innsendte arbeidsgiver-deler som venter på at innlogget bruker (som arbeidstaker)
 * sender inn sin del. Brukes av motpart-CTA-en på oversikten i melosys-skjema-web.
 *
 * En arbeidsgiver-del regnes som ventende når ALLE kriteriene under er oppfylt:
 * - status SENDT og `skjemadel == ARBEIDSGIVERS_DEL` med innlogget brukers fnr som arbeidstaker
 * - ikke koblet til en arbeidstaker-del (`kobletSkjemaId == null`)
 * - ikke erstattet av en nyere versjon (kun siste versjon i en erstatter-kjede vises)
 * - saken er ikke avsluttet i Melosys (`innsending.saksstatus != AVSLUTTET` — motparten kan ha
 *   sendt via annen kanal uten at kobling finnes)
 * - brukeren har ikke allerede et arbeidstaker-utkast for samme juridiske enhet (samme guard
 *   som [ArbeidstakerVarslingService] bruker for å unngå purring av de som er i gang)
 */
@Service
class VentendeMotpartSoknadService(
    private val skjemaRepository: SkjemaRepository,
    private val innsendingRepository: InnsendingRepository,
    private val subjectHandler: SubjectHandler,
    private val unleash: Unleash
) {

    fun hentVentendeMotpartSoknader(): VentendeMotpartSoknaderResponse {
        if (!unleash.isEnabled(ToggleNavn.MOTPART_CTA.navn)) {
            return VentendeMotpartSoknaderResponse(emptyList())
        }

        val innloggetBrukerFnr = subjectHandler.getUserID()
        val sendte = skjemaRepository.findByFnrAndTypeAndStatus(innloggetBrukerFnr, SkjemaType.UTSENDT_ARBEIDSTAKER, SkjemaStatus.SENDT)

        val erstattedeIder = sendte
            .mapNotNull { (it.metadata as? UtsendtArbeidstakerMetadata)?.erstatterSkjemaId }
            .toSet()

        val juridiskeEnheterMedArbeidstakerUtkast = skjemaRepository
            .findByFnrAndTypeAndStatus(innloggetBrukerFnr, SkjemaType.UTSENDT_ARBEIDSTAKER, SkjemaStatus.UTKAST)
            .mapNotNull { utkast ->
                (utkast.metadata as? UtsendtArbeidstakerMetadata)
                    ?.takeIf { it.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }
                    ?.juridiskEnhetOrgnr
            }
            .toSet()

        val ventende = sendte
            .mapNotNull { skjema ->
                val id = skjema.id ?: return@mapNotNull null
                val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata ?: return@mapNotNull null

                if (metadata.skjemadel != Skjemadel.ARBEIDSGIVERS_DEL) return@mapNotNull null
                if (metadata.kobletSkjemaId != null) return@mapNotNull null
                if (id in erstattedeIder) return@mapNotNull null
                if (metadata.juridiskEnhetOrgnr in juridiskeEnheterMedArbeidstakerUtkast) return@mapNotNull null
                if (innsendingRepository.findBySkjemaId(id)?.saksstatus == Saksstatus.AVSLUTTET) return@mapNotNull null

                VentendeMotpartSoknadDto(
                    skjemaId = id,
                    arbeidsgiverNavn = metadata.arbeidsgiverNavn,
                    arbeidsgiverOrgnr = skjema.orgnr,
                    utsendingsperiode = skjema.utsendelsePeriode(),
                    innsendtDato = skjema.endretDato
                )
            }
            .sortedByDescending { it.innsendtDato }

        log.debug { "Fant ${ventende.size} ventende motpart-søknader for innlogget bruker" }
        return VentendeMotpartSoknaderResponse(ventende)
    }
}
