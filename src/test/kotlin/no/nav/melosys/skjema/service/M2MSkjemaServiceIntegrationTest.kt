package no.nav.melosys.skjema.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class M2MSkjemaServiceIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var m2mSkjemaService: M2MSkjemaService

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    private val overlappendePeriode = PeriodeDto(
        fraDato = LocalDate.of(2024, 1, 1),
        tilDato = LocalDate.of(2024, 12, 31)
    )

    private val arbeidstakersDataMedOverlappendePeriode = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
        utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier().copy(
            utsendelsePeriode = overlappendePeriode
        )
    )

    private val arbeidsgiversDataMedOverlappendePeriode = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
        utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier().copy(
            arbeidstakerUtsendelsePeriode = overlappendePeriode
        )
    )

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
    }

    @Test
    fun `returnerer arbeidstakersDel og arbeidsgiversDel via kobletSkjemaId`() {
        // Lagre arbeidsgiver-del
        val arbeidsgiversSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidsgiversDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        ))

        // Lagre arbeidstaker-del med kobling til arbeidsgiver
        val arbeidstakersSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                kobletSkjemaId = arbeidsgiversSkjema.id
            )
        ))

        innsendingRepository.save(innsendingMedDefaultVerdier(
            skjema = arbeidstakersSkjema,
            status = InnsendingStatus.FERDIG,
            referanseId = "MEL-TEST01"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        result.referanseId shouldBe "MEL-TEST01"
        result.skjemaer shouldHaveSize 2

        // Finn arbeidstakers og arbeidsgivers skjema fra listen
        val arbeidstakerSkjemaDto = result.skjemaer.find {
            (it.metadata as UtsendtArbeidstakerMetadata).skjemadel == Skjemadel.ARBEIDSTAKERS_DEL
        }!!
        val arbeidsgiversSkjemaDto = result.skjemaer.find {
            (it.metadata as UtsendtArbeidstakerMetadata).skjemadel == Skjemadel.ARBEIDSGIVERS_DEL
        }!!

        arbeidstakerSkjemaDto.id shouldBe arbeidstakersSkjema.id
        (arbeidstakerSkjemaDto.data as UtsendtArbeidstakerArbeidstakersSkjemaDataDto).utenlandsoppdraget shouldBe arbeidstakersDataMedOverlappendePeriode.utenlandsoppdraget
        arbeidsgiversSkjemaDto.id shouldBe arbeidsgiversSkjema.id
        (arbeidsgiversSkjemaDto.data as UtsendtArbeidstakerArbeidsgiversSkjemaDataDto).utenlandsoppdraget shouldBe arbeidsgiversDataMedOverlappendePeriode.utenlandsoppdraget
    }

    @Test
    fun `returnerer kun arbeidstakersDel når ingen kobling finnes`() {
        val arbeidstakersSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
            )
        ))

        innsendingRepository.save(innsendingMedDefaultVerdier(
            skjema = arbeidstakersSkjema,
            status = InnsendingStatus.FERDIG,
            referanseId = "MEL-TEST02"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        result.referanseId shouldBe "MEL-TEST02"
        result.skjemaer shouldHaveSize 1
        result.skjemaer[0].id shouldBe arbeidstakersSkjema.id
    }

    @Test
    fun `returnerer kun arbeidsgiversDel når det er hovedskjema uten kobling`() {
        val arbeidsgiversSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidsgiversDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        ))

        innsendingRepository.save(innsendingMedDefaultVerdier(
            skjema = arbeidsgiversSkjema,
            status = InnsendingStatus.FERDIG,
            referanseId = "MEL-TEST04"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidsgiversSkjema.id!!)

        result.referanseId shouldBe "MEL-TEST04"
        result.skjemaer shouldHaveSize 1
        result.skjemaer[0].id shouldBe arbeidsgiversSkjema.id
    }

    @Test
    fun `inkluderer erstatterSkjemaId i metadata når skjema erstatter et annet`() {
        val gammelSkjemaId = java.util.UUID.randomUUID()

        val arbeidstakersSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                erstatterSkjemaId = gammelSkjemaId
            )
        ))

        innsendingRepository.save(innsendingMedDefaultVerdier(
            skjema = arbeidstakersSkjema,
            status = InnsendingStatus.FERDIG,
            referanseId = "MEL-TEST05"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        result.skjemaer shouldHaveSize 1
        (result.skjemaer[0].metadata as UtsendtArbeidstakerMetadata).erstatterSkjemaId shouldBe gammelSkjemaId
    }
}
