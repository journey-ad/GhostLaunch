package re.ovo.ghostlaunch

import android.app.Application
import re.ovo.ghostlaunch.service.AppLauncher

class GhostLaunchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLauncher.init(this)
    }
}
