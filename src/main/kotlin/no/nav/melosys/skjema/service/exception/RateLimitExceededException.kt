package no.nav.melosys.skjema.service.exception

import no.nav.melosys.skjema.service.RateLimitOperationType

class RateLimitExceededException(
    val operationType: RateLimitOperationType
) : RuntimeException("Rate limit overskredet for operasjon: $operationType")
