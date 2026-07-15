package re.ovo.ghostlaunch.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import re.ovo.ghostlaunch.data.AppRepository
import re.ovo.ghostlaunch.data.HiddenApp

object AppLauncher {

    private const val TAG = "CalcLauncher"

    private var ctx: Context? = null
    private var pm: PackageManager? = null

    fun init(context: Context) {
        ctx = context.applicationContext
        pm = context.packageManager
    }

    fun context(): Context? = ctx

    fun launchHiddenApp(app: HiddenApp, repository: AppRepository): Boolean {
        val context = ctx ?: return false
        val packageManager = pm ?: return false

        if (!ShizukuManager.isPermissionGranted()) {
            Log.w(TAG, "Shizuku not granted")
            return false
        }

        val enabled = ShizukuManager.enablePackage(app.packageName)
        if (!enabled.success) {
            Log.e(TAG, "enable failed: ${enabled.message}")
            return false
        }

        repository.updateLastLaunched(app.packageName, System.currentTimeMillis())

        val intent = packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
            if (app.className != null) {
                component = ComponentName(app.packageName, app.className)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: run {
            Log.e(TAG, "no launch intent for ${app.packageName}")
            return false
        }

        WatchState.currentLaunchedPackage = app.packageName

        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
