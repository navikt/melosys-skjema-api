package no.nav.melosys.skjema.service

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.dto.Representasjonstype
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.dto.felles.PeriodeDto
import no.nav.melosys.skjema.dto.m2m.UtsendtArbeidstakerM2MSkjemaData
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.SkjemaStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

class M2MSkjemaServiceIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var m2mSkjemaService: M2MSkjemaService

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
    }

    @Test
    fun `hentUtsendtArbeidstakerSkjemaData returnerer arbeidstakersDel og arbeidsgiversDel når begge finnes med overlappende perioder`() {
        // Arrange - felles overlappende periode
        val overlappendePeriode = PeriodeDto(
            fraDato = LocalDate.of(2024, 1, 1),
            tilDato = LocalDate.of(2024, 12, 31)
        )

        // Lagre arbeidstakers skjema (DEG_SELV) med overlappende periode
        val arbeidstakersData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
            utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier().copy(
                utsendelsePeriode = overlappendePeriode
            )
        )
        val arbeidstakersSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                data = jsonMapper.valueToTree(arbeidstakersData),
                metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV
                )
            )
        )
        val arbeidstakersInnsending = innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = arbeidstakersSkjema,
                status = InnsendingStatus.FERDIG,
                referanseId = "MEL-ARB001"
            )
        )

        // Lagre arbeidsgivers skjema (ARBEIDSGIVER) med overlappende periode
        val arbeidsgiversData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
            utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier().copy(
                arbeidstakerUtsendelsePeriode = overlappendePeriode
            )
        )
        val arbeidsgiversSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                data = jsonMapper.valueToTree(arbeidsgiversData),
                metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER
                )
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = arbeidsgiversSkjema,
                status = InnsendingStatus.FERDIG,
                referanseId = "MEL-ARG001"
            )
        )

        // Act
        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        // Assert
        result shouldBe UtsendtArbeidstakerM2MSkjemaData(
            arbeidstakersDel = arbeidstakersData,
            arbeidsgiversDel = arbeidsgiversData,
            referanseId = arbeidstakersInnsending.referanseId,
            journaposteringId = "Her skal det ligge en journapostering ID"
        )
    }

    @Test
    fun `hentUtsendtArbeidstakerSkjemaData returnerer kun arbeidstakersDel når ingen arbeidsgiversDel finnes`() {
        // Arrange - lagre kun arbeidstakers skjema (DEG_SELV)
        val arbeidstakersData = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        val arbeidstakersSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                data = jsonMapper.valueToTree(arbeidstakersData),
                metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV
                )
            )
        )
        val arbeidstakersInnsending = innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = arbeidstakersSkjema,
                status = InnsendingStatus.FERDIG,
                referanseId = "MEL-ARB002"
            )
        )

        // Act
        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        // Assert
        result shouldBe UtsendtArbeidstakerM2MSkjemaData(
            arbeidstakersDel = arbeidstakersData,
            arbeidsgiversDel = null,
            referanseId = arbeidstakersInnsending.referanseId,
            journaposteringId = "Her skal det ligge en journapostering ID"
        )
    }

    @Test
    fun `hentUtsendtArbeidstakerSkjemaData returnerer kun arbeidstakersDel når arbeidsgiversDel har ikke-overlappende periode`() {
        // Arrange - arbeidstakers periode
        val arbeidstakersPeriode = PeriodeDto(
            fraDato = LocalDate.of(2024, 1, 1),
            tilDato = LocalDate.of(2024, 6, 30)
        )
        val arbeidstakersData = arbeidstakersSkjemaDataDtoMedDefaultVerdier().copy(
            utenlandsoppdraget = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier().copy(
                utsendelsePeriode = arbeidstakersPeriode
            )
        )
        val arbeidstakersSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                data = jsonMapper.valueToTree(arbeidstakersData),
                metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                    representasjonstype = Representasjonstype.DEG_SELV
                )
            )
        )
        val arbeidstakersInnsending = innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = arbeidstakersSkjema,
                status = InnsendingStatus.FERDIG,
                referanseId = "MEL-ARB003"
            )
        )

        // Lagre arbeidsgivers skjema med ikke-overlappende periode
        val ikkeOverlappendePeriode = PeriodeDto(
            fraDato = LocalDate.of(2025, 1, 1),
            tilDato = LocalDate.of(2025, 12, 31)
        )
        val arbeidsgiversData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
            utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier().copy(
                arbeidstakerUtsendelsePeriode = ikkeOverlappendePeriode
            )
        )
        skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                data = jsonMapper.valueToTree(arbeidsgiversData),
                metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER
                )
            )
        )
        innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = skjemaRepository.findAll().last { it.id != arbeidstakersSkjema.id },
                status = InnsendingStatus.FERDIG,
                referanseId = "MEL-ARG003"
            )
        )

        // Act
        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidstakersSkjema.id!!)

        // Assert
        result shouldBe UtsendtArbeidstakerM2MSkjemaData(
            arbeidstakersDel = arbeidstakersData,
            arbeidsgiversDel = null,
            referanseId = arbeidstakersInnsending.referanseId,
            journaposteringId = "Her skal det ligge en journapostering ID"
        )
    }

    @Test
    fun `hentUtsendtArbeidstakerSkjemaData returnerer kun arbeidsgiversDel når arbeidstakersDel ikke finnes`() {
        // Arrange - lagre kun arbeidsgivers skjema (ARBEIDSGIVER)
        val arbeidsgiversData = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()
        val arbeidsgiversSkjema = skjemaRepository.save(
            skjemaMedDefaultVerdier(
                fnr = korrektSyntetiskFnr,
                orgnr = korrektSyntetiskOrgnr,
                status = SkjemaStatus.SENDT,
                data = jsonMapper.valueToTree(arbeidsgiversData),
                metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                    representasjonstype = Representasjonstype.ARBEIDSGIVER
                )
            )
        )
        val arbeidsgiversInnsending = innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = arbeidsgiversSkjema,
                status = InnsendingStatus.FERDIG,
                referanseId = "MEL-ARG002"
            )
        )

        // Act
        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(arbeidsgiversSkjema.id!!)

        // Assert
        result shouldBe UtsendtArbeidstakerM2MSkjemaData(
            arbeidstakersDel = null,
            arbeidsgiversDel = arbeidsgiversData,
            referanseId = arbeidsgiversInnsending.referanseId,
            journaposteringId = "Her skal det ligge en journapostering ID"
        )
    }
}
