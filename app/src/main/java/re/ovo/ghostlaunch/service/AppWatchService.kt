package re.ovo.ghostlaunch.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.data.AppRepository

object WatchState {
    var currentLaunchedPackage: String? = null
}

class AppWatchService : AccessibilityService() {

    private val TAG = "CalcWatch"
    private var launcherPkg: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        launcherPkg = resolveLauncherPackage()
        Log.d(TAG, "launcher package = $launcherPkg")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val watching = WatchState.currentLaunchedPackage ?: return

        if (pkg == watching) return

        if (pkg == launcherPkg) {
            handleExit(watching)
        }
    }

    override fun onInterrupt() {}

    private fun resolveLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(intent, 0)
        return res?.activityInfo?.packageName
    }

    private fun handleExit(packageName: String) {
        WatchState.currentLaunchedPackage = null
        val repo = AppRepository(applicationContext)
        val app = repo.findByPackage(packageName) ?: return

        Log.d(TAG, "returned to home from $packageName, disabling")
        Thread {
            val result = ShizukuManager.disablePackage(packageName)
            Log.d(TAG, "disable $packageName: success=${result.success} msg=${result.message}")
            if (result.success) {
                mainHandler.post {
                    Toast.makeText(applicationContext, applicationContext.getString(R.string.toast_hidden, app.label), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
