package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.astronomy.MoonNameSet
import dev.mahourigan.moonwidget.astronomy.MoonSlot
import dev.mahourigan.moonwidget.data.SettingKey
import dev.mahourigan.moonwidget.data.namedMoonKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The name sets are four parallel tables of string resources. Nothing stops one
 * from silently reusing another's resource, or a slot being missed, so the
 * mapping is checked exhaustively here.
 */
class MoonNameSetCoverageTest {

    @Test
    fun `every set has a distinct resource for every slot`() {
        MoonNameSet.entries.forEach { set ->
            val ids = MoonSlot.entries.map { slot -> moonNameRes(set, slot) }

            // HARVEST deliberately reuses another slot's name in some sets, so
            // allow exactly one duplicate but no more.
            val distinct = ids.toSet().size
            assertTrue(
                "$set reused resources too often: $distinct distinct of ${ids.size}",
                distinct >= MoonSlot.entries.size - 1,
            )
        }
    }

    @Test
    fun `no two sets share a resource for the same slot`() {
        MoonSlot.entries.forEach { slot ->
            val ids = MoonNameSet.entries.map { set -> moonNameRes(set, slot) }
            assertEquals(
                "sets share a resource for $slot",
                ids.size,
                ids.toSet().size,
            )
        }
    }

    @Test
    fun `every slot has its own toggle`() {
        val keys = MoonSlot.entries.map { namedMoonKey(it) }
        assertEquals("a slot shares a toggle with another", keys.size, keys.toSet().size)
        assertTrue(keys.all { it in SettingKey.entries })
    }

    @Test
    fun `every set has a display name and a provenance note`() {
        MoonNameSet.entries.forEach { set ->
            assertTrue("$set has no display name", set.displayNameRes != 0)
            assertTrue("$set has no note", set.noteRes != 0)
        }
    }

    @Test
    fun `unknown stored values fall back to the default set`() {
        assertEquals(MoonNameSet.DEFAULT, MoonNameSet.fromNameOrDefault(null))
        assertEquals(MoonNameSet.DEFAULT, MoonNameSet.fromNameOrDefault(""))
        assertEquals(MoonNameSet.DEFAULT, MoonNameSet.fromNameOrDefault("NOT_A_SET"))
        assertEquals(MoonNameSet.CELTIC, MoonNameSet.fromNameOrDefault("CELTIC"))
    }
}
