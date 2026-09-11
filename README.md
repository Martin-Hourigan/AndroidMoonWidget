# Moon widget

A home-screen widget showing the moon as it currently is, with a companion app
for the detail.

Phase, illumination, moonrise and moonset, the countdown to the next full and
new moon, and which constellation the moon is sitting in. All of it computed on
the device.

## No astronomy library, no image assets

The phase and position maths is hand-rolled Kotlin — a few hundred lines of
Julian dates, orbital terms and spherical trigonometry — because the
calculation is compact, exact enough for a widget, and comes with no
dependency to keep current.

The moon itself is **drawn** rather than shipped as a set of images, so it
scales to any widget size and picks up the theme. That also means the
terminator is drawn at the right angle for your latitude: the moon does not
look the same in Newcastle as it does in Edinburgh, and a stock set of phase
images silently assumes the northern hemisphere.

## What it needs, and why

| Permission | What for |
|---|---|
| `ACCESS_COARSE_LOCATION` | Rise and set times, and the tilt of the terminator. Optional — pick a city from a list or type coordinates instead. |
| `POST_NOTIFICATIONS` | Optional reminders before a full or new moon. |
| `READ_CALENDAR` / `WRITE_CALENDAR` | Optional. Writes phases into a calendar of your choosing so they appear alongside everything else. |

Every one of them is optional, and the widget works with none of them granted
beyond a location you have typed in yourself. Nothing is sent anywhere; there
is no network permission at all.

## Building

```
./gradlew installDebug     # onto a connected phone
./gradlew test             # 23 test classes over the astronomy
```

The astronomy is pure Kotlin with no Android imports, so the phase, rise/set
and position maths is all checked on the JVM against known values.

[PLAN.md](PLAN.md) has the design record and the reasoning behind each choice.

## Structure

| | |
|---|---|
| `astronomy/` | Julian dates, phase, distance, rise and set, zodiac. Pure. |
| `render/` | The moon, the compass and the sky dome, all drawn to Canvas. |
| `widget/` | Glance widget and its update worker. |
| `calendar/` | Optional sync of phases into a device calendar. |
| `ui/` | Compose screens for the companion app. |
