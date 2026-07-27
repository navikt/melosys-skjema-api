package no.nav.melosys.skjema.types.m2m

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.felles.GyldigSaksnummer

/** Massesynk av saksstatus fra melosys-api (historiske og aktive saker). Kaller paginerer i batcher. */
data class BulkOppdaterSaksstatusRequest(
    @field:NotEmpty(message = "Oppdateringer kan ikke være tom")
    @field:Size(max = 1000, message = "Maks 1000 oppdateringer per batch")
    @field:Valid
    val oppdateringer: List<SaksstatusOppdatering>
)

data class SaksstatusOppdatering(
    val skjemaId: UUID,
    @field:GyldigSaksnummer
    val saksnummer: String,
    val saksstatus: Saksstatus
)
