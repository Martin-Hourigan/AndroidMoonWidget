package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MoonAppearanceTest {

    @Test
    fun `new and full are flagged for the simple drawing paths`() {
        assertTrue(MoonAppearance.forPhase(0.0).isEffectivelyNew)
        assertTrue(MoonAppearance.forPhase(180.0).isEffectivelyFull)

        assertFalse(MoonAppearance.forPhase(90.0).isEffectivelyNew)
        assertFalse(MoonAppearance.forPhase(90.0).isEffectivelyFull)
    }

    @Test
    fun `terminator is flat at the quarters`() {
        // A quarter moon's terminator is a straight line, i.e. an ellipse of zero width.
        assertEquals(0.0, MoonAppearance.forPhase(90.0).terminatorCurveFactor, 1e-9)
        assertEquals(0.0, MoonAppearance.forPhase(270.0).terminatorCurveFactor, 1e-9)
    }

    @Test
    fun `terminator curves inward when crescent and outward when gibbous`() {
        assertTrue("crescent should curve inward", MoonAppearance.forPhase(45.0).terminatorCurveFactor < 0)
        assertTrue("gibbous should bulge outward", MoonAppearance.forPhase(135.0).terminatorCurveFactor > 0)
    }

    @Test
    fun `crescent flag agrees with illumination`() {
        assertTrue(MoonAppearance.forPhase(45.0).isCrescent)
        assertFalse(MoonAppearance.forPhase(135.0).isCrescent)
    }

    @Test
    fun `northern waxing moons are lit on the right`() {
        assertTrue(MoonAppearance.forPhase(45.0, observerLatitude = 51.5).litOnRight)
        assertFalse(MoonAppearance.forPhase(315.0, observerLatitude = 51.5).litOnRight)
    }

    @Test
    fun `southern hemisphere mirrors the lit limb`() {
        val newcastle = -32.93
        // The same phase that is lit-on-the-right in London is lit-on-the-left here.
        assertFalse(MoonAppearance.forPhase(45.0, observerLatitude = newcastle).litOnRight)
        assertTrue(MoonAppearance.forPhase(315.0, observerLatitude = newcastle).litOnRight)
    }

    /**
     * The decisive geometry check.
     *
     * Integrate the drawn lit area over the disc and compare it with the
     * analytic illumination. This only passes if the terminator really is a
     * half-ellipse — approximating it with a circular arc fails badly at the
     * crescent and gibbous phases.
     */
    @Test
    fun `drawn lit area matches analytic illumination at every phase`() {
        val samples = 601

        for (phase in listOf(0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 270, 315, 330)) {
            val appearance = MoonAppearance.forPhase(phase.toDouble())

            var litPixels = 0
            var totalPixels = 0

            for (i in 0 until samples) {
                val x = (i - (samples - 1) / 2.0) / ((samples - 1) / 2.0)
                for (j in 0 until samples) {
                    val y = (j - (samples - 1) / 2.0) / ((samples - 1) / 2.0)
                    if (x * x + y * y > 1.0) continue

                    totalPixels++
                    if (isLit(x, y, appearance)) litPixels++
                }
            }

            val drawn = litPixels.toDouble() / totalPixels
            assertEquals(
                "phase $phase: drawn ${"%.3f".format(drawn)} vs " +
                    "analytic ${"%.3f".format(appearance.illumination)}",
                appearance.illumination,
                drawn,
                0.01,
            )
        }
    }

    /**
     * Mirrors the fill rule the renderers use: inside the disc, and on the lit
     * side of the terminator ellipse.
     */
    private fun isLit(x: Double, y: Double, appearance: MoonAppearance): Boolean {
        if (x * x + y * y > 1.0) return false

        val terminatorX = appearance.terminatorCurveFactor * sqrt(1 - y * y)
        // Flip the horizontal axis when the lit limb is on the left.
        val effectiveX = if (appearance.litOnRight) x else -x
        return effectiveX >= -terminatorX
    }

    @Test
    fun `terminator openness peaks at the quarters`() {
        assertEquals(1.0, MoonAppearance.forPhase(90.0).terminatorOpenness, 1e-9)
        assertEquals(0.0, MoonAppearance.forPhase(0.0).terminatorOpenness, 1e-9)
        assertTrue(abs(MoonAppearance.forPhase(180.0).terminatorOpenness) < 1e-9)
    }
}
