package dev.mahourigan.moonwidget.notification

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.ui.MainActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Posting moon-event notifications.
 *
 * All posting goes through [notify], which checks the runtime permission first —
 * on Android 13+ posting without it silently fails, and it can be revoked at any
 * time after being granted.
 */
object MoonNotifications {

    const val CHANNEL_ID = "moon_events"

    private const val ID_FULL_MOON = 1000
    private const val ID_NEW_MOON = 2000
    private const val ID_MOONRISE = 3000

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        // POST_NOTIFICATIONS only exists from API 33; below that it is implicit.
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    fun fullMoon(
        context: Context,
        illuminationPercent: Int,
        isSupermoon: Boolean,
        leadDays: Int = 0,
    ) = notify(
        context = context,
        // A reminder a week out must not say "tonight". The lead decides the
        // wording, not the event.
        id = ID_FULL_MOON + leadDays,
        title = when {
            isSupermoon && leadDays == 0 -> context.getString(R.string.notification_supermoon_title)
            isSupermoon -> context.resources.getQuantityString(
                R.plurals.notification_supermoon_ahead, leadDays, leadDays,
            )
            leadDays == 0 -> context.getString(R.string.notification_full_moon_title)
            leadDays == 1 -> context.getString(R.string.notification_full_moon_tomorrow)
            else -> context.resources.getQuantityString(
                R.plurals.notification_full_moon_ahead, leadDays, leadDays,
            )
        },
        text = context.getString(R.string.notification_full_moon_text, illuminationPercent),
    )

    fun newMoon(context: Context, leadDays: Int = 0) = notify(
        context = context,
        id = ID_NEW_MOON + leadDays,
        title = when {
            leadDays == 0 -> context.getString(R.string.notification_new_moon_title)
            leadDays == 1 -> context.getString(R.string.notification_new_moon_tomorrow)
            else -> context.resources.getQuantityString(
                R.plurals.notification_new_moon_ahead, leadDays, leadDays,
            )
        },
        text = context.getString(R.string.notification_new_moon_text),
    )

    fun moonrise(context: Context, phaseName: String) = notify(
        context = context,
        id = ID_MOONRISE,
        title = context.getString(R.string.notification_moonrise_title),
        text = context.getString(R.string.notification_moonrise_text, phaseName),
    )

    // The permission is checked on the first line, but lint cannot follow the
    // check through hasPermission(); the runCatching below is the real backstop
    // for a revocation racing this call.
    @SuppressLint("MissingPermission")
    private fun notify(context: Context, id: Int, title: String, text: String) {
        if (!hasPermission(context)) return

        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_moon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Guarded above, but the platform still wants the try/catch.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
