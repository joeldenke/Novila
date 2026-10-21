# The watch app is a separate app; it has no workers or action callbacks that R8 could strip yet.
# Keep Metro's generated graph factory and the activity/application named in the manifest are kept by the manifest
# rules; add explicit rules here if the watch ever gets a worker or a second entry point.

# :core:designsystem's NovilaGlanceTheme refers to Glance, which the watch leaves out (wear/build.gradle.kts). The
# watch never calls it, so R8 drops it; don't fail on the missing classes.
-dontwarn androidx.glance.**
-dontwarn androidx.work.**
