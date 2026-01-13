package no.nav.melosys.skjema.controller.validators.familiemedlemmer

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [FamiliemedlemValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GyldigFamiliemedlem(
    val message: String = "Familiemedlem er ikke gyldig",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
