package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class SkjemaKoblingServiceTest : FunSpec({

    val mockSkjemaRepository = mockk<SkjemaRepository>(relaxed = true)
    val jsonMapper: JsonMapper = JsonMapper.builder().addModule(kotlinModule()).build()

    val service = SkjemaKoblingService(
        mockSkjemaRepository,
        jsonMapper
    )

    val juridiskEnhetOrgnr = "999888777"
    val arbeidstakerFnr = korrektSyntetiskFnr

    context("finnOgKoblMotpart") {
        test("skal returnere null når ingen kandidater finnes") {
            val skjemaId = UUID.randomUUID()
            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns emptyList()

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe null
                    }

        test("skal returnere null når kandidat allerede er koblet") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )

            // Kandidat som allerede er koblet til et annet skjema
            val kandidatMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                kobletSkjemaId = UUID.randomUUID() // Allerede koblet
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe null
        }

        test("skal returnere null når kandidat har samme skjemadel") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )

            // Kandidat med samme skjemadel
            val kandidatMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL, // Samme som skjema
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe null
        }

        test("skal returnere null når kandidat har annen juridisk enhet") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata
            )

            // Kandidat med annen juridisk enhet
            val kandidatMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                juridiskEnhetOrgnr = "111222333" // Annen juridisk enhet
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe null
        }

        test("skal koble skjemaer med matchende kriterier") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

            // Overlappende perioder
            val overlappendePeriode = PeriodeDto(
                fraDato = LocalDate.of(2024, 1, 1),
                tilDato = LocalDate.of(2024, 12, 31)
            )

            // Arbeidstakers del (nytt skjema)
            val arbeidstakerData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = overlappendePeriode
                )
            )

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            // Arbeidsgivers del (kandidat)
            val arbeidsgiverData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    arbeidstakerUtsendelsePeriode = overlappendePeriode
                )
            )

            val kandidatMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata,
                data = jsonMapper.valueToTree(arbeidsgiverData)
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)
            every { mockSkjemaRepository.save(any()) } returnsArgument 0

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe kandidatId

            // Verifiser at begge skjemaer ble lagret med kobling
            verify(exactly = 2) { mockSkjemaRepository.save(any()) }
        }

        test("skal returnere null når perioder ikke overlapper") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

            // Arbeidstakers periode: jan-jun 2024
            val arbeidstakerPeriode = PeriodeDto(
                fraDato = LocalDate.of(2024, 1, 1),
                tilDato = LocalDate.of(2024, 6, 30)
            )

            // Arbeidsgivers periode: jul-des 2024 (ingen overlapp)
            val arbeidsgiverPeriode = PeriodeDto(
                fraDato = LocalDate.of(2024, 7, 1),
                tilDato = LocalDate.of(2024, 12, 31)
            )

            val arbeidstakerData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = arbeidstakerPeriode
                )
            )

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            val arbeidsgiverData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    arbeidstakerUtsendelsePeriode = arbeidsgiverPeriode
                )
            )

            val kandidatMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata,
                data = jsonMapper.valueToTree(arbeidsgiverData)
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe null
        }

        test("skal koble når perioder overlapper med kun 1 dag") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

            // Arbeidstakers periode: jan-jun 2024
            val arbeidstakerPeriode = PeriodeDto(
                fraDato = LocalDate.of(2024, 1, 1),
                tilDato = LocalDate.of(2024, 6, 30)
            )

            // Arbeidsgivers periode: 30 jun - des 2024 (1 dag overlapp)
            val arbeidsgiverPeriode = PeriodeDto(
                fraDato = LocalDate.of(2024, 6, 30),
                tilDato = LocalDate.of(2024, 12, 31)
            )

            val arbeidstakerData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = arbeidstakerPeriode
                )
            )

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            val arbeidsgiverData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    arbeidstakerUtsendelsePeriode = arbeidsgiverPeriode
                )
            )

            val kandidatMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata,
                data = jsonMapper.valueToTree(arbeidsgiverData)
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)
            every { mockSkjemaRepository.save(any()) } returnsArgument 0

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe kandidatId
        }

        test("skal ikke inkludere seg selv som kandidat") {
            val skjemaId = UUID.randomUUID()

            val overlappendePeriode = PeriodeDto(
                fraDato = LocalDate.of(2024, 1, 1),
                tilDato = LocalDate.of(2024, 12, 31)
            )

            val arbeidstakerData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = overlappendePeriode
                )
            )

            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val skjema = skjemaMedDefaultVerdier(
                id = skjemaId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = metadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            // Repository returnerer kun skjemaet selv
            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(skjema)

            val resultat = service.finnOgKoblMotpart(skjema)

            resultat.kobletSkjemaId shouldBe null
        }
    }
})
