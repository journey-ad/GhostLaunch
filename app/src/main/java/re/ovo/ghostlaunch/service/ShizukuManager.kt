package re.ovo.ghostlaunch.service

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    private const val TAG = "CalcShizuku"

    fun isRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun isPermissionGranted(): Boolean = try {
        isRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun shouldRequestPermission(): Boolean = try {
        isRunning() && !isPermissionGranted()
    } catch (_: Throwable) {
        false
    }

    fun requestPermission(requestCode: Int = 0) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Throwable) {
            Log.e(TAG, "requestPermission failed", e)
        }
    }

    fun addBinderListener(listener: () -> Unit) {
        Shizuku.addBinderReceivedListenerSticky { listener() }
    }

    fun addPermissionListener(callback: (Int, Int) -> Unit) {
        Shizuku.addRequestPermissionResultListener { requestCode, result ->
            callback(requestCode, result)
        }
    }

    data class ExecResult(
        val code: Int,
        val stdout: String,
        val stderr: String,
        val error: String? = null
    ) {
        val isSuccess: Boolean get() = code == 0 && error == null
    }

    private fun exec(cmd: Array<String>): ExecResult {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            val process = method.invoke(null, cmd, null, null) as Process
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val code = process.waitFor()
            Log.d(TAG, "exec: ${cmd.joinToString(" ")} -> code=$code stdout=$stdout stderr=$stderr")
            ExecResult(code, stdout.trim(), stderr.trim())
        } catch (e: Throwable) {
            Log.e(TAG, "exec failed: ${cmd.joinToString(" ")}", e)
            ExecResult(-1, "", "", e.message ?: e.javaClass.simpleName)
        }
    }

    data class PackageResult(val success: Boolean, val message: String)

    fun enablePackage(packageName: String): PackageResult {
        val r = exec(arrayOf("pm", "enable", packageName))
        return if (r.isSuccess) {
            PackageResult(true, "已启用 $packageName")
        } else {
            PackageResult(false, "enable 失败: ${r.error ?: r.stderr.ifEmpty { "code=${r.code}" }}")
        }
    }

    fun disablePackage(packageName: String): PackageResult {
        val r = exec(arrayOf("pm", "disable-user", "--user", "0", packageName))
        return if (r.isSuccess) {
            PackageResult(true, "已隐藏 $packageName")
        } else {
            PackageResult(false, "disable 失败: ${r.error ?: r.stderr.ifEmpty { "code=${r.code}" }}")
        }
    }

    fun isPackageDisabled(packageName: String): Boolean = try {
        val r = exec(arrayOf("sh", "-c", "pm list packages -d"))
        r.isSuccess && r.stdout.contains("package:$packageName")
    } catch (_: Throwable) {
        false
    }

    fun listDisabledPackages(): Set<String> = try {
        val r = exec(arrayOf("sh", "-c", "pm list packages -d"))
        if (!r.isSuccess) emptySet()
        else r.stdout.lineSequence()
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
            .toSet()
    } catch (_: Throwable) {
        emptySet()
    }
}
