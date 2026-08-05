package no.nav.melosys.skjema.types.felles

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlin.reflect.KClass

/**
 * Felles validering av saksnummer fra melosys-api. Maks-lengden må matche
 * kolonnen innsending.saksnummer (VARCHAR(99), V16).
 */
@NotBlank(message = "Saksnummer kan ikke være tomt")
@Size(max = 99, message = "Saksnummer kan ikke være lengre enn 99 tegn")
@Constraint(validatedBy = [])
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class GyldigSaksnummer(
    val message: String = "Ugyldig saksnummer",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
