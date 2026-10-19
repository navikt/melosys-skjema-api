package no.nav.melosys.skjema.config.observability

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.trace.Span

class TraceparentTest : FunSpec({

    val traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

    test("nåværende gir null uten aktiv trace") {
        Traceparent.nåværende().shouldBeNull()
    }

    test("medTraceparent gjør lagret trace-kontekst gjeldende") {
        Traceparent.medTraceparent(traceparent) {
            Span.current().spanContext.traceId shouldBe "4bf92f3577b34da6a3ce929d0e0e4736"
            Traceparent.nåværende() shouldBe traceparent
        }
        Traceparent.nåværende().shouldBeNull()
    }

    test("medTraceparent kjører blokken uten kontekst når traceparent mangler") {
        Traceparent.medTraceparent(null) { "ok" } shouldBe "ok"
    }

    test("medTraceparent tåler ugyldig traceparent") {
        Traceparent.medTraceparent("ugyldig") {
            Span.current().spanContext.isValid shouldBe false
        }
    }
})
