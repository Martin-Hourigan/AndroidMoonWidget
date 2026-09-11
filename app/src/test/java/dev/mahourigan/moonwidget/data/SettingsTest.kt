package dev.mahourigan.moonwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    /**
     * `valueOf` and `with` are two hand-written exhaustive `when`s over the same
     * enum, so it is entirely possible to wire a key to the wrong field. This
     * round-trips every key to prove each one reads and writes its own value.
     */
    @Test
    fun `every key round trips independently`() {
        SettingKey.entries.forEach { key ->
            val flipped = Settings().with(key, !Settings().valueOf(key))
            assertEquals(
                "$key did not round trip",
                !Settings().valueOf(key),
                flipped.valueOf(key),
            )
        }
    }

    /** Setting one key must not disturb any other — the classic copy/paste bug. */
    @Test
    fun `setting one key leaves the others untouched`() {
        SettingKey.entries.forEach { changed ->
            val defaults = Settings()
            val updated = defaults.with(changed, !defaults.valueOf(changed))

            SettingKey.entries.filter { it != changed }.forEach { other ->
                assertEquals(
                    "changing $changed also changed $other",
                    defaults.valueOf(other),
                    updated.valueOf(other),
                )
            }
        }
    }

    @Test
    fun `defaults keep the widget useful out of the box`() {
        val defaults = Settings()
        assertTrue(defaults.widgetShowPhaseName)
        assertTrue(defaults.widgetShowIllumination)
        assertTrue(defaults.widgetShowRiseSet)
        assertTrue(defaults.widgetShowFullMoonCountdown)
    }

    @Test
    fun `notifications are off by default`() {
        val defaults = Settings()
        assertFalse(defaults.notifyFullMoon)
        assertFalse(defaults.notifyNewMoon)
        assertFalse(defaults.notifyMoonrise)
        assertFalse(defaults.anyNotificationEnabled)
    }

    @Test
    fun `anyNotificationEnabled tracks the individual toggles`() {
        assertTrue(Settings(notifyFullMoon = true).anyNotificationEnabled)
        assertTrue(Settings(notifyNewMoon = true).anyNotificationEnabled)
        assertTrue(Settings(notifyMoonrise = true).anyNotificationEnabled)
    }

    @Test
    fun `only notification keys are flagged as needing permission`() {
        val needing = SettingKey.entries.filter { it.needsNotificationPermission }.toSet()
        assertEquals(
            setOf(
                SettingKey.NOTIFY_FULL_MOON,
                SettingKey.NOTIFY_NEW_MOON,
                SettingKey.NOTIFY_MOONRISE,
            ),
            needing,
        )
    }
}
