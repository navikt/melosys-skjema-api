package no.nav.melosys.skjema.config.observability

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter

/**
 * Lagrer og gjenoppretter W3C trace-kontekst (`traceparent`) for prosessering som skjer
 * utenfor den opprinnelige requesten, f.eks. retry fra scheduler. Da får loggene samme
 * `trace_id` som innsendingen, og prosesseringen havner i samme trace i Tempo.
 */
object Traceparent {
    private const val TRACEPARENT = "traceparent"
    private val propagator = W3CTraceContextPropagator.getInstance()

    fun nåværende(): String? {
        val carrier = mutableMapOf<String, String>()
        propagator.inject(Context.current(), carrier, MapSetter)
        return carrier[TRACEPARENT]
    }

    fun <T> medTraceparent(traceparent: String?, block: () -> T): T {
        if (traceparent == null) return block()
        val context = propagator.extract(Context.current(), mapOf(TRACEPARENT to traceparent), MapGetter)
        return context.makeCurrent().use { block() }
    }

    private object MapSetter : TextMapSetter<MutableMap<String, String>> {
        override fun set(carrier: MutableMap<String, String>?, key: String, value: String) {
            carrier?.put(key, value)
        }
    }

    private object MapGetter : TextMapGetter<Map<String, String>> {
        override fun keys(carrier: Map<String, String>): Iterable<String> = carrier.keys
        override fun get(carrier: Map<String, String>?, key: String): String? = carrier?.get(key)
    }
}
