package no.nav.melosys.skjema.controller.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class FodselsnummerValidator(
    @Value("\${validation.fodselsnummer.valider-syntetisk}")
    val validerSyntetiskFnr: Boolean
) : ConstraintValidator<ErFodselsnummer?, String?> {


    override fun initialize(constraintAnnotation: ErFodselsnummer?) {}

    override fun isValid(
        fodselsnummer: String?,
        cxt: ConstraintValidatorContext
    ): Boolean {
        // Null values are handled by @NotNull annotation
        if (fodselsnummer == null) return true

        return try {
            validerInput(fodselsnummer, erSyntetisk = validerSyntetiskFnr)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    // Implementasjon hentet fra https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/dokumenter/foedselsEllerDNummerValidator.java
    companion object {
        private val vekterK1 = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        private val vekterK2 = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        private val gyldigRestK1 = intArrayOf(0, 1, 2, 3)
        private const val GYLDIG_REST_K2 = 0
        private val dNummerSifre = intArrayOf(4, 5, 6, 7)

        /**
         * Validerer et fødsels-eller-d-nummer(1964 og 2032-type) ved å sjekke kontrollsifrene iht.
         * https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/nytt-fodselsnummer-fra-2032/
         *
         * @param fnrdnr 11-siffret fødsels-eller-D-nummer som skal valideres.
         * @return true hvis fødsels-eller-D-nummer er gyldig, ellers false
         */
        private fun validerKontrollsifferFoedselsEllerDnummer(fnrdnr: String): Boolean {
            val sifre = konverterTilIntArray(fnrdnr)
            val gittK1 = sifre[9]
            val gittK2 = sifre[10]

            val grunnlagK1 = sifre.copyOfRange(0, vekterK1.size)
            val vektetK1 = grunnlagK1.indices.sumOf { i -> grunnlagK1[i] * vekterK1[i] }

            val beregnetRestSifferK1 = (vektetK1 + gittK1) % 11

            if (beregnetRestSifferK1 !in gyldigRestK1) {
                return false
            }

            val grunnlagK2 = sifre.copyOfRange(0, vekterK2.size)
            val vektetK2 = grunnlagK2.indices.sumOf { i -> grunnlagK2[i] * vekterK2[i] }

            val beregnetRestSifferK2 = (vektetK2 + gittK2) % 11

            return beregnetRestSifferK2 == GYLDIG_REST_K2
        }

        /**
         * Konverterer en streng til et array av heltall.
         *
         * @param streng strengen som skal konverteres
         * @return array av heltall
         */
        private fun konverterTilIntArray(streng: String): IntArray {
            return streng.map { it.toString().toInt() }.toIntArray()
        }

        /**
         * Validerer at gitt ID har gyldig format og dato før den kaller selve valideringen.
         *
         * @param gittNummer  ID-nummer som skal valideres.
         * @param erSyntetisk Angir om ID-nummeret er syntetisk.
         * @return true hvis ID-nummeret er gyldig, ellers kaster en IllegalArgumentException.
         * @throws IllegalArgumentException hvis ID-nummeret har ugyldig format eller ikke er gyldig bygget opp.
         */
        private fun validerInput(gittNummer: String, erSyntetisk: Boolean = false): Boolean {
            val gyldigFormat = gittNummer.matches(Regex("^\\d{11}$"))

            if (!gyldigFormat) {
                throw IllegalArgumentException("Ugyldig format: ID må være 11 sifre")
            }

            var dato = gittNummer.substring(0, 6)

            if (erDnummer(gittNummer)) {
                val dagSiffer = dato[0].digitToInt()
                dato = (dagSiffer - 4).toString() + dato.substring(1, 6)
            }

            if (erSyntetisk) {
                val maanedSiffer = dato[2].digitToInt()
                if (maanedSiffer < 8) {
                    throw IllegalArgumentException("Ugyldig format: $gittNummer syntetiske nummer må ha 8 eller 9 på indeks 2")
                }
                // utled kalenderdato fra syntetisk nummer
                dato = dato.substring(0, 2) + (maanedSiffer - 8) + dato.substring(3, 6)
            }

            if (!erDatoGyldig(dato)) {
                throw IllegalArgumentException(
                    "Ugyldig format: $gittNummer har ugyldig dato $dato i formatet ddMMyy"
                )
            }

            if (!validerKontrollsifferFoedselsEllerDnummer(gittNummer)) {
                throw IllegalArgumentException(
                    "Ugyldig ID: $gittNummer er ikke gyldig bygget opp som ${if (erSyntetisk) "syntetisk" else "reelt"} nummer"
                )
            }
            return true
        }

        /**
         * Sjekker om et gitt nummer er et D-nummer.
         *
         * @param gittNummer Nummeret som skal sjekkes.
         * @return true hvis nummeret er et D-nummer, ellers false.
         */
        private fun erDnummer(gittNummer: String): Boolean {
            return gittNummer[0].digitToInt() in dNummerSifre
        }

        /**
         * Sjekker om en gitt dato finnes på en kalender. Da århundre ikke lengre vil kunne utledes av
         * 2032-fødselsnumre, antas alle datoer å være etter år 2000.
         *
         * @param dato Datoen som skal sjekkes i formatet ddMMyy.
         * @return true hvis datoen er gyldig, ellers false.
         */
        private fun erDatoGyldig(dato: String): Boolean {
            val aarhundre = "20"
            val aar = dato.substring(4, 6)
            val maaned = dato.substring(2, 4)
            val dag = dato.substring(0, 2)
            val erSkuddag = dag + maaned == "2902"

            if (erSkuddag && !erSkuddaar(aar)) {
                return false
            }

            return try {
                LocalDate.parse(dag + maaned + aarhundre + aar, DateTimeFormatter.ofPattern("ddMMyyyy"))
                true
            } catch (e: DateTimeParseException) {
                false
            }
        }

        /**
         * Utleder om et gitt år er et skuddår basert på kun to sifre. Dette medfører at man ikke
         * kan vite hvilket århundre det gjelder, så velger å anse '00' som skuddåret 2000.
         * Dette er grunnet i det ikke lengre vil være mulig å utlede århundre av 2032-fødselsnumre.
         *
         * @param aar Året som skal sjekkes i formatet 'yy'.
         * @return true hvis året er et skuddår, ellers false.
         */
        private fun erSkuddaar(aar: String): Boolean {
            return aar.toInt() % 4 == 0
        }
    }
}
