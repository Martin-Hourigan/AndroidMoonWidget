package dev.mahourigan.moonwidget.astronomy

/**
 * The twelve signs of the tropical zodiac, each occupying 30 degrees of
 * ecliptic longitude starting from the vernal equinox at 0 degrees Aries.
 *
 * Note this is the *tropical* zodiac used in Western astrology, which is tied
 * to the equinox rather than to where the constellations actually sit today.
 * Because of precession the two have drifted roughly a full sign apart, so the
 * sign reported here is not the constellation the Moon is visually in front of.
 *
 * Only the symbol is carried here — it is language-neutral. Names and
 * descriptions live in string resources; see `ui/DisplayStrings.kt`.
 */
enum class ZodiacSign(
    private val glyph: String,
    val element: Element,
) {
    ARIES("♈", Element.FIRE),
    TAURUS("♉", Element.EARTH),
    GEMINI("♊", Element.AIR),
    CANCER("♋", Element.WATER),
    LEO("♌", Element.FIRE),
    VIRGO("♍", Element.EARTH),
    LIBRA("♎", Element.AIR),
    SCORPIO("♏", Element.WATER),
    SAGITTARIUS("♐", Element.FIRE),
    CAPRICORN("♑", Element.EARTH),
    AQUARIUS("♒", Element.AIR),
    PISCES("♓", Element.WATER);

    /**
     * The sign's symbol, forced to text presentation.
     *
     * U+2648..U+2653 default to *emoji* presentation, so a bare glyph renders as
     * a coloured badge that clashes with surrounding text. Appending
     * VARIATION SELECTOR-15 (U+FE0E) asks for the monochrome text form, which
     * takes the colour of the text around it.
     */
    val symbol: String get() = glyph + TEXT_PRESENTATION

    enum class Element { FIRE, EARTH, AIR, WATER }

    companion object {
        private const val DEGREES_PER_SIGN = 30.0

        /** VARIATION SELECTOR-15: request the text glyph rather than the emoji. */
        const val TEXT_PRESENTATION = "\uFE0E"

        /** The sign containing a given ecliptic longitude. */
        fun forLongitude(eclipticLongitude: Double): ZodiacSign {
            val index = (normalizeDegrees(eclipticLongitude) / DEGREES_PER_SIGN).toInt()
            return entries[index.coerceIn(0, entries.size - 1)]
        }
    }
}

/** Where the Moon sits within its current sign. */
data class MoonSign(
    val sign: ZodiacSign,
    /** Degrees into the sign, `0.0` up to but not including `30.0`. */
    val degreesIntoSign: Double,
) {
    /** Whole degrees into the sign, for display as e.g. "17° Sagittarius". */
    val wholeDegrees: Int get() = degreesIntoSign.toInt()
}

object MoonZodiac {
    fun at(julianDay: Double): MoonSign {
        val longitude = normalizeDegrees(CelestialPositions.moon(julianDay).longitude)
        return MoonSign(
            sign = ZodiacSign.forLongitude(longitude),
            degreesIntoSign = longitude % 30.0,
        )
    }
}
