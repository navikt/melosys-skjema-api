package no.nav.melosys.skjema.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import no.nav.melosys.skjema.kafka.BrukervarselMelding
import no.nav.melosys.skjema.kafka.BrukervarselProducer
import no.nav.melosys.skjema.kafka.Varseltekst
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import org.junit.jupiter.api.Test

class ArbeidstakerVarslingServiceTest {

    private val brukervarselProducer: BrukervarselProducer = mockk(relaxed = true)
    private val skjemaRepository: SkjemaRepository = mockk()
    private val skjemaLenke = "https://test.nav.no/melosys-skjema/utsendt-arbeidstaker"

    private val service = ArbeidstakerVarslingService(brukervarselProducer, skjemaRepository, skjemaLenke)

    @Test
    fun `AG uten fullmakt og ingen AT-utkast skal sende varsel med link`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)
        every { skjemaRepository.findArbeidstakerUtkastByFnrOgJuridiskEnhet(any(), any()) } returns emptyList()

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify {
            brukervarselProducer.sendBrukervarsel(
                match<BrukervarselMelding> { melding ->
                    melding.ident == skjema.fnr &&
                    melding.tekster.size == 2 &&
                    melding.tekster.any { it.språk == Språk.NORSK_BOKMAL && it.default } &&
                    melding.tekster.any { it.språk == Språk.ENGELSK } &&
                    melding.link == skjemaLenke
                }
            )
        }
    }

    @Test
    fun `AG uten fullmakt med eksisterende AT-utkast skal IKKE sende varsel`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        )
        val eksisterendeUtkast = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.UTKAST,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)
        every { skjemaRepository.findArbeidstakerUtkastByFnrOgJuridiskEnhet(any(), any()) } returns listOf(eksisterendeUtkast)

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
    }

    @Test
    fun `Radgiver uten fullmakt og ingen AT-utkast skal sende varsel med link`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)
        every { skjemaRepository.findArbeidstakerUtkastByFnrOgJuridiskEnhet(any(), any()) } returns emptyList()

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify {
            brukervarselProducer.sendBrukervarsel(match<BrukervarselMelding> { it.link == skjemaLenke })
        }
    }

    @Test
    fun `AG med fullmakt skal sende informasjonsvarsel uten link`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify {
            brukervarselProducer.sendBrukervarsel(match<BrukervarselMelding> { it.link == null })
        }
    }

    @Test
    fun `Radgiver med fullmakt skal sende informasjonsvarsel uten link`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify {
            brukervarselProducer.sendBrukervarsel(match<BrukervarselMelding> { it.link == null })
        }
    }

    @Test
    fun `DEG_SELV skal ikke sende varsel`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
    }

    @Test
    fun `ANNEN_PERSON skal ikke sende varsel`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ANNEN_PERSON,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
    }

    @Test
    fun `Feil i BrukervarselProducer skal fanges og ikke kastes videre`() {
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)
        every { skjemaRepository.findArbeidstakerUtkastByFnrOgJuridiskEnhet(any(), any()) } returns emptyList()
        every { brukervarselProducer.sendBrukervarsel(any()) } throws RuntimeException("Kafka nede")

        // Skal ikke kaste exception
        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)
    }

    @Test
    fun `Varseltekst skal inneholde arbeidsgiverNavn og orgnr`() {
        val arbeidsgiverNavn = "Test Arbeidsgiver AS"
        val skjema = skjemaMedDefaultVerdier(
            id = UUID.randomUUID(),
            status = SkjemaStatus.SENDT,
            orgnr = korrektSyntetiskOrgnr,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = arbeidsgiverNavn
            )
        )
        every { skjemaRepository.findById(skjema.id!!) } returns Optional.of(skjema)
        every { skjemaRepository.findArbeidstakerUtkastByFnrOgJuridiskEnhet(any(), any()) } returns emptyList()

        service.varsleArbeidstakerHvisAktuelt(skjema.id!!)

        verify {
            brukervarselProducer.sendBrukervarsel(
                match<BrukervarselMelding> { melding ->
                    val nbTekst = melding.tekster.first { it.språk == Språk.NORSK_BOKMAL }.tekst
                    nbTekst.contains(arbeidsgiverNavn) && nbTekst.contains(korrektSyntetiskOrgnr)
                }
            )
        }
    }

    @Test
    fun `Ikke-eksisterende skjemaId skal ikke kaste exception`() {
        val skjemaId = UUID.randomUUID()
        every { skjemaRepository.findById(skjemaId) } returns Optional.empty()

        // Skal ikke kaste exception
        service.varsleArbeidstakerHvisAktuelt(skjemaId)

        verify(exactly = 0) { brukervarselProducer.sendBrukervarsel(any()) }
    }
}
