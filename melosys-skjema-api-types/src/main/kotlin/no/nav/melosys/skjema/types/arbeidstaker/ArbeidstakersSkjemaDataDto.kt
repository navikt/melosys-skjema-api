package no.nav.melosys.skjema.types.arbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakersSkjemaDataDto(
    val skjemaId: UUID? = null,
    val innsendtDato: Instant? = null,
    val erstatterSkjemaId: UUID? = null,
    val utenlandsoppdraget: UtenlandsoppdragetArbeidstakersDelDto? = null,
    val arbeidssituasjon: ArbeidssituasjonDto? = null,
    val skatteforholdOgInntekt: SkatteforholdOgInntektDto? = null,
    val familiemedlemmer: FamiliemedlemmerDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
)