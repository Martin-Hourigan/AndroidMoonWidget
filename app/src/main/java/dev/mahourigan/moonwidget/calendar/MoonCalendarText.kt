package dev.mahourigan.moonwidget.calendar

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.data.namedMoonKey
import dev.mahourigan.moonwidget.data.valueOf
import dev.mahourigan.moonwidget.render.TimeFormatting
import dev.mahourigan.moonwidget.ui.moonName
import android.content.Context
import java.time.ZoneId

/**
 * Turns a [MoonOccurrence] into the words that go in the calendar.
 *
 * The boundary between the Android-free planning layer and string resources,
 * mirroring `ui/DisplayStrings.kt`. Titles are what shows in a month grid, so
 * they stay short; everything else goes in the description, which is only ever
 * seen by someone who opens the event.
 */
object MoonCalendarText {

    fun describe(
        context: Context,
        occurrence: MoonOccurrence,
        settings: Settings,
        zone: ZoneId,
    ): DesiredEvent = DesiredEvent(
        date = occurrence.date,
        title = title(context, occurrence, settings),
        description = description(context, occurrence, settings, zone),
    )

    /** Convenience: the whole wanted set, in one call. */
    fun describeAll(
        context: Context,
        occurrences: List<MoonOccurrence>,
        settings: Settings,
        zone: ZoneId,
    ): List<DesiredEvent> = occurrences.map { describe(context, it, settings, zone) }

    private fun title(
        context: Context,
        occurrence: MoonOccurrence,
        settings: Settings,
    ): String {
        val base = when (occurrence.kind) {
            MoonEventKind.NEW -> context.getString(R.string.calendar_title_new_moon)
            MoonEventKind.FULL -> {
                // Same rules as the main screen: the master switch, and then
                // the individual slot's own toggle.
                val slot = occurrence.slot
                    ?.takeIf { settings.showNamedMoons && settings.valueOf(namedMoonKey(it)) }

                if (slot == null) {
                    context.getString(R.string.calendar_title_full_moon)
                } else {
                    context.getString(
                        R.string.calendar_title_full_moon_named,
                        moonName(context, settings.moonNameSet, slot),
                    )
                }
            }
        }

        // Worth putting in the title rather than the body: the title is all
        // that shows in a month grid, and a supermoon is the one night someone
        // might actually change their plans for.
        return if (occurrence.isSupermoon && settings.highlightSupermoon) {
            context.getString(R.string.calendar_title_supermoon, base)
        } else {
            base
        }
    }

    private fun description(
        context: Context,
        occurrence: MoonOccurrence,
        settings: Settings,
        zone: ZoneId,
    ): String {
        val time = TimeFormatting.format(
            occurrence.peak, zone, context, settings.force24HourTime,
        )

        val lines = mutableListOf(
            when (occurrence.kind) {
                MoonEventKind.FULL -> context.getString(R.string.calendar_body_full_peak, time)
                MoonEventKind.NEW -> context.getString(R.string.calendar_body_new_peak, time)
            }
        )

        if (occurrence.isSupermoon && settings.highlightSupermoon) {
            lines += context.getString(R.string.calendar_body_supermoon)
        }

        // Always last, always exactly this text — it is what finds these events
        // again later. See MOON_CALENDAR_MARKER.
        lines += MOON_CALENDAR_MARKER

        return lines.joinToString("\n")
    }
}
