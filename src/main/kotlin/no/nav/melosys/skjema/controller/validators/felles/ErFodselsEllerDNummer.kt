package no.nav.melosys.skjema.controller.validators.felles

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [ErFodselsEllerDNummerValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ErFodselsEllerDNummer(
    val message: String = "Fødselsnummeret er ikke gyldig",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
