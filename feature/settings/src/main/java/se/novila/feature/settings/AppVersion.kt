package se.novila.feature.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/** The installed version: [name] as testers see it (`0.1.42-debug`) and the build number [code] CI set. */
data class AppVersion(val name: String, val code: Long)

/** The version of the installed app, from the package manager. */
internal fun Context.appVersion(): AppVersion {
    val info: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    return AppVersion(name = info.versionName.orEmpty(), code = code)
}
