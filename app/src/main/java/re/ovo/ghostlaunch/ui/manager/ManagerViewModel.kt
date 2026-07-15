package re.ovo.ghostlaunch.ui.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.data.AppRepository
import re.ovo.ghostlaunch.data.HiddenApp
import re.ovo.ghostlaunch.service.AppLauncher
import re.ovo.ghostlaunch.service.ShizukuManager

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val enabled: Boolean
)

class ManagerViewModel(
    private val context: Context,
    private val repository: AppRepository = AppRepository(context)
) : ViewModel() {

    private val _hiddenApps = MutableStateFlow<List<HiddenApp>>(emptyList())
    val hiddenApps: StateFlow<List<HiddenApp>> = _hiddenApps.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _showSystem = MutableStateFlow(false)
    val showSystem: StateFlow<Boolean> = _showSystem.asStateFlow()

    private val _hideStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val hideStatus: StateFlow<Map<String, Boolean>> = _hideStatus.asStateFlow()

    private val _hiddenPackages = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPackages: StateFlow<Set<String>> = _hiddenPackages.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        refreshHidden()
    }

    fun refreshHidden() {
        _hiddenApps.value = repository.listHiddenApps()
        _hiddenPackages.value = _hiddenApps.value.map { it.packageName }.toSet()
    }

    fun refreshHideStatus() {
        Thread {
            val disabled = ShizukuManager.listDisabledPackages()
            val status = _hiddenApps.value.associate { it.packageName to (it.packageName in disabled) }
            _hideStatus.value = status
        }.start()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val own = context.packageName
            val list = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName != own }
                .map {
                    InstalledAppInfo(
                        packageName = it.packageName,
                        label = pm.getApplicationLabel(it).toString(),
                        isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        enabled = it.enabled
                    )
                }
                .sortedBy { it.label.lowercase() }
            withContext(Dispatchers.Main) { _installedApps.value = list }
        }
    }

    fun addHiddenApp(info: InstalledAppInfo, password: String) {
        if (!ShizukuManager.isPermissionGranted()) {
            _toast.value = context.getString(R.string.toast_shizuku_unauthorized_hide)
            return
        }
        val app = HiddenApp(
            packageName = info.packageName,
            className = null,
            label = info.label,
            password = password
        )
        repository.addHiddenApp(app)
        refreshHidden()
        _toast.value = context.getString(R.string.toast_adding, info.label)
        Thread {
            val result = ShizukuManager.disablePackage(info.packageName)
            _toast.value = if (result.success) {
                context.getString(R.string.toast_hidden, info.label)
            } else {
                context.getString(R.string.toast_hide_failed, result.message)
            }
            refreshHideStatus()
        }.start()
    }

    fun removeHiddenApp(packageName: String) {
        repository.removeHiddenApp(packageName)
        refreshHidden()
        Thread {
            val result = ShizukuManager.enablePackage(packageName)
            _toast.value = if (result.success) {
                context.getString(R.string.toast_restored)
            } else {
                context.getString(R.string.toast_restore_failed, result.message)
            }
            refreshHideStatus()
        }.start()
    }

    fun updatePassword(packageName: String, newPassword: String) {
        val current = repository.findByPackage(packageName) ?: return
        repository.addHiddenApp(current.copy(password = newPassword))
        refreshHidden()
        _toast.value = context.getString(R.string.toast_pwd_updated)
    }

    fun updateDefaultPassword(newPassword: String): Boolean {
        if (newPassword.length < 4) {
            _toast.value = context.getString(R.string.pwd_error_min_length)
            return false
        }
        repository.setDefaultPassword(newPassword)
        _toast.value = context.getString(R.string.toast_admin_pwd_updated)
        return true
    }

    fun consumeToast() { _toast.value = null }

    fun toggleShowSystem() { _showSystem.value = !_showSystem.value }

    fun hideAll() {
        val apps = repository.listHiddenApps()
        if (apps.isEmpty()) {
            _toast.value = context.getString(R.string.toast_list_empty)
            return
        }
        if (!ShizukuManager.isPermissionGranted()) {
            _toast.value = context.getString(R.string.toast_shizuku_unauthorized)
            return
        }
        _toast.value = context.getString(R.string.toast_hiding_n, apps.size)
        Thread {
            var ok = 0
            apps.forEach { app ->
                if (ShizukuManager.disablePackage(app.packageName).success) ok++
            }
            _toast.value = context.getString(R.string.toast_hide_all_result, ok, apps.size)
            refreshHideStatus()
        }.start()
    }

    fun hideOne(packageName: String) {
        val app = repository.findByPackage(packageName) ?: return
        if (!ShizukuManager.isPermissionGranted()) {
            _toast.value = context.getString(R.string.toast_shizuku_unauthorized)
            return
        }
        Thread {
            val r = ShizukuManager.disablePackage(packageName)
            _toast.value = if (r.success) {
                context.getString(R.string.toast_hidden, app.label)
            } else {
                context.getString(R.string.toast_hide_failed, r.message)
            }
            refreshHideStatus()
        }.start()
    }

    fun launchApp(packageName: String) {
        val app = repository.findByPackage(packageName) ?: return
        if (!ShizukuManager.isPermissionGranted()) {
            _toast.value = context.getString(R.string.toast_shizuku_unauthorized)
            return
        }
        Thread {
            val ok = AppLauncher.launchHiddenApp(app, repository)
            _toast.value = if (ok) {
                context.getString(R.string.toast_launching, app.label)
            } else {
                context.getString(R.string.toast_launch_failed, app.label)
            }
            if (ok) refreshHideStatus()
        }.start()
    }
}
