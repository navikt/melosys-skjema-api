package no.nav.melosys.skjema.validators.utenlandsoppdraget

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import jakarta.validation.Valid
import java.util.stream.Stream
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.felles.LandKode
import no.nav.melosys.skjema.dto.felles.PeriodeDto
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import no.nav.melosys.skjema.validators.felles.PeriodeValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtenlandsoppdragetValidatorTest {

    private val validator = UtenlandsoppdragetValidator()

    private val utenlandsoppdragetDtoMedGyldigeVerdier = utenlandsoppdragetDtoMedDefaultVerdier().copy(
        utsendelseLand = LandKode.SE,
        arbeidstakerUtsendelsePeriode = PeriodeDto(
            fraDato = java.time.LocalDate.of(2024, 1, 1),
            tilDato = java.time.LocalDate.of(2024, 12, 31)
        ),
        arbeidsgiverHarOppdragILandet = true,
        arbeidstakerBleAnsattForUtenlandsoppdraget = false,
        arbeidstakerForblirAnsattIHelePerioden = true,
        arbeidstakerErstatterAnnenPerson = false,
        arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = null,
        utenlandsoppholdetsBegrunnelse = null,
        ansettelsesforholdBeskrivelse = null,
        forrigeArbeidstakerUtsendelsePeriode = null
    )

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: UtenlandsoppdragetDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: UtenlandsoppdragetDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Default verdier - alle felt gyldig
        utenlandsoppdragetDtoMedGyldigeVerdier,
        // Arbeidsgiver har ikke oppdrag - begrunnelse er satt
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidsgiverHarOppdragILandet = false,
            utenlandsoppholdetsBegrunnelse = "Annen begrunnelse"
        ),
        // Arbeidsgiver har oppdrag - begrunnelse kan være null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidsgiverHarOppdragILandet = true,
            utenlandsoppholdetsBegrunnelse = null
        ),
        // Arbeidstaker ble ansatt for utenlandsoppdraget - må ha verdi for om de skal jobbe i Norge etter
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerBleAnsattForUtenlandsoppdraget = true,
            arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = true
        ),
        // Arbeidstaker ble ansatt for utenlandsoppdraget - false er også gyldig
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerBleAnsattForUtenlandsoppdraget = true,
            arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = false
        ),
        // Arbeidstaker ble ikke ansatt for utenlandsoppdraget - feltet kan være null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerBleAnsattForUtenlandsoppdraget = false,
            arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = null
        ),
        // Arbeidstaker forblir ikke ansatt - beskrivelse er satt
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerForblirAnsattIHelePerioden = false,
            ansettelsesforholdBeskrivelse = "Beskrivelse av ansettelsesforhold"
        ),
        // Arbeidstaker forblir ansatt - beskrivelse kan være null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerForblirAnsattIHelePerioden = true,
            ansettelsesforholdBeskrivelse = null
        ),
        // Arbeidstaker erstatter annen person - begge datoer er satt og gyldige
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerErstatterAnnenPerson = true,
            forrigeArbeidstakerUtsendelsePeriode = PeriodeDto(
                fraDato = java.time.LocalDate.of(2023, 1, 1),
                tilDato = java.time.LocalDate.of(2023, 12, 31)
            )
        ),
        // Arbeidstaker erstatter ikke annen person - datoer kan være null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerErstatterAnnenPerson = false,
            forrigeArbeidstakerUtsendelsePeriode = null
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf<UtenlandsoppdragetDto>(
        // Arbeidsgiver har ikke oppdrag, men begrunnelse er null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidsgiverHarOppdragILandet = false,
            utenlandsoppholdetsBegrunnelse = null
        ),
        // Arbeidsgiver har ikke oppdrag, men begrunnelse er tom
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidsgiverHarOppdragILandet = false,
            utenlandsoppholdetsBegrunnelse = ""
        ),
        // Arbeidsgiver har ikke oppdrag, men begrunnelse er blank
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidsgiverHarOppdragILandet = false,
            utenlandsoppholdetsBegrunnelse = "   "
        ),
        // Arbeidstaker ble ansatt for utenlandsoppdraget, men felt om Norge er null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerBleAnsattForUtenlandsoppdraget = true,
            arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = null
        ),
        // Arbeidstaker forblir ikke ansatt, men beskrivelse er null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerForblirAnsattIHelePerioden = false,
            ansettelsesforholdBeskrivelse = null
        ),
        // Arbeidstaker forblir ikke ansatt, men beskrivelse er tom
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerForblirAnsattIHelePerioden = false,
            ansettelsesforholdBeskrivelse = ""
        ),
        // Arbeidstaker forblir ikke ansatt, men beskrivelse er blank
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerForblirAnsattIHelePerioden = false,
            ansettelsesforholdBeskrivelse = "   "
        ),
        // Arbeidstaker erstatter annen person, men periode er null
        utenlandsoppdragetDtoMedGyldigeVerdier.copy(
            arbeidstakerErstatterAnnenPerson = true,
            forrigeArbeidstakerUtsendelsePeriode = null
        )
    ).map { Arguments.of(it) }.stream()
}
