# Project-specific R8 rules. Compose and AndroidX ship their own consumer rules.
# Keep line numbers so crash stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager saves each piece of work with its worker's class name and looks the class up by that name again, also
# after an update. ReminderWorker is created by ReminderWorkerFactory (it takes the app's stores), so WorkManager's own
# rule, which keeps workers with a (Context, WorkerParameters) constructor, doesn't keep its name.
-keepnames class se.novila.reminders.ReminderWorker

# Glance saves the widget button's action by its class name and creates it again from that name when it is tapped.
-keep class se.novila.feature.widget.LogWaterAction { <init>(); }
