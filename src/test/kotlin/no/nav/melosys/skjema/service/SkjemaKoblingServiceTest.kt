package no.nav.melosys.skjema.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
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

    val overlappendePeriode = PeriodeDto(
        fraDato = LocalDate.of(2024, 1, 1),
        tilDato = LocalDate.of(2024, 12, 31)
    )

    context("motpart-kobling") {
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

            val resultat = service.finnOgKobl(skjema)

            resultat.kobletSkjemaId shouldBe null
            resultat.erstatterSkjemaId shouldBe null
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
                kobletSkjemaId = UUID.randomUUID()
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)

            val resultat = service.finnOgKobl(skjema)

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

            val resultat = service.finnOgKobl(skjema)

            // Samme del → erstatter-match, ikke motpart. Ingen kobling fordi erstatter ikke har arvet kobling.
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
                juridiskEnhetOrgnr = "111222333"
            )

            val kandidat = skjemaMedDefaultVerdier(
                id = kandidatId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = kandidatMetadata
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(kandidat)

            val resultat = service.finnOgKobl(skjema)

            resultat.kobletSkjemaId shouldBe null
        }

        test("skal koble skjemaer med matchende kriterier") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

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

            val resultat = service.finnOgKobl(skjema)

            resultat.kobletSkjemaId shouldBe kandidatId
            resultat.erstatterSkjemaId shouldBe null
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

            val resultat = service.finnOgKobl(skjema)

            resultat.kobletSkjemaId shouldBe null
        }

        test("skal koble når perioder overlapper med kun 1 dag") {
            val skjemaId = UUID.randomUUID()
            val kandidatId = UUID.randomUUID()

            val arbeidstakerPeriode = PeriodeDto(
                fraDato = LocalDate.of(2024, 1, 1),
                tilDato = LocalDate.of(2024, 6, 30)
            )
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

            val resultat = service.finnOgKobl(skjema)

            resultat.kobletSkjemaId shouldBe kandidatId
        }

        test("skal ikke inkludere seg selv som kandidat") {
            val skjemaId = UUID.randomUUID()

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

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(skjema)

            val resultat = service.finnOgKobl(skjema)

            resultat.kobletSkjemaId shouldBe null
            resultat.erstatterSkjemaId shouldBe null
        }
    }

    context("erstatter-kobling") {
        test("skal finne forrige versjon av samme arbeidstaker-del") {
            val gammelId = UUID.randomUUID()
            val nyId = UUID.randomUUID()

            val arbeidstakerData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = overlappendePeriode
                )
            )

            val gammelMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val gammelSkjema = skjemaMedDefaultVerdier(
                id = gammelId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = gammelMetadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            val nyMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val nyttSkjema = skjemaMedDefaultVerdier(
                id = nyId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = nyMetadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(gammelSkjema)
            every { mockSkjemaRepository.save(any()) } returnsArgument 0

            val resultat = service.finnOgKobl(nyttSkjema)

            resultat.erstatterSkjemaId shouldBe gammelId
            resultat.kobletSkjemaId shouldBe null
        }

        test("skal arve kobling fra forrige versjon") {
            val gammelId = UUID.randomUUID()
            val nyId = UUID.randomUUID()
            val motpartId = UUID.randomUUID()

            val arbeidstakerData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = overlappendePeriode
                )
            )

            // Gammel versjon er allerede koblet til motpart
            val gammelMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                kobletSkjemaId = motpartId
            )

            val gammelSkjema = skjemaMedDefaultVerdier(
                id = gammelId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = gammelMetadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            val nyMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val nyttSkjema = skjemaMedDefaultVerdier(
                id = nyId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = nyMetadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            // Motpart-skjema
            val motpartMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                kobletSkjemaId = gammelId
            )

            val motpartSkjema = skjemaMedDefaultVerdier(
                id = motpartId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = motpartMetadata,
                data = jsonMapper.valueToTree(arbeidsgiversSkjemaDataDtoMedDefaultVerdier())
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(gammelSkjema, motpartSkjema)
            every { mockSkjemaRepository.findById(motpartId) } returns Optional.of(motpartSkjema)
            every { mockSkjemaRepository.save(any()) } returnsArgument 0

            val resultat = service.finnOgKobl(nyttSkjema)

            resultat.erstatterSkjemaId shouldBe gammelId
            resultat.kobletSkjemaId shouldBe motpartId
        }

        test("skal ikke matche erstatter med annen juridisk enhet") {
            val gammelId = UUID.randomUUID()
            val nyId = UUID.randomUUID()

            val arbeidstakerData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = overlappendePeriode
                )
            )

            val gammelMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = "111222333" // Annen juridisk enhet
            )

            val gammelSkjema = skjemaMedDefaultVerdier(
                id = gammelId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = gammelMetadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            val nyMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val nyttSkjema = skjemaMedDefaultVerdier(
                id = nyId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = nyMetadata,
                data = jsonMapper.valueToTree(arbeidstakerData)
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(gammelSkjema)

            val resultat = service.finnOgKobl(nyttSkjema)

            resultat.erstatterSkjemaId shouldBe null
        }

        test("skal ikke matche erstatter med ikke-overlappende perioder") {
            val gammelId = UUID.randomUUID()
            val nyId = UUID.randomUUID()

            val gammelData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = PeriodeDto(
                        fraDato = LocalDate.of(2023, 1, 1),
                        tilDato = LocalDate.of(2023, 6, 30)
                    )
                )
            )

            val nyData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
                utenlandsoppdraget = arbeidstakersSkjemaDataDtoMedDefaultVerdier().utenlandsoppdraget!!.copy(
                    utsendelsePeriode = overlappendePeriode
                )
            )

            val gammelMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val gammelSkjema = skjemaMedDefaultVerdier(
                id = gammelId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = gammelMetadata,
                data = jsonMapper.valueToTree(gammelData)
            )

            val nyMetadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = Representasjonstype.DEG_SELV,
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )

            val nyttSkjema = skjemaMedDefaultVerdier(
                id = nyId,
                fnr = arbeidstakerFnr,
                status = SkjemaStatus.SENDT,
                metadata = nyMetadata,
                data = jsonMapper.valueToTree(nyData)
            )

            every { mockSkjemaRepository.findByFnrAndStatus(arbeidstakerFnr, SkjemaStatus.SENDT) } returns listOf(gammelSkjema)

            val resultat = service.finnOgKobl(nyttSkjema)

            resultat.erstatterSkjemaId shouldBe null
        }
    }
})
