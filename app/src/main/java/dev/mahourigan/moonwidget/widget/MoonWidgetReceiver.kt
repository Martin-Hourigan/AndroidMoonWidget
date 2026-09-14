package dev.mahourigan.moonwidget.widget

import dev.mahourigan.moonwidget.notification.MoonEventWorker
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that hosts [MoonWidget].
 *
 * Also kicks off the periodic refresh once the first widget is placed, and
 * cancels it when the last one is removed — no point waking up for a widget
 * that isn't on screen.
 */
class MoonWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MoonWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MoonUpdateWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MoonUpdateWorker.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            // Re-arm after a reboot or an app update, since neither preserves scheduling.
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                MoonUpdateWorker.schedule(context)
                // Scheduled notifications do not survive a reboot either.
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        MoonEventWorker.sync(context)
                    } finally {
                        pending.finish()
                    }
                }
            }

            // Travelling changes both the local day boundary and the rise/set
            // times on display, so redraw now and move the midnight job.
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> {
                MidnightUpdateWorker.schedule(context)
                goAsync(context)
            }
        }
    }

    /** Redraw off the main thread; the receiver may be torn down immediately. */
    private fun goAsync(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                refreshWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
