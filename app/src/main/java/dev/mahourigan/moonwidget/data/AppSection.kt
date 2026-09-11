package dev.mahourigan.moonwidget.data

/**
 * The movable cards on the main screen, in their factory order.
 *
 * The Moon itself and the footer are not here: the drawing is what the screen
 * is for, and the location line and buttons belong at the bottom. Everything
 * between them can be put in any order.
 */
enum class AppSection {
    RISE_SET,
    SKY_PATH,
    TIMING,
    DISTANCE,
    MOON_SIGN,
    SUN_SIGN,
    ;

    companion object {
        val DEFAULT_ORDER: List<AppSection> = entries.toList()

        /** Stored as one string, since DataStore has no ordered-list primitive. */
        fun serialize(order: List<AppSection>): String = order.joinToString(",") { it.name }

        /**
         * Read an order back, tolerating anything unexpected.
         *
         * Names that no longer exist are dropped and sections missing from the
         * stored value are appended in declaration order, so adding a card in a
         * later version slots it in rather than wiping the saved arrangement.
         */
        fun deserialize(stored: String?): List<AppSection> {
            val known = stored
                .orEmpty()
                .split(',')
                .mapNotNull { name -> entries.firstOrNull { it.name == name.trim() } }
                .distinct()

            return known + DEFAULT_ORDER.filterNot { it in known }
        }
    }
}

/**
 * Which typeface the app and widget draw with.
 *
 * All four are families Android already has, so nothing is bundled and the APK
 * does not grow. The exact shapes vary by manufacturer — Samsung ships its own
 * defaults — which is fine: these are requests for a style, not a promise of a
 * specific font.
 */
enum class AppFont {
    SYSTEM,
    SERIF,
    MONOSPACE,
    CONDENSED,
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromNameOrDefault(name: String?): AppFont =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * Overall text size, as a multiplier on every size in the app.
 *
 * Named steps rather than a free number: the gap between 104% and 106% is not
 * worth choosing between, and fixed steps cannot land somewhere that breaks a
 * layout.
 */
enum class TextSize(val scale: Float) {
    SMALL(0.85f),
    DEFAULT(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
    ;

    companion object {
        fun fromNameOrDefault(name: String?): TextSize =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
