package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.extensions.utsendtArbeidstakerMetadataOrThrow
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.types.HentUtkastRequest
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.UtkastListeResponse
import no.nav.melosys.skjema.types.UtkastOversiktDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class HentUtkastUtsendtArbeidstakerService(
    private val skjemaRepository: SkjemaRepository,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val subjectHandler: SubjectHandler
) {

    companion object {
        private val FULLMAKT_TYPER = setOf(
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
            Representasjonstype.RADGIVER_MED_FULLMAKT
        )
    }

    fun hentUtkast(request: HentUtkastRequest): UtkastListeResponse {
        val innloggetBrukerFnr = subjectHandler.getUserID()
        log.debug { "Henter utkast for representasjonstype: ${request.representasjonstype}" }

        val utkastSkjemaer = when (request.representasjonstype) {
            Representasjonstype.DEG_SELV -> hentForDegSelv(innloggetBrukerFnr)
            Representasjonstype.ARBEIDSGIVER,
            Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT -> hentForArbeidsgiver(innloggetBrukerFnr)

            Representasjonstype.RADGIVER,
            Representasjonstype.RADGIVER_MED_FULLMAKT -> hentForRadgiver(innloggetBrukerFnr, request.radgiverfirmaOrgnr)

            Representasjonstype.ANNEN_PERSON -> hentForAnnenPerson(innloggetBrukerFnr)
        }

        val utkastDtos = utkastSkjemaer.map { konverterTilUtkastDto(it) }

        log.debug { "Fant ${utkastDtos.size} utkast for representasjonstype ${request.representasjonstype}" }

        return UtkastListeResponse(
            utkast = utkastDtos,
            antall = utkastDtos.size
        )
    }

    private fun hentForDegSelv(innloggetBrukerFnr: String): List<Skjema> {
        return skjemaRepository.findByFnrAndTypeAndStatus(
            innloggetBrukerFnr,
            SkjemaType.UTSENDT_ARBEIDSTAKER,
            SkjemaStatus.UTKAST
        ).filter { skjema ->
            skjema.utsendtArbeidstakerMetadataOrThrow().representasjonstype == Representasjonstype.DEG_SELV
        }
    }

    private fun hentForArbeidsgiver(innloggetBrukerFnr: String): List<Skjema> {
        val tilgangOrgnr = altinnService.hentBrukersTilganger().map { it.orgnr }.toSet()
        val arbeidsgiverTyper = setOf(Representasjonstype.ARBEIDSGIVER, Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT)
        val personerMedAktivFullmakt by lazy { reprService.hentFullmaktsgiverFnr() }

        return skjemaRepository.findByOpprettetAvAndTypeAndStatus(
            innloggetBrukerFnr,
            SkjemaType.UTSENDT_ARBEIDSTAKER,
            SkjemaStatus.UTKAST
        ).filter { skjema ->
            val metadata = skjema.utsendtArbeidstakerMetadataOrThrow()
            metadata.representasjonstype in arbeidsgiverTyper
                && tilgangOrgnr.contains(skjema.orgnr)
                && (metadata.representasjonstype !in FULLMAKT_TYPER || personerMedAktivFullmakt.contains(skjema.fnr))
        }
    }

    private fun hentForRadgiver(innloggetBrukerFnr: String, radgiverfirmaOrgnr: String?): List<Skjema> {
        requireNotNull(radgiverfirmaOrgnr) { "radgiverfirmaOrgnr er påkrevd for RADGIVER" }
        val radgiverTyper = setOf(Representasjonstype.RADGIVER, Representasjonstype.RADGIVER_MED_FULLMAKT)
        val personerMedAktivFullmakt by lazy { reprService.hentFullmaktsgiverFnr() }

        return skjemaRepository.findByOpprettetAvAndTypeAndStatus(
            innloggetBrukerFnr,
            SkjemaType.UTSENDT_ARBEIDSTAKER,
            SkjemaStatus.UTKAST
        ).filter { skjema ->
            val metadata = skjema.metadata as? UtsendtArbeidstakerMetadata
                ?: return@filter false

            metadata.representasjonstype in radgiverTyper
                && when (metadata) {
                    is RadgiverMetadata -> metadata.radgiverfirma.orgnr == radgiverfirmaOrgnr
                    is RadgiverMedFullmaktMetadata -> metadata.radgiverfirma.orgnr == radgiverfirmaOrgnr
                    else -> false
                }
                && (metadata.representasjonstype !in FULLMAKT_TYPER || personerMedAktivFullmakt.contains(skjema.fnr))
        }
    }

    private fun hentForAnnenPerson(innloggetBrukerFnr: String): List<Skjema> {
        val personerMedFullmaktFnr = reprService.hentFullmaktsgiverFnr()

        return skjemaRepository.findByOpprettetAvAndTypeAndStatus(
            innloggetBrukerFnr,
            SkjemaType.UTSENDT_ARBEIDSTAKER,
            SkjemaStatus.UTKAST
        ).filter { skjema ->
            val metadata = skjema.utsendtArbeidstakerMetadataOrThrow()
            metadata.representasjonstype == Representasjonstype.ANNEN_PERSON
                && personerMedFullmaktFnr.contains(skjema.fnr)
        }
    }

    private fun konverterTilUtkastDto(skjema: Skjema): UtkastOversiktDto {
        val metadata = skjema.utsendtArbeidstakerMetadataOrThrow()

        return UtkastOversiktDto(
            id = skjema.id ?: throw IllegalStateException("Skjema ID er null"),
            arbeidsgiverNavn = metadata.arbeidsgiverNavn,
            arbeidsgiverOrgnr = skjema.orgnr,
            arbeidstakerNavn = null,
            arbeidstakerFnrMaskert = maskerFnr(skjema.fnr),
            opprettetDato = skjema.opprettetDato,
            sistEndretDato = skjema.endretDato,
            status = skjema.status
        )
    }

    private fun maskerFnr(fnr: String): String {
        return if (fnr.length == 11) fnr.substring(0, 6) + "*****" else "***********"
    }
}
