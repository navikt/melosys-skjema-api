package no.nav.melosys.skjema.controller.validators.utenlandsoppdraget

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UtenlandsoppdragetValidator::class])
@MustBeDocumented
annotation class GyldigUtenlandsoppdrag(
    val message: String = "Ugyldig utenlandsoppdrag",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
