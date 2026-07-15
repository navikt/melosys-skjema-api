package no.nav.melosys.skjema.types.m2m

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID
import no.nav.melosys.skjema.types.common.Saksstatus

/** Massesynk av saksstatus fra melosys-api (historiske og aktive saker). */
data class BulkOppdaterSaksstatusRequest(
    @field:NotEmpty(message = "Oppdateringer kan ikke være tom")
    @field:Valid
    val oppdateringer: List<SaksstatusOppdatering>
)

data class SaksstatusOppdatering(
    val skjemaId: UUID,
    @field:NotBlank(message = "Saksnummer kan ikke være tomt")
    @field:Size(max = 99, message = "Saksnummer kan ikke være lengre enn 99 tegn")
    val saksnummer: String,
    val saksstatus: Saksstatus
)
