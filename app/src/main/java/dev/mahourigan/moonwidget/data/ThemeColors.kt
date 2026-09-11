package dev.mahourigan.moonwidget.data

/**
 * The five colours a theme is built from, as opaque ARGB.
 *
 * Deliberately not "primary / secondary / accent": those are brand-palette
 * names, and this app paints a page, cards, text and a Moon. Naming the roles
 * after what they colour means a custom theme has no invisible surprises.
 *
 * Two roles that could look redundant but are not:
 *
 *  - [moon] is separate from [text] even though the default theme uses one
 *    colour for both. Without the split there is no way to have a warm orange
 *    Moon above cool white text.
 *  - [accent] exists because selection had no colour of its own before, so a
 *    ticked box looked much like an unticked one.
 *
 * Muted text and the Moon's rim are derived rather than stored — see
 * `ui/Palette.kt`. Five choices is about the limit of what is enjoyable to
 * pick by hand.
 *
 * No Android imports here on purpose, so the presets can be contrast-checked
 * in plain JVM tests.
 */
data class ThemeColors(
    /** The page behind everything, and the widget tile. */
    val background: Long,
    /** Cards and rows. Also the Moon's unlit side. */
    val surface: Long,
    /** Headings and values. */
    val text: Long,
    /** The Moon's lit face. */
    val moon: Long,
    /** Ticked boxes, selected rows, the supermoon badge. */
    val accent: Long,
) {
    companion object {
        /**
         * The colours the app shipped with, and the starting point the custom
         * editor opens on — better to begin from something that works than
         * from five identical greys.
         */
        val MIDNIGHT = ThemeColors(
            background = 0xFF0B1026,
            surface = 0xFF272B3D,
            text = 0xFFF2EFE4,
            moon = 0xFFF2EFE4,
            accent = 0xFF8FA7FF,
        )
    }
}

/** One editable slot in a custom theme. */
enum class ThemeRole {
    BACKGROUND,
    SURFACE,
    TEXT,
    MOON,
    ACCENT,
}

fun ThemeColors.valueOf(role: ThemeRole): Long = when (role) {
    ThemeRole.BACKGROUND -> background
    ThemeRole.SURFACE -> surface
    ThemeRole.TEXT -> text
    ThemeRole.MOON -> moon
    ThemeRole.ACCENT -> accent
}

fun ThemeColors.with(role: ThemeRole, argb: Long): ThemeColors = when (role) {
    ThemeRole.BACKGROUND -> copy(background = argb)
    ThemeRole.SURFACE -> copy(surface = argb)
    ThemeRole.TEXT -> copy(text = argb)
    ThemeRole.MOON -> copy(moon = argb)
    ThemeRole.ACCENT -> copy(accent = argb)
}

/**
 * The built-in themes, plus [CUSTOM] for hand-picked colours.
 *
 * Names are string resources — see `ui/DisplayStrings.kt` — so this stays
 * free of display text like everything else in the data and astronomy layers.
 */
enum class MoonTheme(private val preset: ThemeColors?) {
    /** The default: deep blue night sky, bone-white Moon. */
    MIDNIGHT(ThemeColors.MIDNIGHT),

    /** True black, for OLED screens where black costs no power. */
    ECLIPSE(
        ThemeColors(
            background = 0xFF000000,
            surface = 0xFF16161A,
            text = 0xFFE8E8EA,
            moon = 0xFFFFFFFF,
            accent = 0xFFFF6B4A,
        )
    ),

    /** Warm autumn: a low orange Moon over brown earth. */
    HARVEST(
        ThemeColors(
            background = 0xFF1A1008,
            surface = 0xFF2E1E10,
            text = 0xFFF5E6D3,
            moon = 0xFFFFA94D,
            accent = 0xFFFFD43B,
        )
    ),

    /** Cool green-blue, like moonlight over water. */
    SEA_GLASS(
        ThemeColors(
            background = 0xFF06201F,
            surface = 0xFF0F3330,
            text = 0xFFDFF2EE,
            moon = 0xFFA8E6CF,
            accent = 0xFF4DD0B1,
        )
    ),

    /** The copper-red Moon of a total lunar eclipse. */
    BLOOD_MOON(
        ThemeColors(
            background = 0xFF14060A,
            surface = 0xFF2E1018,
            text = 0xFFF0DDE2,
            moon = 0xFFD1553F,
            accent = 0xFFE8836E,
        )
    ),

    /**
     * The one light theme. The Moon is drawn dark on pale paper, like an
     * engraving — a cream Moon would simply vanish.
     */
    PARCHMENT(
        ThemeColors(
            background = 0xFFF3EDE1,
            surface = 0xFFE2D8C4,
            text = 0xFF2E2A22,
            moon = 0xFF3E3A2F,
            accent = 0xFF9A5B21,
        )
    ),

    /**
     * For red-green colour blindness, which is by far the most common form.
     *
     * Protanopia and deuteranopia both leave the blue-yellow axis intact, so
     * the Moon is amber and the accent is a strong blue.
     *
     * The obvious Okabe-Ito pairing — their yellow against their sky blue —
     * fails: yellow is red plus green, so a protanope sees it darken until it
     * sits only 1.5:1 from the blue. ColourBlindThemeTest caught that. Pale
     * amber against a deeper blue keeps them 3.7:1 apart under all three
     * dichromacies, because the separation is in brightness rather than hue,
     * and brightness is the one thing colour blindness leaves alone.
     */
    AMBER_SKY(
        ThemeColors(
            background = 0xFF10131A,
            surface = 0xFF262C3A,
            text = 0xFFF2F2F2,
            moon = 0xFFFFF3B0,
            accent = 0xFF1F78B4,
        )
    ),

    /**
     * For blue-yellow colour blindness.
     *
     * Tritanopia confuses blue with green and yellow with pink, but leaves red
     * and green far apart, so the accent is vermillion against a plain warm
     * white Moon. Rarer than the red-green forms, and it needs the opposite
     * choice of hues, which is why one theme cannot serve both.
     */
    EMBER(
        ThemeColors(
            background = 0xFF14100E,
            surface = 0xFF2B2320,
            text = 0xFFF4EDE9,
            moon = 0xFFF7F2EC,
            accent = 0xFFE0603A,
        )
    ),

    /** Whatever the user picked; the real values live in [Settings.customTheme]. */
    CUSTOM(null),
    ;

    /**
     * For every theme but [CUSTOM], the colours themselves. For [CUSTOM] this
     * is only the seed the editor opens on — read [Settings.palette] instead
     * when you want what is actually on screen.
     */
    val colors: ThemeColors get() = preset ?: ThemeColors.MIDNIGHT

    /** True when these colours are the user's to edit. */
    val isCustom: Boolean get() = preset == null

    companion object {
        val DEFAULT = MIDNIGHT

        fun fromNameOrDefault(name: String?): MoonTheme =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
