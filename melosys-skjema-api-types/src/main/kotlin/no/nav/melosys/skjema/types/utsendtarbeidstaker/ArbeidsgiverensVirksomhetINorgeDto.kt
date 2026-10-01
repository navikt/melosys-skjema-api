package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiverensVirksomhetINorgeDto(
    @Deprecated("Brukes kun til visning av historiske V1 innsendinger, kan fjernes når visning av disse ikke er nødvendig lenger")
    val erArbeidsgiverenOffentligVirksomhet: Boolean? = null,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean? = null,
    val opprettholderArbeidsgiverenVanligDrift: Boolean? = null,
    /** Opplysninger om foretakets samlede virksomhet. Kreves bare ved færre enn 20 ansatte eller bemanningsbyrå. */
    val antallAdministrativtAnsatte: Int? = null,
    val antallUtsendteArbeidstakere: Int? = null,
    /** Prosent, 0–100 */
    val andelAnsatteRekruttertINorge: Int? = null,
    /** Prosent, 0–100 */
    val andelOmsetningINorge: Int? = null,
    /** Prosent, 0–100 */
    val andelOppdragUtfortINorge: Int? = null,
    /** Prosent, 0–100 */
    val andelOppdragskontrakterInngattINorge: Int? = null
)