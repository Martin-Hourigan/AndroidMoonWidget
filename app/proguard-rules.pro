# R8 rules for the release build.
#
# Most of what this app uses ships its own consumer rules — Compose, Glance,
# DataStore, WorkManager and play-services-location all do. What follows is only
# the handful of things R8 cannot work out on its own.

# --- Enum names are persisted, so they must survive renaming ---
#
# Settings are stored by enum constant name: SettingKey becomes the preference
# key "setting_<name>", and MoonTheme, MoonNameSet, ThemeRole and NotifiableEvent
# are all written out with .name and read back with a name comparison. If R8
# renamed those constants the keys would change and every saved setting would
# silently revert to its default on the first release build — a bug that would
# never show up in a debug build or in a unit test.
-keepclassmembers enum dev.mahourigan.moonwidget.** {
    *;
}

# --- WorkManager instantiates workers reflectively, by class name ---
-keep class dev.mahourigan.moonwidget.notification.MoonEventWorker { <init>(...); }
-keep class dev.mahourigan.moonwidget.widget.MoonUpdateWorker { <init>(...); }
-keep class dev.mahourigan.moonwidget.widget.MidnightUpdateWorker { <init>(...); }

# --- Glance ---
#
# The receiver is named in the manifest so it is kept anyway, but the widget it
# holds is only reached through it, and updateAll() looks the widget up by class.
-keep class dev.mahourigan.moonwidget.widget.MoonWidget { *; }
-keep class dev.mahourigan.moonwidget.widget.MoonWidgetReceiver { *; }

# Glance builds an ActionCallback by reflection -- it puts the class name in the
# PendingIntent and calls getDeclaredConstructor().newInstance() when the tap
# arrives. Glance ships "-keep public class * extends ActionCallback", but that
# keeps only the class *name*: R8 is still free to drop members nothing calls,
# and nothing in this app calls the constructor. It was duly removed, so tapping
# the widget panel threw NoSuchMethodException inside Glance, which swallows it
# -- the tap highlighted and then nothing happened.
#
# Debug builds are not minified, so this existed only in release, and only from
# the first release build onward. Worth remembering as a class of bug: anything
# constructed by name rather than by code needs its constructor kept, the same
# way the workers above do.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}

# Keep line numbers so a crash report from a release build can be read.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
