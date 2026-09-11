package dev.mahourigan.moonwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ThemeTest {

    // --- Roles ---

    @Test
    fun `every role round-trips through get and set`() {
        ThemeRole.entries.forEach { role ->
            val changed = ThemeColors.MIDNIGHT.with(role, 0xFF123456)
            assertEquals("$role did not store its value", 0xFF123456L, changed.valueOf(role))
        }
    }

    @Test
    fun `setting one role leaves the others alone`() {
        ThemeRole.entries.forEach { role ->
            val changed = ThemeColors.MIDNIGHT.with(role, 0xFF123456)
            ThemeRole.entries.filter { it != role }.forEach { other ->
                assertEquals(
                    "$role bled into $other",
                    ThemeColors.MIDNIGHT.valueOf(other),
                    changed.valueOf(other),
                )
            }
        }
    }

    // --- Themes ---

    @Test
    fun `only CUSTOM is editable`() {
        assertTrue(MoonTheme.CUSTOM.isCustom)
        MoonTheme.entries.filter { it != MoonTheme.CUSTOM }.forEach {
            assertTrue("$it should be a fixed preset", !it.isCustom)
        }
    }

    @Test
    fun `an unknown or missing stored name falls back to the default`() {
        assertEquals(MoonTheme.DEFAULT, MoonTheme.fromNameOrDefault(null))
        assertEquals(MoonTheme.DEFAULT, MoonTheme.fromNameOrDefault("ROSE_QUARTZ"))
        assertEquals(MoonTheme.HARVEST, MoonTheme.fromNameOrDefault("HARVEST"))
    }

    @Test
    fun `settings resolve custom colours only when custom is selected`() {
        val mine = ThemeColors.MIDNIGHT.with(ThemeRole.MOON, 0xFF00FF00)

        val onPreset = Settings(theme = MoonTheme.HARVEST, customTheme = mine)
        assertEquals(MoonTheme.HARVEST.colors, onPreset.palette)

        val onCustom = Settings(theme = MoonTheme.CUSTOM, customTheme = mine)
        assertEquals(mine, onCustom.palette)
    }

    /**
     * The whole point of the split is that a theme can colour the Moon
     * differently from its text. If no preset ever does, the extra role is
     * dead weight and the presets are not using what they were built for.
     */
    @Test
    fun `at least one preset gives the Moon its own colour`() {
        val distinct = MoonTheme.entries
            .filterNot { it.isCustom }
            .count { it.colors.moon != it.colors.text }
        assertTrue("No preset distinguishes moon from text", distinct >= 2)
    }

    @Test
    fun `no preset reuses the text colour as its accent`() {
        MoonTheme.entries.filterNot { it.isCustom }.forEach {
            assertNotEquals("$it has no distinct accent", it.colors.text, it.colors.accent)
        }
    }

    // --- Readability ---
    //
    // A preset that ships unreadable is a bug the compiler cannot see, so the
    // WCAG floors are asserted here: 4.5:1 for body text, 3:1 for shapes.

    @Test
    fun `preset text is readable on its background`() {
        MoonTheme.entries.filterNot { it.isCustom }.forEach {
            val ratio = contrastRatio(it.colors.text, it.colors.background)
            assertTrue("$it text contrast is only ${"%.1f".format(ratio)}:1", ratio >= 4.5)
        }
    }

    @Test
    fun `preset muted text stays legible once its alpha is applied`() {
        MoonTheme.entries.filterNot { it.isCustom }.forEach {
            // Mirrors ui/Palette.kt's MUTED_ALPHA.
            val muted = blend(it.colors.text, it.colors.background, 0.62f)
            val ratio = contrastRatio(muted, it.colors.background)
            assertTrue("$it muted contrast is only ${"%.1f".format(ratio)}:1", ratio >= 3.0)
        }
    }

    @Test
    fun `preset text is readable on cards as well as on the page`() {
        MoonTheme.entries.filterNot { it.isCustom }.forEach {
            val ratio = contrastRatio(it.colors.text, it.colors.surface)
            assertTrue("$it text-on-card contrast is only ${"%.1f".format(ratio)}:1", ratio >= 4.5)
        }
    }

    @Test
    fun `the preset Moon stands out from what is behind it`() {
        MoonTheme.entries.filterNot { it.isCustom }.forEach {
            // The lit face sits against the page; the unlit side is the surface
            // colour, so the two must also differ or the disc has no shape.
            val onPage = contrastRatio(it.colors.moon, it.colors.background)
            val onShadow = contrastRatio(it.colors.moon, it.colors.surface)
            assertTrue("$it Moon is invisible on the page", onPage >= 3.0)
            assertTrue("$it lit and unlit sides are indistinguishable", onShadow >= 1.5)
        }
    }

    @Test
    fun `preset accents are visible against the page`() {
        MoonTheme.entries.filterNot { it.isCustom }.forEach {
            val ratio = contrastRatio(it.colors.accent, it.colors.background)
            assertTrue("$it accent contrast is only ${"%.1f".format(ratio)}:1", ratio >= 3.0)
        }
    }

    @Test
    fun `cards are distinguishable from the page`() {
        MoonTheme.entries.filterNot { it.isCustom }.forEach {
            assertNotEquals("$it cards vanish into the page", it.colors.surface, it.colors.background)
        }
    }

    @Test
    fun `every preset colour is fully opaque`() {
        MoonTheme.entries.forEach { theme ->
            ThemeRole.entries.forEach { role ->
                assertEquals(
                    "$theme $role is not opaque",
                    0xFF000000L,
                    theme.colors.valueOf(role) and 0xFF000000L,
                )
            }
        }
    }

    // --- Colour maths ---

    @Test
    fun `hex parsing accepts six digits with or without a hash`() {
        assertEquals(0xFF1A2B3CL, parseHex("1A2B3C"))
        assertEquals(0xFF1A2B3CL, parseHex("#1A2B3C"))
        assertEquals(0xFFFFFFFFL, parseHex("ffffff"))
    }

    @Test
    fun `hex parsing rejects anything else`() {
        assertNull(parseHex(""))
        assertNull(parseHex("12345"))
        assertNull(parseHex("1234567"))
        assertNull(parseHex("12345G"))
        assertNull(parseHex("#GGGGGG"))
    }

    @Test
    fun `hex formatting drops the alpha and round-trips`() {
        assertEquals("#1A2B3C", hexOf(0xFF1A2B3C))
        assertEquals("#000000", hexOf(0xFF000000))
        assertEquals(0xFF1A2B3CL, parseHex(hexOf(0xFF1A2B3C)))
    }

    @Test
    fun `HSV round-trips every preset colour exactly`() {
        MoonTheme.entries.forEach { theme ->
            ThemeRole.entries.forEach { role ->
                val original = theme.colors.valueOf(role)
                val hsv = rgbToHsv(original)
                assertEquals(
                    "$theme $role did not survive the round trip",
                    original,
                    hsvToRgb(hsv[0], hsv[1], hsv[2]),
                )
            }
        }
    }

    @Test
    fun `HSV round-trips the primary and secondary colours`() {
        listOf(
            0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL,
            0xFFFFFF00L, 0xFF00FFFFL, 0xFFFF00FFL,
            0xFFFFFFFFL, 0xFF000000L, 0xFF808080L,
        ).forEach { original ->
            val hsv = rgbToHsv(original)
            assertEquals(hexOf(original), hexOf(hsvToRgb(hsv[0], hsv[1], hsv[2])))
        }
    }

    @Test
    fun `hue is reported in the expected sector`() {
        assertEquals(0f, rgbToHsv(0xFFFF0000)[0], 0.5f)
        assertEquals(120f, rgbToHsv(0xFF00FF00)[0], 0.5f)
        assertEquals(240f, rgbToHsv(0xFF0000FF)[0], 0.5f)
    }

    @Test
    fun `greys have no saturation`() {
        listOf(0xFF000000L, 0xFF808080L, 0xFFFFFFFFL).forEach {
            assertEquals("$it should be unsaturated", 0f, rgbToHsv(it)[1], 0.001f)
        }
    }

    @Test
    fun `hsvToRgb wraps hue and clamps the rest`() {
        assertEquals(hexOf(hsvToRgb(10f, 0.5f, 0.5f)), hexOf(hsvToRgb(370f, 0.5f, 0.5f)))
        assertEquals(hexOf(hsvToRgb(10f, 0.5f, 0.5f)), hexOf(hsvToRgb(-350f, 0.5f, 0.5f)))
        assertEquals(hexOf(hsvToRgb(0f, 1f, 1f)), hexOf(hsvToRgb(0f, 2f, 5f)))
    }

    @Test
    fun `blending at the extremes returns each side untouched`() {
        assertEquals(0xFFFFFFFFL, blend(0xFFFFFFFF, 0xFF000000, 1f))
        assertEquals(0xFF000000L, blend(0xFFFFFFFF, 0xFF000000, 0f))
    }

    @Test
    fun `blending halfway lands in the middle`() {
        val mid = blend(0xFFFFFFFF, 0xFF000000, 0.5f)
        val channel = (mid shr 16) and 0xFF
        assertTrue("expected mid grey, got ${hexOf(mid)}", abs(channel - 128L) <= 1)
    }

    @Test
    fun `contrast matches the known WCAG extremes`() {
        assertEquals(21.0, contrastRatio(0xFF000000, 0xFFFFFFFF), 0.05)
        assertEquals(1.0, contrastRatio(0xFF808080, 0xFF808080), 0.001)
    }

    @Test
    fun `contrast does not depend on argument order`() {
        assertEquals(
            contrastRatio(0xFF1A2B3C, 0xFFEEDDCC),
            contrastRatio(0xFFEEDDCC, 0xFF1A2B3C),
            0.0001,
        )
    }
}
