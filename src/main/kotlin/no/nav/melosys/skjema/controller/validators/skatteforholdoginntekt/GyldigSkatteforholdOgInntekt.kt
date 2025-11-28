package no.nav.melosys.skjema.controller.validators.skatteforholdoginntekt

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [SkatteforholdOgInntektValidator::class])
@MustBeDocumented
annotation class GyldigSkatteforholdOgInntekt(
    val message: String = "Ugyldig skatteforhold og inntekt",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
