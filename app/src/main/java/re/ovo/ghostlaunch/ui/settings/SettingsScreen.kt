package re.ovo.ghostlaunch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.ui.PasswordInputDialog
import re.ovo.ghostlaunch.ui.PasswordReminderDialog
import re.ovo.ghostlaunch.ui.manager.ManagerViewModel

@Composable
fun SettingsScreen(
    viewModel: ManagerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var editingPassword by remember { mutableStateOf(false) }
    var passwordReminder by remember { mutableStateOf<String?>(null) }

    val versionName = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("1.0")
    }

    val titleSettings = stringResource(R.string.settings)
    val titleAbout = stringResource(R.string.settings_about)
    val titleMgmt = stringResource(R.string.settings_management)
    val appNameLabel = stringResource(R.string.settings_app_name_label)
    val appNameValue = stringResource(R.string.settings_app_name_value)
    val versionLabel = stringResource(R.string.settings_version_label)
    val pkgLabel = stringResource(R.string.settings_pkg_label)
    val changeAdminPwd = stringResource(R.string.settings_change_admin_pwd)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                titleSettings,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { SectionHeader(titleAbout) }
            item { InfoRow(appNameLabel, appNameValue) }
            item { InfoRow(versionLabel, versionName ?: "1.0") }
            item { InfoRow(pkgLabel, context.packageName) }

            item { Spacer(Modifier.size(16.dp)) }

            item { SectionHeader(titleMgmt) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .clickable { editingPassword = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(changeAdminPwd, fontSize = 15.sp, color = Color.Black, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFAAAAAA))
                }
            }
        }
    }

    if (editingPassword) {
        PasswordInputDialog(
            title = stringResource(R.string.admin_pwd_title),
            confirmText = stringResource(R.string.ok),
            label = stringResource(R.string.admin_pwd_label),
            hint = stringResource(R.string.admin_pwd_hint),
            onConfirm = { pwd ->
                if (viewModel.updateDefaultPassword(pwd)) {
                    editingPassword = false
                    passwordReminder = pwd
                }
            },
            onDismiss = { editingPassword = false }
        )
    }

    passwordReminder?.let { pwd ->
        PasswordReminderDialog(password = pwd, onDismiss = { passwordReminder = null })
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF888888),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(6.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}
