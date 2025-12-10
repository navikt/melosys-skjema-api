package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.controller.dto.translations.ArbeidsgiverensVirksomhetINorgeTranslation
import no.nav.melosys.skjema.controller.dto.translations.ArbeidssituasjonTranslation
import no.nav.melosys.skjema.controller.dto.translations.ArbeidsstedIUtlandetTranslation
import no.nav.melosys.skjema.controller.dto.translations.ArbeidstakerensLonnTranslation
import no.nav.melosys.skjema.controller.dto.translations.ErrorMessageTranslation
import no.nav.melosys.skjema.controller.dto.translations.ErrorMessageTranslations
import no.nav.melosys.skjema.controller.dto.translations.FellesTranslation
import no.nav.melosys.skjema.controller.dto.translations.OmBordPaFlyTranslation
import no.nav.melosys.skjema.controller.dto.translations.PaLandTranslation
import no.nav.melosys.skjema.controller.dto.translations.PaSkipTranslation
import no.nav.melosys.skjema.controller.dto.translations.PeriodeTranslation
import no.nav.melosys.skjema.controller.dto.translations.SkatteforholdOgInntektTranslation
import no.nav.melosys.skjema.controller.dto.translations.TilleggsopplysningerTranslation
import no.nav.melosys.skjema.controller.dto.translations.UtenlandsoppdragetArbeidstakerTranslation
import no.nav.melosys.skjema.controller.dto.translations.UtenlandsoppdragetTranslation
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/error-translation")
@Tag(name = "Error Translation", description = "Endepunkter for feilmeldinger og oversettelser")
class ErrorTranslationController() {

    @Unprotected
    @GetMapping
    @Operation(
        summary = "Hent feiloversettelser",
        description = "Returnerer alle feiloversettelser"
    )
    @ApiResponse(responseCode = "200", description = "Feiloversettelser hentet")
    fun getErrorTranslations(): ResponseEntity<ErrorMessageTranslations> {
        log.info { "Henter feiloversettelser" }
        return ResponseEntity.ok(TRANSLATIONS)
    }

