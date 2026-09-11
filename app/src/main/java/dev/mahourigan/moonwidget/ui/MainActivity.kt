package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.data.LocationSource
import dev.mahourigan.moonwidget.data.SettingKey
import dev.mahourigan.moonwidget.data.relativeLuminance
import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MoonApp()
                }
            }
        }
    }
}

private enum class Screen { DETAIL, LOCATION, SETTINGS, NAME_SET, THEME, TYPOGRAPHY }

@Composable
private fun MoonApp(viewModel: MoonViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var screen by remember { mutableStateOf(Screen.DETAIL) }
    val palette = state.settings.palette.toPalette()

    // The system draws the clock and battery over our background, and picks
    // neither colour itself. Without this a light theme gets white-on-cream.
    val view = LocalView.current
    val lightBackground = relativeLuminance(state.settings.palette.background) > 0.5
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBackground
                isAppearanceLightNavigationBars = lightBackground
            }
        }
    }

    // The location picker is reachable from two places, so going back has to
    // return to whichever one opened it.
    var locationOpenedFrom by remember { mutableStateOf(Screen.DETAIL) }

    // Lives out here so stepping into a picker and back does not fold the
    // settings screen up again.
    val settingsExpansion = rememberSettingsExpansion()

    // Rearranging is a mode over the main screen rather than a screen of its
    // own, so the cards can be moved where they actually live.
    var rearranging by remember { mutableStateOf(false) }

    val up: () -> Unit = {
        screen = when (screen) {
            Screen.NAME_SET, Screen.THEME, Screen.TYPOGRAPHY -> Screen.SETTINGS
            Screen.LOCATION -> locationOpenedFrom
            else -> Screen.DETAIL
        }
    }

    var locationGranted by remember { mutableStateOf(viewModel.hasLocationPermission()) }
    var notificationsGranted by remember { mutableStateOf(viewModel.hasNotificationPermission()) }
    var calendarGranted by remember { mutableStateOf(viewModel.hasCalendarPermission()) }

    // Which toggle asked for permission, so it can be switched on if granted.
    var pendingNotificationSetting by remember { mutableStateOf<SettingKey?>(null) }
    var pendingCalendarSetting by remember { mutableStateOf<SettingKey?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
        if (granted) viewModel.selectLocation(LocationSource.Auto)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        pendingNotificationSetting?.let { viewModel.onNotificationPermissionResult(granted, it) }
        pendingNotificationSetting = null
    }

    // Read and write are asked for together: the app has to find its own
    // entries again in order to update or remove them, so write alone would
    // leave it able to create events it could never clean up.
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        calendarGranted = granted
        pendingCalendarSetting?.let { viewModel.onCalendarPermissionResult(granted, it) }
        pendingCalendarSetting = null
    }

    // Without this, system back closes the app from a sub-screen instead of
    // stepping back the way the on-screen arrow does.
    BackHandler(enabled = screen != Screen.DETAIL, onBack = up)

    // Font family and size are applied once, here, rather than on every Text.
    //
    // LocalTextStyle is what Text merges its own parameters into, so setting a
    // family there reaches every label in the app. Size works differently:
    // overriding fontScale on the density scales every sp value while leaving
    // dp alone, so the type grows without the padding and card sizes moving
    // with it.
    val density = LocalDensity.current
    val scaled = remember(density, state.settings.textSize) {
        Density(
            density = density.density,
            fontScale = density.fontScale * state.settings.textSize.scale,
        )
    }

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalDensity provides scaled,
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontFamily = state.settings.appFont.toFontFamily(),
        ),
    ) {
        when (screen) {
            Screen.DETAIL -> DetailScreen(
                state = state,
                rearranging = rearranging,
                onReorder = viewModel::setSectionOrder,
                onDoneRearranging = { rearranging = false },
                onChangeLocation = {
                    locationOpenedFrom = Screen.DETAIL
                    screen = Screen.LOCATION
                },
                onOpenSettings = { screen = Screen.SETTINGS },
                onRetry = viewModel::refresh,
            )

            Screen.LOCATION -> LocationScreen(
                state = state,
                hasPermission = locationGranted,
                onSelect = viewModel::selectLocation,
                onSaveCustom = viewModel::saveCustomPlace,
                onDelete = viewModel::deleteCustomPlace,
                onRequestPermission = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onBack = up,
            )

            Screen.SETTINGS -> SettingsScreen(
                settings = state.settings,
                hasNotificationPermission = notificationsGranted,
                hasCalendarPermission = calendarGranted,
                onToggle = viewModel::setSetting,
                onRequestCalendarPermission = { key ->
                    pendingCalendarSetting = key
                    calendarPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR,
                        )
                    )
                },
                onRequestNotificationPermission = { key ->
                    pendingNotificationSetting = key
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // No runtime prompt below API 33 — notifications are allowed
                        // unless switched off in system settings.
                        notificationsGranted = true
                        viewModel.onNotificationPermissionResult(granted = true, key = key)
                        pendingNotificationSetting = null
                    }
                },
                onOpenLocation = {
                    locationOpenedFrom = Screen.SETTINGS
                    screen = Screen.LOCATION
                },
                onOpenNameSet = { screen = Screen.NAME_SET },
                onOpenTheme = { screen = Screen.THEME },
                onOpenTypography = { screen = Screen.TYPOGRAPHY },
                onRearrange = {
                    rearranging = true
                    screen = Screen.DETAIL
                },
                onCustomLeadDays = viewModel::setCustomLeadDays,
                onNotificationTime = viewModel::setNotificationMinuteOfDay,
                onReset = viewModel::resetSettings,
                onBack = up,
                expansion = settingsExpansion,
            )

            Screen.NAME_SET -> MoonNameSetScreen(
                selected = state.settings.moonNameSet,
                onSelect = viewModel::selectMoonNameSet,
                onBack = up,
            )

            Screen.TYPOGRAPHY -> TypographyScreen(
                font = state.settings.appFont,
                size = state.settings.textSize,
                onSelectFont = viewModel::selectFont,
                onSelectSize = viewModel::selectTextSize,
                onBack = up,
            )

            Screen.THEME -> ThemeScreen(
                settings = state.settings,
                onSelectTheme = viewModel::selectTheme,
                onCustomColor = viewModel::setCustomColor,
                onResetCustom = viewModel::resetCustomTheme,
                onBack = up,
            )
        }
    }
}

