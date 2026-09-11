# AndroidMoonWidget — Plan

A home-screen widget showing the current moon phase, with a companion app for detail.

## Scope

### Widget (home screen)
- Moon phase graphic — drawn programmatically, no image assets
- Phase name (e.g. "Waxing Gibbous")
- Moonrise / moonset times (local)
- Countdown to next full & new moon

### App (tap the widget to open)
- Everything above, larger
- Illumination percentage
- Moon sign (zodiac constellation the moon currently occupies) + explanation text
- Possibly: upcoming phase calendar

## Technical approach

| Concern | Choice | Why |
|---|---|---|
| Language | Kotlin | Standard for modern Android |
| Widget framework | Jetpack Glance | Compose-style widgets; far nicer than raw RemoteViews |
| App UI | Jetpack Compose | Shares styling/mental model with Glance |
| Min SDK | 26 (Android 8.0) | Glance needs 21+; 26 gives modern date/time APIs |
| Target SDK | Current (36) | Play Store requirement baseline |
| Astronomy | Hand-rolled Kotlin, no library | Phase math is compact and dependency-free |
| Moon graphic | Canvas drawing | Scales to any widget size, themes cleanly |
| Update scheduling | WorkManager (periodic) | Survives reboots, battery-friendly |
| Location | User-selectable: auto (GPS) / preset / custom | See "Location handling" below |

## Module layout

```
app/
  src/main/java/.../
    astronomy/          <- pure Kotlin, zero Android imports (unit-testable)
      MoonPhase.kt        phase angle, illumination, phase name
      MoonPosition.kt     ecliptic longitude -> zodiac sign
      RiseSet.kt          moonrise/moonset for lat/long
      JulianDate.kt       date conversion helpers
    widget/
      MoonWidget.kt       Glance widget definition
      MoonWidgetReceiver.kt
      MoonCanvas.kt       the drawn moon shape
      UpdateWorker.kt     WorkManager periodic refresh
    ui/
      MainActivity.kt
      DetailScreen.kt     illumination, moon sign, explanation
      LocationScreen.kt   picker: auto / presets / saved / custom
    data/
      LocationRepository.kt   resolves the active location
      LocationSource.kt       sealed type: Auto | Preset | Custom
      PresetLocations.kt      bundled city list
      Prefs.kt                DataStore: selected source, saved places
  src/test/java/.../
    MoonPhaseTest.kt      verify against known dates
```

Keeping `astronomy/` free of Android imports means it runs in plain JVM unit tests — important, because the maths is the part most likely to be subtly wrong.

## Location handling

Rise/set times are location-dependent, so the user picks how location is determined. One setting, three modes:

```kotlin
sealed interface LocationSource {
    object Auto : LocationSource                          // device GPS / coarse location
    data class Preset(val id: String) : LocationSource    // bundled city
    data class Custom(                                    // user-entered, saved
        val label: String,
        val lat: Double,
        val lon: Double,
    ) : LocationSource
}
```

**Auto** — coarse location via `FusedLocationProviderClient`. Coarse (~city block) is plenty; the moon doesn't care about street-level precision, and it avoids asking for fine-location permission. Cached, so a widget refresh doesn't wake the GPS.

**Presets** — bundled list of cities with lat/long baked in, no network lookup. Ships with Australian capitals + Newcastle, plus a set of major world cities. Searchable if the list grows.

**Custom** — user enters coordinates or a place name, saves it with a label. Multiple saved places, one active at a time.

### Resolution order & fallbacks
`LocationRepository` returns the active location, falling back gracefully so the widget *always* has something to show:

1. Whatever `LocationSource` the user selected
2. If Auto but permission denied or no fix yet → last known good location
3. If none of the above → default preset (Newcastle), flagged in the UI so it's clearly a default rather than real

The widget must never show an error state just because a location fix is slow — stale-but-labelled beats blank.

