package no.nav.melosys.skjema.service.pdf

import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.OffshoreDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.OmBordPaFlyDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaLandDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaSkipDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.skjemadefinisjon.SeksjonDefinisjonDto
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto

/**
 * Rendrer seksjoner til HTML med typede DTO-er.
 * Ingen Map-konvertering eller reflection - direkte tilgang til felter.
 */
class SeksjonRenderer(
    private val feltRenderer: FeltRenderer
) {

    // ==================== ARBEIDSTAKER ====================

    fun byggArbeidstakerSeksjoner(
        data: UtsendtArbeidstakerArbeidstakersSkjemaDataDto,
        definisjon: SkjemaDefinisjonDto
    ): String {
        val builder = StringBuilder()

        data.utenlandsoppdraget?.let { dto ->
            definisjon.seksjoner["utenlandsoppdragetArbeidstaker"]?.let { seksjon ->
                builder.append(byggUtenlandsoppdragetArbeidstaker(dto, seksjon))
            }
        }

        data.arbeidssituasjon?.let { dto ->
            definisjon.seksjoner["arbeidssituasjon"]?.let { seksjon ->
                builder.append(byggArbeidssituasjon(dto, seksjon))
            }
        }

        data.skatteforholdOgInntekt?.let { dto ->
            definisjon.seksjoner["skatteforholdOgInntekt"]?.let { seksjon ->
                builder.append(byggSkatteforholdOgInntekt(dto, seksjon))
            }
        }

        data.familiemedlemmer?.let { dto ->
            definisjon.seksjoner["familiemedlemmer"]?.let { seksjon ->
                builder.append(byggFamiliemedlemmer(dto, seksjon))
            }
        }

        data.tilleggsopplysninger?.let { dto ->
            definisjon.seksjoner["tilleggsopplysningerArbeidstaker"]?.let { seksjon ->
                builder.append(byggTilleggsopplysninger(dto, seksjon))
            }
        }

        return builder.toString()
    }

    private fun byggUtenlandsoppdragetArbeidstaker(
        data: UtenlandsoppdragetArbeidstakersDelDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("utsendelsesLand", data.utsendelsesLand)
            felt("utsendelsePeriode", data.utsendelsePeriode)
        }
    }

    private fun byggArbeidssituasjon(
        data: ArbeidssituasjonDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt(
                "harVaertEllerSkalVaereILonnetArbeidFoerUtsending",
                data.harVaertEllerSkalVaereILonnetArbeidFoerUtsending
            )
            felt("aktivitetIMaanedenFoerUtsendingen", data.aktivitetIMaanedenFoerUtsendingen)
            felt("skalJobbeForFlereVirksomheter", data.skalJobbeForFlereVirksomheter)
            felt(
                "virksomheterArbeidstakerJobberForIutsendelsesPeriode",
                data.virksomheterArbeidstakerJobberForIutsendelsesPeriode
            )
        }
    }

    private fun byggSkatteforholdOgInntekt(
        data: SkatteforholdOgInntektDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("erSkattepliktigTilNorgeIHeleutsendingsperioden", data.erSkattepliktigTilNorgeIHeleutsendingsperioden)
            felt("mottarPengestotteFraAnnetEosLandEllerSveits", data.mottarPengestotteFraAnnetEosLandEllerSveits)
            felt("landSomUtbetalerPengestotte", data.landSomUtbetalerPengestotte)
            felt("pengestotteSomMottasFraAndreLandBelop", data.pengestotteSomMottasFraAndreLandBelop)
            felt("pengestotteSomMottasFraAndreLandBeskrivelse", data.pengestotteSomMottasFraAndreLandBeskrivelse)
        }
    }

    private fun byggFamiliemedlemmer(
        data: FamiliemedlemmerDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("skalHaMedFamiliemedlemmer", data.skalHaMedFamiliemedlemmer)
            felt("familiemedlemmer", data.familiemedlemmer)
        }
    }

    private fun byggTilleggsopplysninger(
        data: TilleggsopplysningerDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("harFlereOpplysningerTilSoknaden", data.harFlereOpplysningerTilSoknaden)
            felt("tilleggsopplysningerTilSoknad", data.tilleggsopplysningerTilSoknad)
        }
    }

    // ==================== ARBEIDSGIVER ====================

    fun byggArbeidsgiverSeksjoner(
        data: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto,
        definisjon: SkjemaDefinisjonDto
    ): String {
        val builder = StringBuilder()

        data.arbeidsgiverensVirksomhetINorge?.let { dto ->
            definisjon.seksjoner["arbeidsgiverensVirksomhetINorge"]?.let { seksjon ->
                builder.append(byggArbeidsgiverensVirksomhetINorge(dto, seksjon))
            }
        }

        data.utenlandsoppdraget?.let { dto ->
            definisjon.seksjoner["utenlandsoppdragetArbeidsgiver"]?.let { seksjon ->
                builder.append(byggUtenlandsoppdragetArbeidsgiver(dto, seksjon))
            }
        }

        data.arbeidstakerensLonn?.let { dto ->
            definisjon.seksjoner["arbeidstakerensLonn"]?.let { seksjon ->
                builder.append(byggArbeidstakerensLonn(dto, seksjon))
            }
        }

        data.tilleggsopplysninger?.let { dto ->
            definisjon.seksjoner["tilleggsopplysningerArbeidsgiver"]?.let { seksjon ->
                builder.append(byggTilleggsopplysninger(dto, seksjon))
            }
        }

        data.arbeidsstedIUtlandet?.let { dto ->
            builder.append(byggArbeidsstedIUtlandet(dto, definisjon))
        }

        return builder.toString()
    }

    private fun byggArbeidsgiverensVirksomhetINorge(
        data: ArbeidsgiverensVirksomhetINorgeDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("erArbeidsgiverenOffentligVirksomhet", data.erArbeidsgiverenOffentligVirksomhet)
            felt("erArbeidsgiverenBemanningsEllerVikarbyraa", data.erArbeidsgiverenBemanningsEllerVikarbyraa)
            felt("opprettholderArbeidsgiverenVanligDrift", data.opprettholderArbeidsgiverenVanligDrift)
        }
    }

    private fun byggUtenlandsoppdragetArbeidsgiver(
        data: UtenlandsoppdragetDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("utsendelseLand", data.utsendelseLand)
            felt("arbeidstakerUtsendelsePeriode", data.arbeidstakerUtsendelsePeriode)
            felt("arbeidsgiverHarOppdragILandet", data.arbeidsgiverHarOppdragILandet)
            felt("arbeidstakerBleAnsattForUtenlandsoppdraget", data.arbeidstakerBleAnsattForUtenlandsoppdraget)
            felt("arbeidstakerForblirAnsattIHelePerioden", data.arbeidstakerForblirAnsattIHelePerioden)
            felt("arbeidstakerErstatterAnnenPerson", data.arbeidstakerErstatterAnnenPerson)
            felt(
                "arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget",
                data.arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget
            )
            felt("utenlandsoppholdetsBegrunnelse", data.utenlandsoppholdetsBegrunnelse)
            felt("ansettelsesforholdBeskrivelse", data.ansettelsesforholdBeskrivelse)
            felt("forrigeArbeidstakerUtsendelsePeriode", data.forrigeArbeidstakerUtsendelsePeriode)
        }
    }

    private fun byggArbeidstakerensLonn(
        data: ArbeidstakerensLonnDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt(
                "arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden",
                data.arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden
            )
            felt("virksomheterSomUtbetalerLonnOgNaturalytelser", data.virksomheterSomUtbetalerLonnOgNaturalytelser)
        }
    }

    private fun byggArbeidsstedIUtlandet(
        data: ArbeidsstedIUtlandetDto,
        definisjon: SkjemaDefinisjonDto
    ): String {
        val builder = StringBuilder()

        // Arbeidssted type
        definisjon.seksjoner["arbeidsstedIUtlandet"]?.let { seksjon ->
            builder.append(byggSeksjon(seksjon) {
                feltDirekte("Type arbeidssted", arbeidsstedTypeLabel(data.arbeidsstedType))
            })
        }

        // Subtype-spesifikke seksjoner
        when (data.arbeidsstedType) {
            ArbeidsstedType.PA_LAND -> data.paLand?.let { dto ->
                definisjon.seksjoner["arbeidsstedPaLand"]?.let { seksjon ->
                    builder.append(byggArbeidsstedPaLand(dto, seksjon))
                }
            }

            ArbeidsstedType.OFFSHORE -> data.offshore?.let { dto ->
                definisjon.seksjoner["arbeidsstedOffshore"]?.let { seksjon ->
                    builder.append(byggArbeidsstedOffshore(dto, seksjon))
                }
            }

            ArbeidsstedType.PA_SKIP -> data.paSkip?.let { dto ->
                definisjon.seksjoner["arbeidsstedPaSkip"]?.let { seksjon ->
                    builder.append(byggArbeidsstedPaSkip(dto, seksjon))
                }
            }

            ArbeidsstedType.OM_BORD_PA_FLY -> data.omBordPaFly?.let { dto ->
                definisjon.seksjoner["arbeidsstedOmBordPaFly"]?.let { seksjon ->
                    builder.append(byggArbeidsstedOmBordPaFly(dto, seksjon))
                }
            }
        }

        return builder.toString()
    }

    private fun byggArbeidsstedPaLand(
        data: PaLandDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("navnPaVirksomhet", data.navnPaVirksomhet)
            felt("fastEllerVekslendeArbeidssted", data.fastEllerVekslendeArbeidssted)
            // Adressefelter fra fastArbeidssted
            data.fastArbeidssted?.let { adresse ->
                felt("vegadresse", adresse.vegadresse)
                felt("nummer", adresse.nummer)
                felt("postkode", adresse.postkode)
                felt("bySted", adresse.bySted)
            }
            felt("beskrivelseVekslende", data.beskrivelseVekslende)
            felt("erHjemmekontor", data.erHjemmekontor)
        }
    }

    private fun byggArbeidsstedOffshore(
        data: OffshoreDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("navnPaVirksomhet", data.navnPaVirksomhet)
            felt("navnPaInnretning", data.navnPaInnretning)
            felt("typeInnretning", data.typeInnretning)
            felt("sokkelLand", data.sokkelLand)
        }
    }

    private fun byggArbeidsstedPaSkip(
        data: PaSkipDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("navnPaVirksomhet", data.navnPaVirksomhet)
            felt("navnPaSkip", data.navnPaSkip)
            felt("yrketTilArbeidstaker", data.yrketTilArbeidstaker)
            felt("seilerI", data.seilerI)
            felt("flaggland", data.flaggland)
            felt("territorialfarvannLand", data.territorialfarvannLand)
        }
    }

    private fun byggArbeidsstedOmBordPaFly(
        data: OmBordPaFlyDto,
        seksjon: SeksjonDefinisjonDto
    ): String {
        return byggSeksjon(seksjon) {
            felt("navnPaVirksomhet", data.navnPaVirksomhet)
            felt("hjemmebaseLand", data.hjemmebaseLand)
            felt("hjemmebaseNavn", data.hjemmebaseNavn)
            felt("erVanligHjemmebase", data.erVanligHjemmebase)
            felt("vanligHjemmebaseLand", data.vanligHjemmebaseLand)
            felt("vanligHjemmebaseNavn", data.vanligHjemmebaseNavn)
        }
    }

    private fun arbeidsstedTypeLabel(type: ArbeidsstedType): String = when (type) {
        ArbeidsstedType.PA_LAND -> "På land"
        ArbeidsstedType.OFFSHORE -> "Offshore"
        ArbeidsstedType.PA_SKIP -> "På skip"
        ArbeidsstedType.OM_BORD_PA_FLY -> "Om bord på fly"
    }

    // ==================== HELPERS ====================

    private fun byggSeksjon(
        seksjon: SeksjonDefinisjonDto,
        block: SeksjonBuilder.() -> Unit
    ): String {
        val builder = SeksjonBuilder(seksjon, feltRenderer)
        builder.block()
        return builder.build()
    }

    /**
     * Builder for å bygge en seksjon med felter.
     */
    inner class SeksjonBuilder(
        private val seksjon: SeksjonDefinisjonDto,
        private val feltRenderer: FeltRenderer
    ) {
        private val html = StringBuilder()

        fun felt(feltNavn: String, verdi: Any?) {
            if (verdi == null) return

            seksjon.felter[feltNavn]?.let { feltDef ->
                val feltHtml = feltRenderer.render(feltDef, verdi)
                if (feltHtml.isNotBlank()) {
                    html.append(feltHtml)
                }
            }
        }

        fun feltDirekte(label: String, verdi: String) {
            html.append(feltRenderer.renderEnkeltFelt(label, verdi))
        }

        fun build(): String {
            return """
                <div class="form-summary">
                    <div class="form-summary-header">
                        <h3 class="form-summary-heading">${escapeHtml(seksjon.tittel)}</h3>
                    </div>
                    <div class="form-summary-answers">$html</div>
                </div>
            """.trimIndent()
        }

        private fun escapeHtml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }
    }
}
