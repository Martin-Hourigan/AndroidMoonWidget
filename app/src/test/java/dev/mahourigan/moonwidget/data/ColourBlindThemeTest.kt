package dev.mahourigan.moonwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two colour-blind presets, checked against a simulation rather than taken
 * on trust.
 *
 * Picking colours off an accessible palette is a good start, but it is not
 * evidence. These run every pairing through [simulate] for all three
 * dichromacies and re-apply the same contrast floors the sighted palettes have
 * to meet, so a preset that collapses under one of them fails here.
 */
class ColourBlindThemeTest {

    private val accessible = listOf(MoonTheme.AMBER_SKY, MoonTheme.EMBER)
    private val everyVision = ColourVision.entries

    /** Body text has to clear 4.5:1 however the eye behind it works. */
    @Test
    fun `text stays readable under every dichromacy`() {
        accessible.forEach { theme ->
            everyVision.forEach { vision ->
                val text = simulate(theme.colors.text, vision)
                val background = simulate(theme.colors.background, vision)
                val ratio = contrastRatio(text, background)

                assertTrue(
                    "$theme text is only ${"%.1f".format(ratio)}:1 under $vision",
                    ratio >= 4.5,
                )
            }
        }
    }

    @Test
    fun `text stays readable on cards under every dichromacy`() {
        accessible.forEach { theme ->
            everyVision.forEach { vision ->
                val ratio = contrastRatio(
                    simulate(theme.colors.text, vision),
                    simulate(theme.colors.surface, vision),
                )
                assertTrue(
                    "$theme text-on-card is only ${"%.1f".format(ratio)}:1 under $vision",
                    ratio >= 4.5,
                )
            }
        }
    }

    @Test
    fun `the Moon stays visible under every dichromacy`() {
        accessible.forEach { theme ->
            everyVision.forEach { vision ->
                val ratio = contrastRatio(
                    simulate(theme.colors.moon, vision),
                    simulate(theme.colors.background, vision),
                )
                assertTrue(
                    "$theme Moon is only ${"%.1f".format(ratio)}:1 under $vision",
                    ratio >= 3.0,
                )
            }
        }
    }

    /**
     * The point of the accent is to say "this one is ticked". If it drops to
     * the same tone as the page under some form of colour blindness then it has
     * stopped saying anything.
     */
    @Test
    fun `the accent stays visible under every dichromacy`() {
        accessible.forEach { theme ->
            everyVision.forEach { vision ->
                val ratio = contrastRatio(
                    simulate(theme.colors.accent, vision),
                    simulate(theme.colors.background, vision),
                )
                assertTrue(
                    "$theme accent is only ${"%.1f".format(ratio)}:1 under $vision",
                    ratio >= 3.0,
                )
            }
        }
    }

    /**
     * Moon and accent are the two coloured things on screen, and this is the
     * pairing a red-green palette most easily gets wrong: yellow and green look
     * alike to a deuteranope, orange and red to a protanope. They have to stay
     * apart in brightness, since hue is the thing that cannot be relied on.
     */
    @Test
    fun `moon and accent never collapse into each other`() {
        accessible.forEach { theme ->
            everyVision.forEach { vision ->
                val ratio = contrastRatio(
                    simulate(theme.colors.moon, vision),
                    simulate(theme.colors.accent, vision),
                )
                assertTrue(
                    "$theme moon and accent are ${"%.1f".format(ratio)}:1 apart under $vision",
                    ratio >= 1.6,
                )
            }
        }
    }

    // --- The simulation itself ---

    @Test
    fun `grey is unchanged by any dichromacy`() {
        // No colour to lose, so every transform must leave it where it was.
        listOf(0xFF000000L, 0xFF808080L, 0xFFFFFFFFL).forEach { grey ->
            everyVision.forEach { vision ->
                val simulated = simulate(grey, vision)
                val drift = contrastRatio(grey, simulated)
                assertTrue(
                    "${hexOf(grey)} moved to ${hexOf(simulated)} under $vision",
                    drift < 1.05,
                )
            }
        }
    }

    @Test
    fun `red and green converge for a deuteranope`() {
        // The defining symptom. If the transform does not reproduce it, the
        // whole check above is worthless.
        val red = simulate(0xFFFF0000, ColourVision.DEUTERANOPIA)
        val green = simulate(0xFF00FF00, ColourVision.DEUTERANOPIA)

        val apartNormally = contrastRatio(0xFFFF0000, 0xFF00FF00)
        val apartSimulated = contrastRatio(red, green)

        assertTrue(
            "red and green should look closer, not further: $apartNormally -> $apartSimulated",
            apartSimulated < apartNormally,
        )
    }

    @Test
    fun `simulated colours stay opaque and in range`() {
        MoonTheme.entries.forEach { theme ->
            ThemeRole.entries.forEach { role ->
                everyVision.forEach { vision ->
                    val simulated = simulate(theme.colors.valueOf(role), vision)
                    assertEquals(
                        "$theme $role lost its alpha under $vision",
                        0xFF000000L,
                        simulated and 0xFF000000L,
                    )
                    assertTrue(simulated and 0xFFFFFF <= 0xFFFFFF)
                }
            }
        }
    }
}
