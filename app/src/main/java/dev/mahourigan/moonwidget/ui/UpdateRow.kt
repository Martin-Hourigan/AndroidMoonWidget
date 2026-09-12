package dev.mahourigan.moonwidget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mahourigan.moonwidget.BuildConfig
import dev.mahourigan.moonwidget.update.InstallStart
import dev.mahourigan.moonwidget.update.ReleaseVersion
import dev.mahourigan.moonwidget.update.UpdateInstaller
import dev.mahourigan.moonwidget.update.Updates
import kotlinx.coroutines.launch

/** What the row is doing, which is the whole of its state. */
private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object Failed : UpdateState
    data class Available(val release: ReleaseVersion) : UpdateState
    data class Downloading(val release: ReleaseVersion) : UpdateState

    /**
     * Android will not let this app install anything yet.
     *
     * A separate state rather than a failure, because it is not one: nothing
     * has gone wrong, there is simply a switch the person has to flick, and
     * the row can take them straight to it.
     */
    data class NeedsPermission(val release: ReleaseVersion) : UpdateState
}

/**
 * Checking for a newer build, and installing it.
 *
 * One row that changes what it says rather than a screen of its own: checking
 * for an update is a thing you do once in a while and then forget, and it does
 * not deserve navigation.
 *
 * The last step is Android's, not this app's. Once the APK is handed over, the
 * system shows its own confirmation and makes its own decision — including
 * refusing outright if the download is not signed by the same key as the
 * installed app. That refusal is the reason an in-app updater is safe to have
 * at all, so it is worth knowing it is there rather than assuming this code
 * is doing the checking.
 */
@Composable
fun UpdateRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    val installed = BuildConfig.VERSION_NAME
    val current = state

    val subtitle = when (current) {
        UpdateState.Idle -> "Version $installed"
        UpdateState.Checking -> "Checking…"
        UpdateState.UpToDate -> "Version $installed — up to date"
        UpdateState.Failed -> "Could not reach GitHub. Try again later."
        is UpdateState.Available -> "Version ${current.release.name} is available"
        is UpdateState.Downloading -> "Downloading ${current.release.name}…"
        is UpdateState.NeedsPermission ->
            "Android needs your permission to install apps from here. Tap to allow it, " +
                "then tap again to install ${current.release.name}."
    }

    val action: () -> Unit = when (current) {
        // Nothing to do while work is in flight; a second tap would start a
        // second download of the same file.
        UpdateState.Checking, is UpdateState.Downloading -> ({})

        // Sending them to the system screen that grants it. Coming back,
        // a second tap installs.
        is UpdateState.NeedsPermission -> ({
            runCatching { context.startActivity(UpdateInstaller.permissionSettings(context)) }
            state = UpdateState.Available(current.release)
        })

        is UpdateState.Available -> ({
            val release = current.release
            if (!UpdateInstaller.canInstall(context)) {
                // Checked before downloading rather than after: three megabytes
                // fetched and then silently discarded is a poor way to find out
                // about a permission.
                state = UpdateState.NeedsPermission(release)
            } else {
                state = UpdateState.Downloading(release)
                scope.launch {
                    val apk = Updates.download(context, release)
                    state = when {
                        apk == null -> UpdateState.Failed
                        else -> when (UpdateInstaller.install(context, apk)) {
                            // The system prompt takes over. Back to Idle so that
                            // declining it leaves the row usable rather than
                            // stuck saying "Downloading" for ever.
                            InstallStart.STARTED -> UpdateState.Idle
                            InstallStart.NEEDS_PERMISSION -> UpdateState.NeedsPermission(release)
                            InstallStart.FAILED -> UpdateState.Failed
                        }
                    }
                }
            }
        })

        else -> ({
            state = UpdateState.Checking
            scope.launch {
                val latest = Updates.latest()
                state = when {
                    latest == null -> UpdateState.Failed
                    latest.isNewerThan(BuildConfig.VERSION_CODE) -> UpdateState.Available(latest)
                    else -> UpdateState.UpToDate
                }
            }
        })
    }

    val title = when (current) {
        is UpdateState.Available -> "Download and install"
        is UpdateState.NeedsPermission -> "Allow installing updates"
        else -> "Check for updates"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = action)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = MoonColors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(text = subtitle, color = MoonColors.muted, fontSize = 13.sp)

        if (current is UpdateState.Available && current.release.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = current.release.notes.lines().take(4).joinToString("\n"),
                color = MoonColors.muted,
                fontSize = 12.sp,
            )
        }
    }
}
