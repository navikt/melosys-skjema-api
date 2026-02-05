package no.nav.melosys.skjema.service

import io.kotest.matchers.collections.shouldBeEmpty
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
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.json.JsonMapper

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
            data = jsonMapper.valueToTree(arbeidsgiversDataMedOverlappendePeriode),
            metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
                representasjonstype = Representasjonstype.ARBEIDSGIVER,
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            )
        ))

        // Lagre arbeidstaker-del med kobling til arbeidsgiver
        val arbeidstakersSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = jsonMapper.valueToTree(arbeidstakersDataMedOverlappendePeriode),
            metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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
        result.arbeidstakersDeler shouldHaveSize 1
        result.arbeidsgiversDeler shouldHaveSize 1
        result.arbeidstakersDeler[0].skjemaId shouldBe arbeidstakersSkjema.id
        result.arbeidstakersDeler[0].utenlandsoppdraget shouldBe arbeidstakersDataMedOverlappendePeriode.utenlandsoppdraget
        result.arbeidsgiversDeler[0].skjemaId shouldBe arbeidsgiversSkjema.id
        result.arbeidsgiversDeler[0].utenlandsoppdraget shouldBe arbeidsgiversDataMedOverlappendePeriode.utenlandsoppdraget
    }

    @Test
    fun `returnerer kun arbeidstakersDel når ingen kobling finnes`() {
        val arbeidstakersSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = jsonMapper.valueToTree(arbeidstakersDataMedOverlappendePeriode),
            metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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
        result.arbeidstakersDeler shouldHaveSize 1
        result.arbeidsgiversDeler.shouldBeEmpty()
        result.arbeidstakersDeler[0].skjemaId shouldBe arbeidstakersSkjema.id
    }

    @Test
    fun `returnerer kun arbeidsgiversDel når det er hovedskjema uten kobling`() {
        val arbeidsgiversSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = jsonMapper.valueToTree(arbeidsgiversDataMedOverlappendePeriode),
            metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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
        result.arbeidstakersDeler.shouldBeEmpty()
        result.arbeidsgiversDeler shouldHaveSize 1
        result.arbeidsgiversDeler[0].skjemaId shouldBe arbeidsgiversSkjema.id
    }

    @Test
    fun `inkluderer erstatterSkjemaId i respons når skjema erstatter et annet`() {
        val gammelSkjemaId = java.util.UUID.randomUUID()

        val arbeidstakersSkjema = skjemaRepository.save(skjemaMedDefaultVerdier(
            fnr = korrektSyntetiskFnr,
            orgnr = korrektSyntetiskOrgnr,
            status = SkjemaStatus.SENDT,
            data = jsonMapper.valueToTree(arbeidstakersDataMedOverlappendePeriode),
            metadata = utsendtArbeidstakerMetadataJsonNodeMedDefaultVerdier(
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

        result.arbeidstakersDeler shouldHaveSize 1
        result.arbeidstakersDeler[0].erstatterSkjemaId shouldBe gammelSkjemaId
    }
}
