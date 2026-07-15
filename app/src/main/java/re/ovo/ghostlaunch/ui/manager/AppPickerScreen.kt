package re.ovo.ghostlaunch.ui.manager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.ui.PasswordInputDialog

@Composable
fun AppPickerScreen(
    viewModel: ManagerViewModel,
    onBack: () -> Unit
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val showSystem by viewModel.showSystem.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<InstalledAppInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
        viewModel.refreshHidden()
    }

    val filtered = remember(installedApps, query, showSystem) {
        val bySystem = if (showSystem) installedApps else installedApps.filter { !it.isSystem }
        if (query.isBlank()) bySystem
        else bySystem.filter {
            it.label.contains(query, true) || it.packageName.contains(query, true)
        }
    }

    val added = filtered.filter { it.packageName in hiddenPackages }
    val notAdded = filtered.filter { it.packageName !in hiddenPackages }

    val title = stringResource(R.string.picker_title)
    val searchHint = stringResource(R.string.picker_search)
    val showSystemLabel = stringResource(R.string.picker_show_system)
    val systemBadge = stringResource(R.string.picker_system_badge)
    val loadingText = stringResource(R.string.picker_loading)
    val noMatchText = stringResource(R.string.picker_no_match)
    val sectionAdded = stringResource(R.string.picker_section_added)
    val sectionNotAdded = stringResource(R.string.picker_section_not_added)

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
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(searchHint) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                showSystemLabel,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = showSystem,
                onCheckedChange = { viewModel.toggleShowSystem() }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (added.isNotEmpty()) {
                item { SectionHeader(sectionAdded.format(added.size)) }
                items(added, key = { "added_" + it.packageName }) { info ->
                    AppRow(info, systemBadge) { selected = info }
                }
            }
            if (notAdded.isNotEmpty()) {
                item { SectionHeader(sectionNotAdded.format(notAdded.size)) }
                items(notAdded, key = { "not_" + it.packageName }) { info ->
                    AppRow(info, systemBadge) { selected = info }
                }
            }
            if (added.isEmpty() && notAdded.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (query.isBlank()) loadingText else noMatchText,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }

    selected?.let { info ->
        val titleStr = stringResource(R.string.app_pwd_title_set)
        val confirmStr = stringResource(R.string.app_pwd_confirm_add)
        val hintStr = stringResource(R.string.app_pwd_hint)
        val appStr = stringResource(R.string.app_pwd_header_app, info.label)
        val pkgStr = stringResource(R.string.app_pwd_header_pkg, info.packageName)

        PasswordInputDialog(
            title = titleStr,
            confirmText = confirmStr,
            allowEmpty = true,
            header = {
                Column {
                    Text(appStr, fontSize = 14.sp)
                    Text(pkgStr, fontSize = 12.sp, color = Color(0xFF666666))
                }
            },
            hint = hintStr,
            onConfirm = { pwd ->
                viewModel.addHiddenApp(info, pwd)
                selected = null
            },
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF666666),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun AppRow(info: InstalledAppInfo, systemBadge: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(info.packageName, 40)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                info.label,
                fontSize = 15.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
            Text(
                info.packageName,
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )
        }
        if (info.isSystem) {
            Text(
                systemBadge,
                fontSize = 10.sp,
                color = Color(0xFF999999),
                modifier = Modifier.background(
                    Color(0xFFEEEEEE),
                    RoundedCornerShape(4.dp)
                ).padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun AppIcon(packageName: String, sizeDp: Int) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density * 2).toInt()
    val icon = remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToImageBitmap(drawable, sizePx)
        }.getOrNull()
    }
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(sizeDp.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
        )
    }
}

private fun drawableToImageBitmap(drawable: Drawable, size: Int) =
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
    }.asImageBitmap()
