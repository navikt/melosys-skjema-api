package no.nav.melosys.skjema.service

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.felles.LandKode
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
        utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
            utsendelseLand = LandKode.SE,
            utsendelsePeriode = overlappendePeriode
        )
    )

    private val arbeidsgiversDataMedOverlappendePeriode = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
        utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
            utsendelseLand = LandKode.SE,
            utsendelsePeriode = overlappendePeriode
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
            referanseId = "TEST01"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        result.referanseId shouldBe "TEST01"
        result.tidligereInnsendteSkjema shouldBe emptyList()

        val kobletSkjemaDto = result.kobletSkjema.shouldNotBeNull()
        kobletSkjemaDto.id shouldBe arbeidsgiversSkjema.id
        (kobletSkjemaDto.data as UtsendtArbeidstakerArbeidsgiversSkjemaDataDto).utenlandsoppdraget shouldBe arbeidsgiversDataMedOverlappendePeriode.utenlandsoppdraget
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
            referanseId = "TEST02"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        result.referanseId shouldBe "TEST02"
        result.kobletSkjema.shouldBeNull()
        result.tidligereInnsendteSkjema shouldBe emptyList()
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
            referanseId = "TEST04"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidsgiversSkjema.id!!)

        result.referanseId shouldBe "TEST04"
        result.kobletSkjema.shouldBeNull()
        result.tidligereInnsendteSkjema shouldBe emptyList()
    }

    @Test
    fun `inkluderer erstatterSkjemaId i metadata når erstattet skjema ikke finnes i DB`() {
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
            referanseId = "TEST05"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        result.kobletSkjema.shouldBeNull()
        result.tidligereInnsendteSkjema shouldBe emptyList()
        result.skjema.metadata.erstatterSkjemaId shouldBe gammelSkjemaId
    }

    @Test
    fun `tidligereInnsendteSkjema returnerer erstatter-kjede`() {
        // v1 → v2 → v3 (v3 erstatter v2, v2 erstatter v1)
        val v1 = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
            )
        ))

        val v2 = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                erstatterSkjemaId = v1.id
            )
        ))

        val v3 = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                erstatterSkjemaId = v2.id
            )
        ))

        innsendingRepository.save(innsendingMedDefaultVerdier(
            skjema = v3,
            status = InnsendingStatus.FERDIG,
            referanseId = "TEST06"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(v3.id!!)

        result.tidligereInnsendteSkjema.size shouldBe 2
        result.tidligereInnsendteSkjema[0].id shouldBe v2.id
        result.tidligereInnsendteSkjema[1].id shouldBe v1.id
    }

    @Test
    fun `tidligereInnsendteSkjema stopper ved ikke-SENDT skjema`() {
        // v1 har status UTKAST, v2 erstatter v1, v3 erstatter v2
        val v1 = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.UTKAST,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
            )
        ))

        val v2 = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                erstatterSkjemaId = v1.id
            )
        ))

        val v3 = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = arbeidstakersDataMedOverlappendePeriode,
            metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                erstatterSkjemaId = v2.id
            )
        ))

        innsendingRepository.save(innsendingMedDefaultVerdier(
            skjema = v3,
            status = InnsendingStatus.FERDIG,
            referanseId = "TEST07"
        ))

        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(v3.id!!)

        // Kun v2 inkluderes - v1 har feil status og stoppes
        result.tidligereInnsendteSkjema.size shouldBe 1
        result.tidligereInnsendteSkjema[0].id shouldBe v2.id
    }
}
