package no.nav.melosys.skjema.integrasjon.ereg.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

/**
 * Base sealed class for all organization types from EREG.
 * Uses Jackson annotations for polymorphic deserialization based on the "type" discriminator field.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = JuridiskEnhet::class, name = "JuridiskEnhet"),
    JsonSubTypes.Type(value = Virksomhet::class, name = "Virksomhet"),
    JsonSubTypes.Type(value = Organisasjonsledd::class, name = "Organisasjonsledd")
)
sealed class Organisasjon {
    abstract val organisasjonsnummer: String
    abstract val navn: Navn?
    abstract val type: String
}

/**
 * Juridisk enhet (legal entity) - e.g., a company
 */
data class JuridiskEnhet(
    override val organisasjonsnummer: String,
    override val navn: Navn? = null,
    override val type: String,
    val organisasjonDetaljer: OrganisasjonDetaljer? = null,
    val juridiskEnhetDetaljer: JuridiskEnhetDetaljer? = null
) : Organisasjon()

/**
 * Virksomhet (business unit) - operational unit under a legal entity
 */
data class Virksomhet(
    override val organisasjonsnummer: String,
    override val navn: Navn? = null,
    override val type: String,
    val organisasjonDetaljer: OrganisasjonDetaljer? = null,
    val virksomhetDetaljer: VirksomhetDetaljer? = null,
    val bestaarAvOrganisasjonsledd: List<BestaarAvOrganisasjonsledd>? = null,
    val inngaarIJuridiskEnheter: List<InngaarIJuridiskEnhet>? = null
) : Organisasjon()

/**
 * Organisasjonsledd (organizational unit) - sub-unit in an organization
 */
data class Organisasjonsledd(
    override val organisasjonsnummer: String,
    override val navn: Navn? = null,
    override val type: String,
    val organisasjonDetaljer: OrganisasjonDetaljer? = null,
    val organisasjonsleddDetaljer: OrganisasjonsleddDetaljer? = null,
    val driverVirksomheter: List<DriverVirksomhet>? = null,
    val inngaarIJuridiskEnheter: List<InngaarIJuridiskEnhet>? = null,
    val organisasjonsleddUnder: List<BestaarAvOrganisasjonsledd>? = null,
    val organisasjonsleddOver: List<BestaarAvOrganisasjonsledd>? = null
) : Organisasjon()

/**
 * Organization name information
 */
data class Navn(
    val sammensattnavn: String? = null,
    val navnelinje1: String? = null,
    val navnelinje2: String? = null,
    val navnelinje3: String? = null,
    val navnelinje4: String? = null,
    val navnelinje5: String? = null
)

/**
 * Common organization details
 */
data class OrganisasjonDetaljer(
    val registreringsdato: String? = null,
    val stiftelsesdato: LocalDate? = null,
    val opphoersdato: LocalDate? = null,
    val enhetstyper: List<Enhetstype>? = null,
    val navn: List<Navn>? = null,
    val naeringer: List<Naering>? = null,
    val forretningsadresser: List<Adresse>? = null,
    val postadresser: List<Adresse>? = null
)

/**
 * Details specific to juridisk enhet
 */
data class JuridiskEnhetDetaljer(
    val enhetstype: String? = null,
    val harAnsatte: Boolean? = null,
    val sektorkode: String? = null
)

/**
 * Details specific to virksomhet
 */
data class VirksomhetDetaljer(
    val enhetstype: String? = null,
    val ubemannetVirksomhet: Boolean? = null,
    val oppstartsdato: LocalDate? = null,
    val nedleggelsesdato: LocalDate? = null
)

/**
 * Details specific to organisasjonsledd
 */
data class OrganisasjonsleddDetaljer(
    val enhetstype: String? = null,
    val sektorkode: String? = null
)

/**
 * Organization unit type
 */
data class Enhetstype(
    val enhetstype: String? = null
)

/**
 * Industry/business sector information
 */
data class Naering(
    val naeringskode: String? = null,
    val hjelpeenhet: Boolean? = null
)

/**
 * Address information - simplified without discriminator since it causes issues
 */
data class Adresse(
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String? = null,
    val kommunenummer: String? = null
)

/**
 * Key information about an organization (simplified response)
 */
data class OrganisasjonNoekkelinfo(
    val organisasjonsnummer: String,
    val navn: Navn? = null,
    val enhetstype: String? = null,
    val adresse: Adresse? = null,
    val opphoersdato: LocalDate? = null
)

/**
 * Error response from EREG service
 */
data class TjenestefeilResponse(
    val melding: String? = null
)

/**
 * Wrapper for organizational units that a virksomhet or organisasjonsledd consists of
 */
data class BestaarAvOrganisasjonsledd(
    val organisasjonsledd: Organisasjonsledd? = null,
    val bruksperiode: Bruksperiode? = null,
    val gyldighetsperiode: Gyldighetsperiode? = null
)

/**
 * Wrapper for juridisk enhet references
 */
data class InngaarIJuridiskEnhet(
    val organisasjonsnummer: String? = null,
    val navn: Navn? = null,
    val bruksperiode: Bruksperiode? = null,
    val gyldighetsperiode: Gyldighetsperiode? = null
)

/**
 * Wrapper for virksomhet references
 */
data class DriverVirksomhet(
    val organisasjonsnummer: String? = null,
    val navn: Navn? = null,
    val bruksperiode: Bruksperiode? = null,
    val gyldighetsperiode: Gyldighetsperiode? = null
)

/**
 * Period when something was in use in the system
 */
data class Bruksperiode(
    val fom: String? = null,
    val tom: String? = null
)

/**
 * Period when something was valid
 */
data class Gyldighetsperiode(
    val fom: LocalDate? = null,
    val tom: LocalDate? = null
)
