package re.ovo.ghostlaunch.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.data.HiddenApp

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onOpenManager: () -> Unit,
    onRequestPermission: () -> Unit,
    onToast: (String) -> Unit,
    onLaunched: (HiddenApp) -> Unit
) {
    val display by viewModel.display.collectAsState()
    val shizukuRunning by viewModel.shizukuRunning.collectAsState()
    val permissionGranted by viewModel.permissionGranted.collectAsState()
    val navEvent by viewModel.navEvent.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val statusNotRunning = stringResource(R.string.shizuku_not_running)
    val statusNotAuth = stringResource(R.string.shizuku_not_authorized)
    val statusReady = stringResource(R.string.shizuku_ready)

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkAndHideLeaked()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(navEvent) {
        when (val e = navEvent) {
            is CalcNavEvent.OpenManager -> {
                onOpenManager()
                viewModel.clearAfterNavigate()
                viewModel.consumeNavEvent()
            }
            is CalcNavEvent.LaunchApp -> {
                onLaunched(e.app)
                viewModel.clearAfterNavigate()
                viewModel.consumeNavEvent()
            }
            is CalcNavEvent.RequestShizukuPermission -> {
                onRequestPermission()
                viewModel.consumeNavEvent()
            }
            is CalcNavEvent.Toast -> {
                onToast(e.msg)
                viewModel.consumeNavEvent()
            }
            null -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(12.dp)
    ) {
        val statusText = when {
            !shizukuRunning -> statusNotRunning
            !permissionGranted -> statusNotAuth
            else -> statusReady
        }
        val statusColor = if (shizukuRunning && permissionGranted) Color(0xFF1B5E20) else Color(0xFF8B0000)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = statusText, color = statusColor, fontSize = 12.sp)
        }

        Display(
            text = display,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Keypad(
            onDigit = viewModel::onDigit,
            onOperator = viewModel::onOperator,
            onClear = viewModel::onClear,
            onBackspace = viewModel::onBackspace,
            onPercent = viewModel::onPercent,
            onEquals = viewModel::onEquals
        )
    }
}

@Composable
private fun Display(text: String, modifier: Modifier = Modifier) {
    val fontSize = when {
        text.length <= 8 -> 56.sp
        text.length <= 12 -> 44.sp
        text.length <= 16 -> 36.sp
        text.length <= 20 -> 28.sp
        else -> 22.sp
    }
    Box(
        modifier = modifier.padding(vertical = 8.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit
) {
    val ac = stringResource(R.string.calc_clear)
    val bs = stringResource(R.string.calc_backspace)
    val div = stringResource(R.string.calc_divide)
    val mul = stringResource(R.string.calc_multiply)
    val sub = stringResource(R.string.calc_subtract)
    val add = stringResource(R.string.calc_add)
    val eq = stringResource(R.string.calc_equals)
    val pct = stringResource(R.string.calc_percent)
    val dot = stringResource(R.string.calc_dot)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FuncKey(ac, Modifier.weight(1f)) { onClear() }
            FuncKey(pct, Modifier.weight(1f)) { onPercent() }
            FuncKey(bs, Modifier.weight(1f)) { onBackspace() }
            OpKey(div, Modifier.weight(1f)) { onOperator("/") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumKey("7", Modifier.weight(1f)) { onDigit("7") }
            NumKey("8", Modifier.weight(1f)) { onDigit("8") }
            NumKey("9", Modifier.weight(1f)) { onDigit("9") }
            OpKey(mul, Modifier.weight(1f)) { onOperator("*") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumKey("4", Modifier.weight(1f)) { onDigit("4") }
            NumKey("5", Modifier.weight(1f)) { onDigit("5") }
            NumKey("6", Modifier.weight(1f)) { onDigit("6") }
            OpKey(sub, Modifier.weight(1f)) { onOperator("-") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumKey("1", Modifier.weight(1f)) { onDigit("1") }
            NumKey("2", Modifier.weight(1f)) { onDigit("2") }
            NumKey("3", Modifier.weight(1f)) { onDigit("3") }
            OpKey(add, Modifier.weight(1f)) { onOperator("+") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumKey("0", Modifier.weight(2f)) { onDigit("0") }
            NumKey(dot, Modifier.weight(1f)) { onDigit(".") }
            EqualsKey(eq, Modifier.weight(1f)) { onEquals() }
        }
    }
}

@Composable
private fun RowScope.NumKey(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 26.sp, color = Color.Black)
    }
}

@Composable
private fun RowScope.OpKey(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 26.sp, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowScope.FuncKey(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 22.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowScope.EqualsKey(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
