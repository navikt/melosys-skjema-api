package no.nav.melosys.skjema.controller.validators.felles

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PeriodeValidator::class])
@MustBeDocumented
annotation class GyldigPeriode(
    val message: String = "FraDato må være før eller lik tilDato",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
