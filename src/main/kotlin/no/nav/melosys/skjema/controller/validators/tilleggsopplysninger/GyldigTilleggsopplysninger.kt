package no.nav.melosys.skjema.controller.validators.tilleggsopplysninger

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TilleggsopplysningerValidator::class])
@MustBeDocumented
annotation class GyldigTilleggsopplysninger(
    val message: String = "Ugyldig tilleggsopplysninger",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