    companion object {
        val TRANSLATIONS = ErrorMessageTranslations(
            no = ErrorMessageTranslation(
                arbeidsgiverensVirksomhetINorgeTranslation = ArbeidsgiverensVirksomhetINorgeTranslation(
                    offentligVirksomhetSkalIkkeOppgiBemanningsbyraa = "Offentlige virksomheter skal ikke oppgi om de er bemannings- eller vikarbyrå",
                    offentligVirksomhetSkalIkkeOppgiVanligDrift = "Offentlige virksomheter skal ikke oppgi vanlig drift",
                    maaOppgiOmBemanningsbyraa = "Du må oppgi om arbeidsgiver er bemannings- eller vikarbyrå",
                    maaOppgiOmVanligDrift = "Du må oppgi om arbeidsgiver opprettholder vanlig drift"
                ),
                arbeidssituasjonTranslation = ArbeidssituasjonTranslation(
                    maaOppgiAktivitetFoerUtsending = "Du må oppgi aktivitet i måneden før utsendingen",
                    maaOppgiMinstEnVirksomhet = "Du må oppgi minst én virksomhet"
                ),
                arbeidsstedIUtlandetTranslation = ArbeidsstedIUtlandetTranslation(
                    maaOppgiArbeidsstedPaLand = "Du må oppgi arbeidssted på land",
                    maaOppgiOffshoreArbeidssted = "Du må oppgi offshore arbeidssted",
                    maaOppgiArbeidsstedPaSkip = "Du må oppgi arbeidssted på skip",
                    maaOppgiArbeidsstedOmBordPaFly = "Du må oppgi arbeidssted om bord på fly"
                ),
                omBordPaFlyTranslation = OmBordPaFlyTranslation(
                    vanligHjemmebaseLandSkalIkkeOppgis = "Vanlig hjemmebase land skal ikke oppgis",
                    vanligHjemmebaseNavnSkalIkkeOppgis = "Vanlig hjemmebase navn skal ikke oppgis",
                    maaOppgiVanligHjemmebaseLand = "Du må oppgi vanlig hjemmebase land",
                    maaOppgiVanligHjemmebaseNavn = "Du må oppgi vanlig hjemmebase navn"
                ),
                paLandTranslation = PaLandTranslation(
                    maaOppgiFastArbeidssted = "Du må oppgi fast arbeidssted",
                    beskrivelseVekslendeSkalIkkeOppgis = "Beskrivelse av vekslende arbeidssted skal ikke oppgis",
                    fastArbeidsstedSkalIkkeOppgis = "Fast arbeidssted skal ikke oppgis",
                    maaOppgiBeskrivelseVekslende = "Du må oppgi beskrivelse av vekslende arbeidssted"
                ),
                paSkipTranslation = PaSkipTranslation(
                    duMaOppgiFlaggland = "Du må oppgi flaggland for skip i internasjonalt farvann",
                    territorialfarvannLandSkalIkkeOppgis = "Territorialfarvann land skal ikke oppgis for internasjonalt farvann",
                    duMaOppgiTerritorialfarvannLand = "Du må oppgi territorialfarvann land",
                    flagglandSkalIkkeOppgis = "Flaggland skal ikke oppgis for territorialfarvann"
                ),
                arbeidstakerensLonnTranslation = ArbeidstakerensLonnTranslation(
                    virksomheterSkalIkkeOppgis = "Virksomheter som utbetaler lønn skal ikke oppgis når arbeidsgiver betaler alt",
                    maaOppgiVirksomheter = "Du må oppgi virksomheter som utbetaler lønn og naturalytelser"
                ),
                periodeTranslation = PeriodeTranslation(
                    fraDatoMaaVaereFoerTilDato = "Fra-dato må være før eller lik til-dato"
                ),
                skatteforholdOgInntektTranslation = SkatteforholdOgInntektTranslation(
                    maaOppgiLandSomUtbetalerPengestotte = "Du må oppgi land som utbetaler pengestøtte",
                    maaOppgiBelopPengestotte = "Du må oppgi beløp for pengestøtte fra andre land",
                    maaOppgiBeskrivelsePengestotte = "Du må oppgi beskrivelse av pengestøtte fra andre land"
                ),
                tilleggsopplysningerTranslation = TilleggsopplysningerTranslation(
                    maaOppgiTilleggsopplysninger = "Du må oppgi tilleggsopplysninger",
                    tilleggsopplysningerSkalIkkeOppgis = "Tilleggsopplysninger skal ikke oppgis"
                ),
                utenlandsoppdragetTranslation = UtenlandsoppdragetTranslation(
                    duMaOppgiBegrunnelse = "Du må oppgi begrunnelse for utenlandsoppholdet",
                    duMaOppgiOmArbeidstakerVilJobbeEtterOppdraget = "Du må oppgi om arbeidstaker vil jobbe for virksomhet i Norge etter oppdraget",
                    duMaOppgiBeskrivelseAvAnsettelsesforhold = "Du må oppgi beskrivelse av ansettelsesforholdet",
                    duMaOppgiForrigeArbeidstakerUtsendelsePeriode = "Du må oppgi forrige arbeidstakers utsendelseperiode"
                ),
                utenlandsoppdragetArbeidstakerTranslation = UtenlandsoppdragetArbeidstakerTranslation(
                    duMaOppgiUtsendelsesland = "Du må oppgi utsendelsesland"
                ),
                fellesTranslation = FellesTranslation(
                    organisasjonsnummerHarUgyldigFormat = "Organisasjonsnummer har ugyldig format",
                    organisasjonsnummerFinnesIkke = "Organisasjonsnummer finnes ikke i Enhetsregisteret",
                    ugyldigFodselsellerDNummer = "Ugyldig fødsels- eller D-nummer"
                )
            ),
            en = ErrorMessageTranslation(
                arbeidsgiverensVirksomhetINorgeTranslation = ArbeidsgiverensVirksomhetINorgeTranslation(
                    offentligVirksomhetSkalIkkeOppgiBemanningsbyraa = "Public organizations should not specify whether they are staffing or temporary employment agencies",
                    offentligVirksomhetSkalIkkeOppgiVanligDrift = "Public organizations should not specify normal operations",
                    maaOppgiOmBemanningsbyraa = "You must specify whether the employer is a staffing or temporary employment agency",
                    maaOppgiOmVanligDrift = "You must specify whether the employer maintains normal operations"
                ),
                arbeidssituasjonTranslation = ArbeidssituasjonTranslation(
                    maaOppgiAktivitetFoerUtsending = "You must specify activity in the month before posting",
                    maaOppgiMinstEnVirksomhet = "You must specify at least one company"
                ),
                arbeidsstedIUtlandetTranslation = ArbeidsstedIUtlandetTranslation(
                    maaOppgiArbeidsstedPaLand = "You must specify workplace on land",
                    maaOppgiOffshoreArbeidssted = "You must specify offshore workplace",
                    maaOppgiArbeidsstedPaSkip = "You must specify workplace on ship",
                    maaOppgiArbeidsstedOmBordPaFly = "You must specify workplace on board aircraft"
                ),
                omBordPaFlyTranslation = OmBordPaFlyTranslation(
                    vanligHjemmebaseLandSkalIkkeOppgis = "Regular home base country should not be specified",
                    vanligHjemmebaseNavnSkalIkkeOppgis = "Regular home base name should not be specified",
                    maaOppgiVanligHjemmebaseLand = "You must specify regular home base country",
                    maaOppgiVanligHjemmebaseNavn = "You must specify regular home base name"
                ),
                paLandTranslation = PaLandTranslation(
                    maaOppgiFastArbeidssted = "You must specify fixed workplace",
                    beskrivelseVekslendeSkalIkkeOppgis = "Description of alternating workplace should not be specified",
                    fastArbeidsstedSkalIkkeOppgis = "Fixed workplace should not be specified",
                    maaOppgiBeskrivelseVekslende = "You must specify description of alternating workplace"
                ),
                paSkipTranslation = PaSkipTranslation(
                    duMaOppgiFlaggland = "You must specify flag country for ship in international waters",
                    territorialfarvannLandSkalIkkeOppgis = "Territorial waters country should not be specified for international waters",
                    duMaOppgiTerritorialfarvannLand = "You must specify territorial waters country",
                    flagglandSkalIkkeOppgis = "Flag country should not be specified for territorial waters"
                ),
                arbeidstakerensLonnTranslation = ArbeidstakerensLonnTranslation(
                    virksomheterSkalIkkeOppgis = "Companies that pay salary should not be specified when employer pays all",
                    maaOppgiVirksomheter = "You must specify companies that pay salary and benefits in kind"
                ),
                periodeTranslation = PeriodeTranslation(
                    fraDatoMaaVaereFoerTilDato = "From date must be before or equal to to date"
                ),
                skatteforholdOgInntektTranslation = SkatteforholdOgInntektTranslation(
                    maaOppgiLandSomUtbetalerPengestotte = "You must specify country that pays financial support",
                    maaOppgiBelopPengestotte = "You must specify amount for financial support from other countries",
                    maaOppgiBeskrivelsePengestotte = "You must specify description of financial support from other countries"
                ),
                tilleggsopplysningerTranslation = TilleggsopplysningerTranslation(
                    maaOppgiTilleggsopplysninger = "You must provide additional information",
                    tilleggsopplysningerSkalIkkeOppgis = "Additional information should not be provided"
                ),
                utenlandsoppdragetTranslation = UtenlandsoppdragetTranslation(
                    duMaOppgiBegrunnelse = "You must provide justification for the stay abroad",
                    duMaOppgiOmArbeidstakerVilJobbeEtterOppdraget = "You must specify whether employee will work for company in Norway after assignment",
                    duMaOppgiBeskrivelseAvAnsettelsesforhold = "You must provide description of employment relationship",
                    duMaOppgiForrigeArbeidstakerUtsendelsePeriode = "You must specify previous employee's posting period"
                ),
                utenlandsoppdragetArbeidstakerTranslation = UtenlandsoppdragetArbeidstakerTranslation(
                    duMaOppgiUtsendelsesland = "You must specify posting country"
                ),
                fellesTranslation = FellesTranslation(
                    organisasjonsnummerHarUgyldigFormat = "Organization number has invalid format",
                    organisasjonsnummerFinnesIkke = "Organization number does not exist in the Business Register",
                    ugyldigFodselsellerDNummer = "Invalid birth or D-number"
                )
            )
        )
    }
}

