package re.ovo.ghostlaunch.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    fun getDefaultPassword(): String = prefs.getString(KEY_DEFAULT_PASSWORD, "") ?: ""

    fun setDefaultPassword(password: String) {
        prefs.edit().putString(KEY_DEFAULT_PASSWORD, password).apply()
    }

    fun isFirstRun(): Boolean = getDefaultPassword().isEmpty()

    fun listHiddenApps(): List<HiddenApp> {
        val raw = prefs.getString(KEY_HIDDEN_APPS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<HiddenApp>>() {}.type
            gson.fromJson<List<HiddenApp>>(raw, type)
        }.getOrDefault(emptyList())
    }

    fun findByPassword(password: String): HiddenApp? =
        listHiddenApps().firstOrNull { it.password == password }

    fun findByPackage(packageName: String): HiddenApp? =
        listHiddenApps().firstOrNull { it.packageName == packageName }

    fun addHiddenApp(app: HiddenApp) {
        val current = listHiddenApps().toMutableList()
        current.removeAll { it.packageName == app.packageName }
        current.add(app)
        save(current)
    }

    fun removeHiddenApp(packageName: String) {
        val current = listHiddenApps().toMutableList()
        current.removeAll { it.packageName == packageName }
        save(current)
    }

    fun updateLastLaunched(packageName: String, time: Long) {
        val current = listHiddenApps().toMutableList()
        val idx = current.indexOfFirst { it.packageName == packageName }
        if (idx >= 0) {
            current[idx] = current[idx].copy(lastLaunched = time)
            save(current)
        }
    }

    private fun save(list: List<HiddenApp>) {
        prefs.edit().putString(KEY_HIDDEN_APPS, gson.toJson(list)).apply()
    }

    companion object {
        private const val PREFS_NAME = "calc_hidden_prefs"
        private const val KEY_DEFAULT_PASSWORD = "default_password"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
    }
}
