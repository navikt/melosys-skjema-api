package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PaSkipValidator::class])
@MustBeDocumented
annotation class GyldigPaSkip(
    val message: String = "Ugyldig på skip",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
