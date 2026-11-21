package com.example.customalarm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.customalarm.BuildConfig
import com.example.customalarm.data.prefs.ApiEndpoints
import com.example.customalarm.data.prefs.ThemeMode
import com.example.customalarm.di.AppViewModelProvider
import com.example.customalarm.ui.viewmodel.SettingsViewModel
import com.example.customalarm.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onUsage: () -> Unit, onAbout: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionTitle("外观主题")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode.name,
                        onClick = { vm.setTheme(mode) },
                        label = { Text(themeLabel(mode)) }
                    )
                }
            }

            SectionTitle("API 配置")
            ApiSelector(
                selectedIndex = settings.apiSelectedIndex,
                customUrl = settings.apiCustomUrl,
                onSelect = { index, custom -> vm.setApiSelection(index, custom) }
            )

            Button(
                onClick = { vm.testConnectivity() },
                enabled = !vm.testing,
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(if (vm.testing) "测试中…" else "测试连通性") }

            vm.testMessage?.let {
                Text(it, color = if (settings.apiValidated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (vm.matrix.isNotEmpty()) {
                Text("能力矩阵：", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                vm.matrix.forEach { entry ->
                    Text(
                        "${entry.server}: ${if (entry.passed) "通过" else "失败"}${if (entry.passed) "（${entry.latencyMs}ms）" else "（${entry.reason}）"}",
                        fontSize = 13.sp,
                        color = if (entry.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (entry.urlResolvable == false) {
                        Text(
                            "提示：${entry.server} 音频解析不可用，仅能获取歌单信息，铃声可能无法缓存",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                }
            }

            SectionTitle("权限体检")
            PermissionRow(context, refresh, "精确闹钟", PermissionHelper.canScheduleExactAlarms(context)) {
                PermissionHelper.openExactAlarmSettings(context)
            }
            PermissionRow(context, refresh, "全屏通知", PermissionHelper.canUseFullScreenIntent(context)) {
                PermissionHelper.openFullScreenIntentSettings(context)
            }
            PermissionRow(context, refresh, "显示在其他应用之上", PermissionHelper.canDrawOverlays(context)) {
                PermissionHelper.openDrawOverlaySettings(context)
            }
            PermissionRow(context, refresh, "通知", PermissionHelper.hasPostNotifications(context)) {
                PermissionHelper.openNotificationSettings(context)
            }
            PermissionRow(context, refresh, "忽略电池优化", PermissionHelper.isIgnoringBatteryOptimizations(context)) {
                PermissionHelper.openBatteryOptimizationSettings(context)
            }
            Text(
                "最近任务里把应用上锁（下拉锁定），避免被划掉",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            SectionTitle("其他")
            SettingsLink("使用说明", onUsage)
            SettingsLink("关于", onAbout)
            Text(
                "定制闹钟 v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiSelector(selectedIndex: Int, customUrl: String, onSelect: (Int, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = apiLabel(selectedIndex, customUrl),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ApiEndpoints.PRESETS.forEachIndexed { i, url ->
                    DropdownMenuItem(
                        text = { Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelect(i, customUrl); expanded = false }
                    )
                }
                DropdownMenuItem(
                    text = { Text("自定义…") },
                    onClick = { onSelect(3, customUrl); expanded = false }
                )
            }
        }
        if (selectedIndex == 3) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customUrl,
                onValueChange = { onSelect(3, it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("完整 base URL（含路径）") },
                singleLine = true,
                isError = customUrl.isNotBlank() && !isValidUrl(customUrl)
            )
            if (customUrl.isNotBlank() && !isValidUrl(customUrl)) {
                Text("非法 URL", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PermissionRow(context: android.content.Context, refresh: Int, label: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Text(if (ok) "已授权" else "未授权", color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontSize = 13.sp)
        if (!ok) {
            OutlinedButton(onClick = onFix, modifier = Modifier.padding(start = 8.dp)) { Text("去设置") }
        }
    }
}

@Composable
private fun SettingsLink(text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f))
        Text("›", fontSize = 20.sp)
    }
    HorizontalDivider()
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.DARK -> "深色"
    ThemeMode.LIGHT -> "浅色"
}

private fun apiLabel(index: Int, custom: String): String =
    if (index in 0..2) ApiEndpoints.PRESETS[index] else if (custom.isBlank()) "自定义（未填写）" else custom

private fun isValidUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return runCatching {
        val uri = java.net.URI(url.trim())
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
