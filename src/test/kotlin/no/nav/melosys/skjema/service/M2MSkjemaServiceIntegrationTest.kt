package no.nav.melosys.skjema.service

import io.kotest.matchers.shouldBe
import java.time.LocalDate
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import no.nav.melosys.skjema.types.Representasjonstype
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.json.JsonMapper

data class HentUtsendtArbeidstakerSkjemaDataTestScenario(
    val beskrivelse: String,
    val hovedSkjema: Skjema,
    val innsending: Innsending,
    val andreEksisterendeSkjemaer: List<Skjema>,
    val forventetResultat: UtsendtArbeidstakerM2MSkjemaData
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class M2MSkjemaServiceIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var m2mSkjemaService: M2MSkjemaService

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    private val jsonMapper = JsonMapper.builder().build()

    private val overlappendePeriode = PeriodeDto(
        fraDato = LocalDate.of(2024, 1, 1),
        tilDato = LocalDate.of(2024, 12, 31)
    )

    private val ikkeOverlappendePeriode = PeriodeDto(
        fraDato = LocalDate.of(2025, 1, 1),
        tilDato = LocalDate.of(2025, 12, 31)
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

    private val arbeidsgiversDataMedIkkeOverlappendePeriode = arbeidsgiversSkjemaDataDtoMedDefaultVerdier().copy(
        utenlandsoppdraget = utenlandsoppdragetDtoMedDefaultVerdier().copy(
            arbeidstakerUtsendelsePeriode = ikkeOverlappendePeriode
        )
    )

    private fun arbeidstakersSkjemaMedOverlappendePeriode() = skjemaMedDefaultVerdier(
        fnr = korrektSyntetiskFnr,
        orgnr = korrektSyntetiskOrgnr,
        status = SkjemaStatus.SENDT,
        data = jsonMapper.valueToTree(arbeidstakersDataMedOverlappendePeriode),
        metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.DEG_SELV
        )
    )

    private fun arbeidsgiversSkjemaMedOverlappendePeriode() = skjemaMedDefaultVerdier(
        fnr = korrektSyntetiskFnr,
        orgnr = korrektSyntetiskOrgnr,
        status = SkjemaStatus.SENDT,
        data = jsonMapper.valueToTree(arbeidsgiversDataMedOverlappendePeriode),
        metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )
    )

    private fun arbeidsgiversSkjemaMedIkkeOverlappendePeriode() = skjemaMedDefaultVerdier(
        fnr = korrektSyntetiskFnr,
        orgnr = korrektSyntetiskOrgnr,
        status = SkjemaStatus.SENDT,
        data = jsonMapper.valueToTree(arbeidsgiversDataMedIkkeOverlappendePeriode),
        metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
            representasjonstype = Representasjonstype.ARBEIDSGIVER
        )
    )

    fun testScenarioer(): List<HentUtsendtArbeidstakerSkjemaDataTestScenario> {
        val scenario1HovedSkjema = arbeidstakersSkjemaMedOverlappendePeriode()
        val scenario2HovedSkjema = arbeidstakersSkjemaMedOverlappendePeriode()
        val scenario3HovedSkjema = arbeidstakersSkjemaMedOverlappendePeriode()
        val scenario4HovedSkjema = arbeidsgiversSkjemaMedOverlappendePeriode()

        return listOf(
            HentUtsendtArbeidstakerSkjemaDataTestScenario(
                beskrivelse = "returnerer arbeidstakersDel og arbeidsgiversDel når begge finnes med overlappende perioder",
                hovedSkjema = scenario1HovedSkjema,
                innsending = innsendingMedDefaultVerdier(
                    skjema = scenario1HovedSkjema,
                    status = InnsendingStatus.FERDIG,
                    referanseId = "MEL-TEST01"
                ),
                andreEksisterendeSkjemaer = listOf(arbeidsgiversSkjemaMedOverlappendePeriode()),
                forventetResultat = UtsendtArbeidstakerM2MSkjemaData(
                    arbeidstakersDel = arbeidstakersDataMedOverlappendePeriode,
                    arbeidsgiversDel = arbeidsgiversDataMedOverlappendePeriode,
                    referanseId = "MEL-TEST01"
                )
            ),
            HentUtsendtArbeidstakerSkjemaDataTestScenario(
                beskrivelse = "returnerer kun arbeidstakersDel når ingen arbeidsgiversDel finnes",
                hovedSkjema = scenario2HovedSkjema,
                innsending = innsendingMedDefaultVerdier(
                    skjema = scenario2HovedSkjema,
                    status = InnsendingStatus.FERDIG,
                    referanseId = "MEL-TEST02"
                ),
                andreEksisterendeSkjemaer = emptyList(),
                forventetResultat = UtsendtArbeidstakerM2MSkjemaData(
                    arbeidstakersDel = arbeidstakersDataMedOverlappendePeriode,
                    arbeidsgiversDel = null,
                    referanseId = "MEL-TEST02"
                )
            ),
            HentUtsendtArbeidstakerSkjemaDataTestScenario(
                beskrivelse = "returnerer kun arbeidstakersDel når arbeidsgiversDel har ikke-overlappende periode",
                hovedSkjema = scenario3HovedSkjema,
                innsending = innsendingMedDefaultVerdier(
                    skjema = scenario3HovedSkjema,
                    status = InnsendingStatus.FERDIG,
                    referanseId = "MEL-TEST03"
                ),
                andreEksisterendeSkjemaer = listOf(arbeidsgiversSkjemaMedIkkeOverlappendePeriode()),
                forventetResultat = UtsendtArbeidstakerM2MSkjemaData(
                    arbeidstakersDel = arbeidstakersDataMedOverlappendePeriode,
                    arbeidsgiversDel = null,
                    referanseId = "MEL-TEST03"
                )
            ),
            HentUtsendtArbeidstakerSkjemaDataTestScenario(
                beskrivelse = "returnerer kun arbeidsgiversDel når arbeidstakersDel ikke finnes",
                hovedSkjema = scenario4HovedSkjema,
                innsending = innsendingMedDefaultVerdier(
                    skjema = scenario4HovedSkjema,
                    status = InnsendingStatus.FERDIG,
                    referanseId = "MEL-TEST04"
                ),
                andreEksisterendeSkjemaer = emptyList(),
                forventetResultat = UtsendtArbeidstakerM2MSkjemaData(
                    arbeidstakersDel = null,
                    arbeidsgiversDel = arbeidsgiversDataMedOverlappendePeriode,
                    referanseId = "MEL-TEST04"
                )
            )
        )
    }

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testScenarioer")
    fun hentUtsendtArbeidstakerSkjemaData(scenario: HentUtsendtArbeidstakerSkjemaDataTestScenario) {
        // Arrange
        val lagretHovedSkjema = skjemaRepository.save(scenario.hovedSkjema)
        innsendingRepository.save(
            innsendingMedDefaultVerdier(
                skjema = lagretHovedSkjema,
                status = scenario.innsending.status,
                referanseId = scenario.innsending.referanseId
            )
        )
        scenario.andreEksisterendeSkjemaer.forEach { skjemaRepository.save(it) }

        // Act
        val result = m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(lagretHovedSkjema.id!!)

        // Assert
        result shouldBe scenario.forventetResultat
    }
}