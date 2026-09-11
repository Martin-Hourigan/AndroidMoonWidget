package dev.mahourigan.moonwidget.render

import dev.mahourigan.moonwidget.R
import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Time display that follows the device's 12/24-hour setting.
 *
 * Hardcoding `HH:mm` looks wrong to anyone who uses a 12-hour clock, and the
 * system setting is the only reliable signal — locale alone is not enough,
 * since users override it.
 */
object TimeFormatting {

    /**
     * @param force24Hour overrides the system setting, for users who want a
     *   24-hour clock in this app regardless of how the device is configured.
     */
    fun formatter(context: Context, force24Hour: Boolean = false): DateTimeFormatter {
        val use24 = force24Hour || DateFormat.is24HourFormat(context)
        return DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    /** Format an instant in [zone], or return [placeholder] when it is null. */
    fun format(
        instant: Instant?,
        zone: ZoneId,
        context: Context,
        force24Hour: Boolean = false,
        placeholder: String = context.getString(R.string.time_placeholder),
    ): String = instant
        ?.atZone(zone)
        ?.format(formatter(context, force24Hour))
        ?: placeholder

    /**
     * A short weekday and date, e.g. "Sat 29 Aug".
     *
     * The field order is taken from the locale rather than hardcoded, so this
     * reads "Sat 29 Aug" in en-AU and "Sat, Aug 29" in en-US.
     */
    fun dayAndDate(instant: Instant, zone: ZoneId, context: Context): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
        return instant.atZone(zone).format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    /**
     * Adds a day marker when the time is not on the same local date as [now],
     * so "rises at 1:20 am" is not mistaken for something that already happened.
     */
    fun formatWithDayHint(
        instant: Instant?,
        zone: ZoneId,
        context: Context,
        now: Instant = Instant.now(),
        force24Hour: Boolean = false,
        placeholder: String = context.getString(R.string.time_placeholder),
    ): String {
        if (instant == null) return placeholder

        val target = instant.atZone(zone)
        val today = now.atZone(zone).toLocalDate()
        val time = target.format(formatter(context, force24Hour))

        return when (target.toLocalDate()) {
            today -> time
            today.plusDays(1) -> context.getString(R.string.time_tomorrow, time)
            else -> context.getString(
                R.string.time_on_day,
                time,
                dayAndDate(instant, zone, context),
            )
        }
    }
}
