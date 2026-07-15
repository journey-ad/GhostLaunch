package re.ovo.ghostlaunch.ui.manager

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.service.AccessibilityHelper
import re.ovo.ghostlaunch.service.ShizukuManager
import re.ovo.ghostlaunch.ui.PasswordInputDialog
import re.ovo.ghostlaunch.ui.PasswordReminderDialog

@Composable
fun ManagerScreen(
    viewModel: ManagerViewModel,
    onPickApp: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit
) {
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val hideStatus by viewModel.hideStatus.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var editingPkg by remember { mutableStateOf<String?>(null) }
    var passwordReminder by remember { mutableStateOf<String?>(null) }

    var accessibilityEnabled by remember { mutableStateOf(AccessibilityHelper.isEnabled(context)) }
    val shizukuReady = ShizukuManager.isPermissionGranted()

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = AccessibilityHelper.isEnabled(context)
                viewModel.refreshHideStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshHidden()
        viewModel.refreshHideStatus()
    }

    val title = stringResource(R.string.mgr_title)
    val hideAllLabel = stringResource(R.string.mgr_hide_all)
    val addLabel = stringResource(R.string.add)
    val emptyText = stringResource(R.string.mgr_empty)
    val pwdUnset = stringResource(R.string.mgr_password_unset)
    val hiddenBadge = stringResource(R.string.mgr_hidden_badge)
    val hideBtn = stringResource(R.string.mgr_hide)
    val editPwdBtn = stringResource(R.string.mgr_edit_pwd)
    val removeBtn = stringResource(R.string.mgr_remove)
    val pwdLabelTpl = stringResource(R.string.mgr_password_label)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.hideAll() }) {
                Icon(Icons.Outlined.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(hideAllLabel, fontSize = 13.sp)
            }
            TextButton(onClick = onPickApp) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(addLabel, fontSize = 13.sp)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
            }
        }

        if (!accessibilityEnabled) {
            AccessibilityGuideCard(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                onClick = { AccessibilityHelper.openSettings(context) }
            )
            Spacer(Modifier.size(8.dp))
        }

        if (!shizukuReady) {
            ShizukuGuideCard(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                onClick = {
                    if (ShizukuManager.isRunning()) {
                        ShizukuManager.requestPermission()
                    } else {
                        Toast.makeText(context, context.getString(R.string.shizuku_start_first), Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Spacer(Modifier.size(8.dp))
        }

        if (hiddenApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emptyText,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hiddenApps, key = { it.packageName }) { app ->
                    val isHidden = hideStatus[app.packageName] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .clickable { viewModel.launchApp(app.packageName) }
                            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Spacer(Modifier.size(2.dp))
                            val pwdDisplay = app.password.ifEmpty { pwdUnset }
                            Text(
                                pwdLabelTpl.format(pwdDisplay),
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                            Text(
                                app.packageName,
                                fontSize = 11.sp,
                                color = Color(0xFF999999)
                            )
                            if (isHidden) {
                                Text(hiddenBadge, fontSize = 10.sp, color = Color(0xFF1B5E20))
                            }
                        }
                        if (!isHidden) {
                            TextButton(
                                onClick = { viewModel.hideOne(app.packageName) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) { Text(hideBtn, fontSize = 13.sp) }
                        }
                        TextButton(
                            onClick = { editingPkg = app.packageName },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = editPwdBtn, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(editPwdBtn, fontSize = 13.sp)
                        }
                        TextButton(
                            onClick = { viewModel.removeHiddenApp(app.packageName) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = removeBtn, tint = Color(0xFF8B0000), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(removeBtn, color = Color(0xFF8B0000), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    editingPkg?.let { pkg ->
        val app = hiddenApps.firstOrNull { it.packageName == pkg }
        if (app != null) {
            val titleStr = stringResource(R.string.app_pwd_title_edit)
            val okStr = stringResource(R.string.ok)
            val hintStr = stringResource(R.string.app_pwd_hint)
            val appStr = stringResource(R.string.app_pwd_header_app, app.label)
            val pkgStr = stringResource(R.string.app_pwd_header_pkg, app.packageName)

            PasswordInputDialog(
                title = titleStr,
                confirmText = okStr,
                allowEmpty = true,
                header = {
                    Column {
                        Text(appStr, fontSize = 14.sp)
                        Text(pkgStr, fontSize = 12.sp, color = Color(0xFF666666))
                    }
                },
                hint = hintStr,
                onConfirm = { pwd ->
                    viewModel.updatePassword(pkg, pwd)
                    editingPkg = null
                },
                onDismiss = { editingPkg = null }
            )
        }
    }

    passwordReminder?.let { pwd ->
        PasswordReminderDialog(password = pwd, onDismiss = { passwordReminder = null })
    }
}

@Composable
private fun AccessibilityGuideCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val title = stringResource(R.string.guide_accessibility_title)
    val desc = stringResource(R.string.guide_accessibility_desc)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE6C200), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Accessibility, contentDescription = null, tint = Color(0xFF8B6500))
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B6500))
            Text(desc, fontSize = 12.sp, color = Color(0xFF555555))
        }
        Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFF8B6500))
    }
}

@Composable
private fun ShizukuGuideCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val running = ShizukuManager.isRunning()
    val title = if (running) stringResource(R.string.shizuku_not_authorized) else stringResource(R.string.shizuku_not_running)
    val desc = if (running) stringResource(R.string.shizuku_click_auth) else stringResource(R.string.shizuku_start_service)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFB71C1C), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFFB71C1C))
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
            Text(desc, fontSize = 12.sp, color = Color(0xFF555555))
        }
    }
}
