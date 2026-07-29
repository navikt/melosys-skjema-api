package no.nav.melosys.skjema.service

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import java.util.UUID
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.etAnnetKorrektSyntetiskFnr
import no.nav.melosys.skjema.featuretoggle.ToggleNavn
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.periodeDtoMedDefaultVerdier
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VentendeMotpartSoknadServiceIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var service: VentendeMotpartSoknadService

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var unleash: Unleash

    @MockkBean
    private lateinit var subjectHandler: SubjectHandler

    private val arbeidstakerFnr = korrektSyntetiskFnr
    private val annenJuridiskEnhetOrgnr = "974761076"

    @BeforeEach
    fun setUp() {
        clearMocks(subjectHandler)
        every { subjectHandler.getUserID() } returns arbeidstakerFnr
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
        (unleash as FakeUnleash).enableAll()
    }

    @AfterEach
    fun tearDown() {
        (unleash as FakeUnleash).enableAll()
    }

    @Test
    @DisplayName("Ukoblet innsendt arbeidsgiver-del gir treff med arbeidsgiver, periode og innsendt dato")
    fun `ukoblet arbeidsgiver-del gir treff`() {
        val skjema = lagreSendtArbeidsgiverDel()

        val response = service.hentVentendeMotpartSoknader()

        response.soknader shouldHaveSize 1
        with(response.soknader.single()) {
            skjemaId shouldBe skjema.id
            arbeidsgiverNavn shouldBe "Test Arbeidsgiver AS"
            arbeidsgiverOrgnr shouldBe korrektSyntetiskOrgnr
            utsendingsperiode shouldBe periodeDtoMedDefaultVerdier()
            innsendtDato shouldBe skjemaRepository.findById(skjema.id!!).orElseThrow().endretDato
        }
    }

    @Test
    @DisplayName("Toggle av gir alltid tom liste")
    fun `toggle av gir tom liste`() {
        lagreSendtArbeidsgiverDel()
        (unleash as FakeUnleash).enableAllExcept(ToggleNavn.MOTPART_CTA.navn)

        service.hentVentendeMotpartSoknader().soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("Koblet arbeidsgiver-del gir ikke treff")
    fun `koblet arbeidsgiver-del gir ikke treff`() {
        lagreSendtArbeidsgiverDel(kobletSkjemaId = UUID.randomUUID())

        service.hentVentendeMotpartSoknader().soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("Arbeidsgiver-del erstattet av nyere versjon gir kun treff for nyeste")
    fun `erstattet versjon gir kun treff for nyeste`() {
        val gammel = lagreSendtArbeidsgiverDel()
        val ny = lagreSendtArbeidsgiverDel(erstatterSkjemaId = gammel.id)

        val response = service.hentVentendeMotpartSoknader()

        response.soknader shouldHaveSize 1
        response.soknader.single().skjemaId shouldBe ny.id
    }

    @Test
    @DisplayName("Erstatter-kjede over flere ledd gir kun treff for nyeste versjon")
    fun `erstatter-kjede over flere ledd gir kun treff for nyeste`() {
        val a = lagreSendtArbeidsgiverDel()
        val b = lagreSendtArbeidsgiverDel(erstatterSkjemaId = a.id)
        val c = lagreSendtArbeidsgiverDel(erstatterSkjemaId = b.id)

        val response = service.hentVentendeMotpartSoknader()

        response.soknader shouldHaveSize 1
        response.soknader.single().skjemaId shouldBe c.id
    }

    @Test
    @DisplayName("Arbeidsgiver-del uten utsendingsperiode gir treff uten periode")
    fun `arbeidsgiver-del uten utsendingsperiode gir treff uten periode`() {
        val skjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = arbeidstakerFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER,
                    skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                )
            )
        )

        val response = service.hentVentendeMotpartSoknader()

        response.soknader shouldHaveSize 1
        with(response.soknader.single()) {
            skjemaId shouldBe skjema.id
            utsendingsperiode shouldBe null
        }
    }

    @Test
    @DisplayName("Innsending uten synket saksstatus gir treff")
    fun `innsending uten synket saksstatus gir treff`() {
        val skjema = lagreSendtArbeidsgiverDel()
        innsendingRepository.save(innsendingMedDefaultVerdier(skjema = skjema, saksstatus = null))

        service.hentVentendeMotpartSoknader().soknader shouldHaveSize 1
    }

    @Test
    @DisplayName("Avsluttet sak gir ikke treff, mottatt sak gir treff")
    fun `avsluttet sak gir ikke treff`() {
        val avsluttet = lagreSendtArbeidsgiverDel()
        innsendingRepository.save(innsendingMedDefaultVerdier(skjema = avsluttet, saksstatus = Saksstatus.AVSLUTTET))

        val mottatt = lagreSendtArbeidsgiverDel(juridiskEnhetOrgnr = annenJuridiskEnhetOrgnr)
        innsendingRepository.save(innsendingMedDefaultVerdier(skjema = mottatt, saksstatus = Saksstatus.MOTTATT))

        val response = service.hentVentendeMotpartSoknader()

        response.soknader shouldHaveSize 1
        response.soknader.single().skjemaId shouldBe mottatt.id
    }

    @Test
    @DisplayName("Eksisterende arbeidstaker-utkast for samme juridiske enhet gir ikke treff")
    fun `arbeidstaker-utkast for samme juridiske enhet gir ikke treff`() {
        lagreSendtArbeidsgiverDel()
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.UTKAST,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                )
            )
        )

        service.hentVentendeMotpartSoknader().soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("Arbeidstaker-utkast for annen juridisk enhet gir fortsatt treff")
    fun `arbeidstaker-utkast for annen juridisk enhet gir fortsatt treff`() {
        lagreSendtArbeidsgiverDel()
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.UTKAST,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    juridiskEnhetOrgnr = annenJuridiskEnhetOrgnr
                )
            )
        )

        service.hentVentendeMotpartSoknader().soknader shouldHaveSize 1
    }

    @Test
    @DisplayName("Egen innsendt arbeidstaker-del og kombinert del gir ikke treff")
    fun `andre skjemadeler gir ikke treff`() {
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV,
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                )
            )
        )
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
                    skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                )
            )
        )

        service.hentVentendeMotpartSoknader().soknader.shouldBeEmpty()
    }

    @Test
    @DisplayName("Arbeidsgiver-del for annen arbeidstaker gir ikke treff")
    fun `annen arbeidstakers arbeidsgiver-del gir ikke treff`() {
        lagreSendtArbeidsgiverDel(fnr = etAnnetKorrektSyntetiskFnr)

        service.hentVentendeMotpartSoknader().soknader.shouldBeEmpty()
    }

    private fun lagreSendtArbeidsgiverDel(
        fnr: String = arbeidstakerFnr,
        kobletSkjemaId: UUID? = null,
        erstatterSkjemaId: UUID? = null,
        juridiskEnhetOrgnr: String = korrektSyntetiskOrgnr
    ): Skjema = skjemaRepository.save(
        skjemaMedDefaultVerdier(
            fnr = fnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().medUtsendingsperiode(),
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                kobletSkjemaId = kobletSkjemaId,
                erstatterSkjemaId = erstatterSkjemaId,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )
        )
    )

    private fun UtsendtArbeidstakerArbeidsgiversSkjemaDataDto.medUtsendingsperiode(): UtsendtArbeidstakerArbeidsgiversSkjemaDataDto =
        copy(
            utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                utsendelseLand = LandKode.SE,
                utsendelsePeriode = periodeDtoMedDefaultVerdier()
            )
        )
}
