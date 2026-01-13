package no.nav.melosys.skjema.controller.validators.familiemedlemmer

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [FamiliemedlemmerValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GyldigFamiliemedlemmer(
    val message: String = "Familiemedlemmer er ikke gyldig",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