### Permissions
Location permission is requested **only** when the user actively chooses Auto — not at first launch. Preset and Custom modes need no permission at all, so the app is fully usable without ever granting one. This also keeps the widget working if permission is later revoked.

## The astronomy, briefly

- **Phase**: from the moon's age since a known new moon (synodic month ≈ 29.53 days). Refined via solar/lunar ecliptic longitude difference for accuracy.
- **Illumination**: `(1 - cos(phase angle)) / 2`.
- **Moon sign**: moon's ecliptic longitude, divided into 12 × 30° segments starting at Aries 0°.
- **Rise/set**: iterative — compute altitude across the day, find zero crossings. The fiddliest part; needs latitude/longitude.

Accuracy target: within a minute or two for rise/set, well under a degree for position. Meeus' *Astronomical Algorithms* low-precision formulae are more than enough.

## Build order

1. ~~**Astronomy core + unit tests**~~ — **DONE.** Validated against Meeus worked examples
   (47.a moon position exact to printed precision, 25.b sun to 1e-5°), known 2024 lunations,
   and the full-moon-rises-at-sunset constraint (5 min agreement at Newcastle).
2. ~~**Moon shape geometry**~~ — **DONE** (`MoonAppearance.kt`). Terminator is a true half-ellipse,
   verified by area integration: drawn lit fraction matches analytic illumination to <0.2%
   at every phase. Southern-hemisphere flip included, driven by observer latitude.
3. ~~**Glance widget**~~ — **DONE.** Compact + wide layouts, `SizeMode.Exact`, tap opens the app.
4. ~~**Real data + WorkManager refresh**~~ — **DONE.** 30-min periodic refresh, scheduled when the
   first widget is placed, cancelled with the last, re-armed after reboot/app-update.
5. ~~**Location: presets**~~ — **DONE.** 25 cities with coordinates + timezones, searchable picker.
6. ~~**Rise/set times**~~ — **DONE**, shown on both widget and detail screen.
7. ~~**Auto location**~~ — **DONE.** Coarse permission requested only on opt-in, fused provider,
   cached coordinates, graceful fallback chain.
8. ~~**Custom saved locations**~~ — **DONE.** Enter/label/persist/delete via DataStore.
9. ~~**Companion app screen**~~ — **DONE.** Illumination, age, countdowns, moon sign + explanation.
10. **Polish** — visual pass, theming, widget resize behaviour *(next)*

Presets deliberately came before Auto: it made rise/set testable against known coordinates, and
meant permission handling couldn't block progress on the maths.

## Settings

16 toggles, grouped into Widget / In the app / Presentation / Notifications, all persisted to
DataStore and all actually wired — no decorative checkboxes.

The screen is generated from a declared list of rows keyed by `SettingKey`, and `Settings.valueOf`
/ `Settings.with` are exhaustive `when`s over that enum, so adding a setting without wiring it is a
compile error rather than a silent no-op.

New capability that came with it: **moon distance**, which the position series already produced and
we were discarding. That gives apparent size, perigee/apogee proximity, and supermoon detection
(full moon within 90% of perigee — the Nolle criterion).

**Notifications**: full moon, new moon, moonrise. Each is a self-rescheduling one-shot rather than a
poll, so nothing wakes up unless there is something to say. `POST_NOTIFICATIONS` is requested only
when a toggle is switched on, never at launch.

## Status

`./gradlew assembleDebug` produces a working APK. `./gradlew testDebugUnitTest` — **57 tests, all
passing.** `./gradlew lintDebug` — **0 errors**, 11 cosmetic warnings.

Not yet verified: anything visual. Nothing has been run on a device or emulator, so layout,
spacing, colours and the widget's real-world sizing are all unreviewed.

## Open questions

- Widget sizes to support — small square only, or a wide 4×2 variant too?
- Moon sign explanation text: write it ourselves, or source it?
- Southern-hemisphere orientation: the moon appears flipped in Newcastle vs. northern-hemisphere references. Worth handling, easy to forget.
