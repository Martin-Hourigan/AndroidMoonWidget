package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.data.SettingKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the settings screen against silently drifting out of step with the
 * model. Adding a [SettingKey] without a row would compile and persist happily,
 * but the user could never reach it.
 */
class SettingsCoverageTest {

    @Test
    fun `every setting has a row in the settings screen`() {
        val shown = settingsScreenKeys.toSet()
        val missing = SettingKey.entries.filterNot { it in shown }

        assertTrue(
            "these settings exist but have no row in the UI: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `no setting is listed twice`() {
        val shown = settingsScreenKeys
        assertEquals(
            "duplicate rows: ${shown.groupBy { it }.filter { it.value.size > 1 }.keys}",
            shown.size,
            shown.toSet().size,
        )
    }

    @Test
    fun `the screen shows no rows for keys that do not exist`() {
        // Guards against a stale row left behind after a key is renamed.
        assertTrue(settingsScreenKeys.all { it in SettingKey.entries })
    }
}
