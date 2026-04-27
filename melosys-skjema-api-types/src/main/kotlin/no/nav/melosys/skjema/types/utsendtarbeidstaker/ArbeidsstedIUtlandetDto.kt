package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.types.felles.LandKode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsstedIUtlandetDto(
    val arbeidsstedType: ArbeidsstedType,
    @field:Valid val paLand: PaLandDto? = null,
    val offshore: OffshoreDto? = null,
    @field:Valid val paSkip: PaSkipDto? = null,
    @field:Valid val omBordPaFly: OmBordPaFlyDto? = null
)

enum class ArbeidsstedType {
    PA_LAND,
    OFFSHORE,
    PA_SKIP,
    OM_BORD_PA_FLY
}

enum class Farvann {
    INTERNASJONALT_FARVANN,
    TERRITORIALFARVANN
}

enum class FastEllerVekslendeArbeidssted {
    FAST,
    VEKSLENDE
}

enum class TypeInnretning {
    PLATTFORM_ELLER_ANNEN_FAST_INNRETNING,
    BORESKIP_ELLER_ANNEN_FLYTTBAR_INNRETNING
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffshoreDto(
    val navnPaVirksomhet: String,
    val navnPaInnretning: String,
    val typeInnretning: TypeInnretning,
    val sokkelLand: LandKode
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OmBordPaFlyDto(
    val navnPaVirksomhet: String,
    val hjemmebaseLand: LandKode,
    val hjemmebaseNavn: String,
    val erVanligHjemmebase: Boolean,
    val vanligHjemmebaseLand: LandKode?,
    val vanligHjemmebaseNavn: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaLandDto(
    val navnPaVirksomhet: String,
    val fastEllerVekslendeArbeidssted: FastEllerVekslendeArbeidssted,
    val fastArbeidssted: PaLandFastArbeidsstedDto?,
    val erHjemmekontor: Boolean
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaLandFastArbeidsstedDto(
    val vegadresse: String,
    val nummer: String,
    val postkode: String,
    val bySted: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaSkipDto(
    val navnPaVirksomhet: String,
    val navnPaSkip: String,
    val yrketTilArbeidstaker: String,
    val seilerI: Farvann,
    val flaggland: LandKode?,
    val territorialfarvannLand: LandKode?
)
