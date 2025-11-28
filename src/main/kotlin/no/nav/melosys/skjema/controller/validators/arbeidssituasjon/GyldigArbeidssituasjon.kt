package no.nav.melosys.skjema.controller.validators.arbeidssituasjon

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ArbeidssituasjonValidator::class])
@MustBeDocumented
annotation class GyldigArbeidssituasjon(
    val message: String = "Ugyldig arbeidssituasjon",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
