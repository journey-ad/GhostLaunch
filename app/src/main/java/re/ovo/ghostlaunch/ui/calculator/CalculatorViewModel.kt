package re.ovo.ghostlaunch.ui.calculator

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.data.AppRepository
import re.ovo.ghostlaunch.data.HiddenApp
import re.ovo.ghostlaunch.service.AppLauncher
import re.ovo.ghostlaunch.service.ShizukuManager

sealed class CalcNavEvent {
    data object OpenManager : CalcNavEvent()
    data class LaunchApp(val app: HiddenApp) : CalcNavEvent()
    data object RequestShizukuPermission : CalcNavEvent()
    data class Toast(val msg: String) : CalcNavEvent()
}

class CalculatorViewModel(
    private val context: Context,
    private val repository: AppRepository = AppRepository(context)
) : ViewModel() {

    private val _display = MutableStateFlow("0")
    val display: StateFlow<String> = _display.asStateFlow()

    private val _shizukuRunning = MutableStateFlow(false)
    val shizukuRunning: StateFlow<Boolean> = _shizukuRunning.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _navEvent = MutableStateFlow<CalcNavEvent?>(null)
    val navEvent: StateFlow<CalcNavEvent?> = _navEvent.asStateFlow()

    private val buffer = StringBuilder()
    private var isResult = false

    init {
        refreshShizukuState()
        ShizukuManager.addBinderListener { refreshShizukuState() }
        ShizukuManager.addPermissionListener { _, _ -> refreshShizukuState() }
        checkAndHideLeaked()
    }

    fun checkAndHideLeaked() {
        Thread {
            if (!ShizukuManager.isRunning()) return@Thread
            if (!ShizukuManager.isPermissionGranted()) return@Thread
            val apps = repository.listHiddenApps()
            if (apps.isEmpty()) return@Thread
            val disabled = ShizukuManager.listDisabledPackages()
            apps.forEach { app ->
                if (app.packageName !in disabled) {
                    ShizukuManager.disablePackage(app.packageName)
                }
            }
        }.start()
    }

    private fun refreshShizukuState() {
        _shizukuRunning.value = ShizukuManager.isRunning()
        _permissionGranted.value = ShizukuManager.isPermissionGranted()
    }

    fun onDigit(d: String) {
        if (isResult) {
            buffer.clear()
            isResult = false
        }
        if (buffer.length >= 100) return
        val lastOpIndex = buffer.lastIndexOfAny(charArrayOf('+', '-', '*', '/'))
        val currentNumber = if (lastOpIndex >= 0) buffer.substring(lastOpIndex + 1) else buffer.toString()
        if (currentNumber.length >= 15) return
        if (buffer.toString() == "0" && d != ".") {
            buffer.clear()
        }
        buffer.append(d)
        _display.value = buffer.toString()
    }

    fun onOperator(op: String) {
        if (isResult) {
            isResult = false
        }
        if (buffer.isEmpty()) return
        val last = buffer.lastOrNull()
        if (last != null && last in "+-*/") {
            buffer.deleteCharAt(buffer.length - 1)
        }
        buffer.append(op)
        _display.value = buffer.toString()
    }

    fun onClear() {
        buffer.clear()
        isResult = false
        _display.value = "0"
    }

    fun onBackspace() {
        if (isResult) {
            onClear()
            return
        }
        if (buffer.isNotEmpty()) {
            buffer.deleteCharAt(buffer.length - 1)
            _display.value = if (buffer.isEmpty()) "0" else buffer.toString()
        }
    }

    fun onPercent() {
        if (buffer.isEmpty()) return
        val last = buffer.last()
        if (!last.isDigit() && last != '.') return
        if (isResult) {
            isResult = false
        }
        buffer.append("%")
        _display.value = buffer.toString()
    }

    fun onEquals() {
        val expr = buffer.toString()
        if (expr.isEmpty()) return

        if (expr.last() in "+-*/") return

        val isPureNumber = expr.all { it.isDigit() || it == '.' }

        if (isPureNumber && !isResult) {
            val matchedApp = repository.findByPassword(expr)
            if (matchedApp != null) {
                tryLaunch(matchedApp)
                isResult = true
                return
            }

            if (expr == repository.getDefaultPassword()) {
                _navEvent.value = CalcNavEvent.OpenManager
                isResult = true
                return
            }
        }

        val result = evaluate(expr)
        isResult = true
        buffer.clear()
        buffer.append(result)
        _display.value = result
    }

    private fun tryLaunch(app: HiddenApp) {
        when {
            !_shizukuRunning.value -> {
                _navEvent.value = CalcNavEvent.Toast(context.getString(R.string.shizuku_not_running))
            }
            !_permissionGranted.value -> {
                ShizukuManager.requestPermission()
                _navEvent.value = CalcNavEvent.RequestShizukuPermission
            }
            else -> {
                val ok = AppLauncher.launchHiddenApp(app, repository)
                if (ok) {
                    _navEvent.value = CalcNavEvent.LaunchApp(app)
                } else {
                    _navEvent.value = CalcNavEvent.Toast(
                        context.getString(R.string.toast_launch_failed, app.label)
                    )
                }
            }
        }
    }

    fun consumeNavEvent() {
        _navEvent.value = null
    }

    fun clearAfterNavigate() {
        buffer.clear()
        isResult = false
        _display.value = "0"
    }

    private fun evaluate(expr: String): String {
        return try {
            val tokens = tokenize(expr)
            if (tokens.isEmpty()) return "0"
            val value = evalTokens(tokens)
            if (value.isNaN() || value.isInfinite()) return "Error"
            formatNumber(value)
        } catch (_: Throwable) {
            "Error"
        }
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val absValue = Math.abs(value)
        return when {
            absValue == 0.0 -> "0"
            absValue >= 1e12 -> formatScientific(value)
            absValue < 1e-4 -> formatScientific(value)
            value == value.toLong().toDouble() -> value.toLong().toString()
            else -> {
                String.format(java.util.Locale.US, "%.10f", value)
                    .trimEnd('0')
                    .trimEnd('.')
            }
        }
    }

    private fun formatScientific(value: Double): String {
        val s = String.format(java.util.Locale.US, "%.6E", value)
        return s
            .replace("E+", "E")
            .replace("E-0", "E-")
            .replace("E0", "E")
            .replace(".000000E", "E")
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        val num = StringBuilder()
        for (c in expr) {
            if (c.isDigit() || c == '.' || c == 'E' || c == 'e') {
                num.append(c)
            } else if (c == '+' || c == '-') {
                if (num.isNotEmpty() && (num.last() == 'E' || num.last() == 'e')) {
                    num.append(c)
                } else {
                    if (num.isNotEmpty()) {
                        tokens.add(num.toString())
                        num.clear()
                    }
                    tokens.add(c.toString())
                }
            } else if (c == '*' || c == '/') {
                if (num.isNotEmpty()) {
                    tokens.add(num.toString())
                    num.clear()
                }
                tokens.add(c.toString())
            } else if (c == '%') {
                if (num.isNotEmpty()) {
                    val bd = java.math.BigDecimal(num.toString())
                        .divide(java.math.BigDecimal(100))
                    num.clear()
                    num.append(bd.toPlainString())
                }
            }
        }
        if (num.isNotEmpty()) tokens.add(num.toString())
        return tokens
    }

    private fun evalTokens(tokens: List<String>): Double {
        if (tokens.isEmpty()) return 0.0
        var left = tokens[0].toDoubleOrNull() ?: return Double.NaN
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val right = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: return Double.NaN
            left = when (op) {
                "+" -> left + right
                "-" -> left - right
                "*" -> left * right
                "/" -> if (right == 0.0) Double.NaN else left / right
                else -> return Double.NaN
            }
            i += 2
        }
        return left
    }
}
