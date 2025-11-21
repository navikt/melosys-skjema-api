package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ArbeidsstedIUtlandetValidator::class])
@MustBeDocumented
annotation class GyldigArbeidsstedIUtlandet(
    val message: String = "Ugyldig arbeidssted i utlandet",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)