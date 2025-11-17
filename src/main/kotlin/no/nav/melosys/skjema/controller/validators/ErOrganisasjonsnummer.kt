package no.nav.melosys.skjema.controller.validators

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [OrganisasjonsnummerValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ErOrganisasjonsnummer(
    val message: String = "Organisasjonsnummeret er ikke gyldig",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
