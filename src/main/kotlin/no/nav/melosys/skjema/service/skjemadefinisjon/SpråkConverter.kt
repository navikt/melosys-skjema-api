package no.nav.melosys.skjema.service.skjemadefinisjon

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * JPA AttributeConverter for Språk enum.
 *
 * Konverterer mellom Språk enum og språkkode-string ("nb", "en") i databasen.
 * Brukes automatisk på alle Språk-felter i entities.
 */
@Converter(autoApply = true)
class SpråkConverter : AttributeConverter<Språk, String> {

    override fun convertToDatabaseColumn(språk: Språk?): String? {
        return språk?.kode
    }

    override fun convertToEntityAttribute(kode: String?): Språk? {
        return kode?.let { Språk.fraKode(it) }
    }
}
