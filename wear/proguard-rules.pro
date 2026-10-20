# The watch app is a separate app; it has no workers or action callbacks that R8 could strip yet.
# Keep Metro's generated graph factory and the activity/application named in the manifest are kept by the manifest
# rules; add explicit rules here if the watch ever gets a worker or a second entry point.
