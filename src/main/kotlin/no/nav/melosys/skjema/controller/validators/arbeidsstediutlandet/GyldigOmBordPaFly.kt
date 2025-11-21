package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [OmBordPaFlyValidator::class])
@MustBeDocumented
annotation class GyldigOmBordPaFly(
    val message: String = "Ugyldig om bord på fly",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
