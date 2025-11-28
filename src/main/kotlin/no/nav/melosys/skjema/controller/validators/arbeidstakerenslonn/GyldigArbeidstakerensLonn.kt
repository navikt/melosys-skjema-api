package no.nav.melosys.skjema.controller.validators.arbeidstakerenslonn

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ArbeidstakerensLonnValidator::class])
@MustBeDocumented
annotation class GyldigArbeidstakerensLonn(
    val message: String = "Ugyldig arbeidstakerens lønn",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
