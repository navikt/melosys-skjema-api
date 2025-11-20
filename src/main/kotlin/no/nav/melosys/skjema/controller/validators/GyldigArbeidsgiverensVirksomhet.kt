package no.nav.melosys.skjema.controller.validators

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ArbeidsgiverensVirksomhetINorgeValidator::class])
@MustBeDocumented
annotation class GyldigArbeidsgiverensVirksomhet(
    val message: String = "Ugyldig arbeidsgiverens virksomhet",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
