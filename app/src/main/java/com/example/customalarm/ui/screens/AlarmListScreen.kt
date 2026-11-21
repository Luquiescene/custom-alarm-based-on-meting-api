package com.example.customalarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.customalarm.data.local.AlarmEntity
import com.example.customalarm.di.AppViewModelProvider
import com.example.customalarm.domain.RepeatRule
import com.example.customalarm.domain.ScheduleCalculator
import com.example.customalarm.ui.viewmodel.AlarmListViewModel
import com.example.customalarm.util.PermissionHelper
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    onDebug: () -> Unit
) {
    val vm: AlarmListViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val alarms by vm.alarms.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<AlarmEntity?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exactOk = remember(refresh) { PermissionHelper.canScheduleExactAlarms(context) }
    val fsiOk = remember(refresh) { PermissionHelper.canUseFullScreenIntent(context) }
    val notifOk = remember(refresh) { PermissionHelper.hasPostNotifications(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定制闹钟", fontWeight = FontWeight.Bold) },
                actions = {
                    if (com.example.customalarm.BuildConfig.DEBUG) {
                        IconButton(onClick = onDebug) { Icon(Icons.Default.BugReport, contentDescription = "调试") }
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "设置") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "新建闹钟") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                !settings.apiValidated -> Banner("请先完成 API 校验，否则闹钟不可启用", Color(0xFFFFF3CD), Color(0xFF664D03), onSettings)
                !exactOk -> Banner("未授予精确闹钟权限，闹钟可能不准时", Color(0xFFFFF3CD), Color(0xFF664D03), onSettings)
                !fsiOk -> Banner("未授予全屏通知权限，锁屏可能无法全屏响铃", Color(0xFFFFF3CD), Color(0xFF664D03), onSettings)
                !notifOk -> Banner("未授予通知权限", Color(0xFFFFF3CD), Color(0xFF664D03), onSettings)
            }

            vm.enableError?.let {
                Banner(it, Color(0xFFFFE1E1), Color(0xFFB71C1C), onClick = { vm.clearEnableError() })
            }

            if (alarms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有闹钟，点右下角 + 新建", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm,
                            onClick = { onEdit(alarm.id) },
                            onToggle = { vm.setEnabled(alarm, it) },
                            onDelete = { pendingDelete = alarm }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除闹钟") },
            text = { Text("确定删除 ${String.format("%02d:%02d", alarm.hour, alarm.minute)} 的闹钟吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAlarm(alarm)
                        pendingDelete = null
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun Banner(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, color = fg, fontSize = 14.sp)
    }
}

@Composable
private fun AlarmCard(alarm: AlarmEntity, onClick: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .alpha(if (alarm.enabled) 1f else 0.55f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.padding(top = 2.dp))
                Text(
                    text = if (alarm.label.isBlank()) RepeatRule.summary(alarm.repeatDays) else alarm.label,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${RepeatRule.summary(alarm.repeatDays)} · ${nextTriggerText(alarm)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.width(4.dp))
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun nextTriggerText(alarm: AlarmEntity): String {
    if (!alarm.enabled) return "已关闭"
    val now = System.currentTimeMillis()
    val next = ScheduleCalculator.nextTriggerMillis(alarm.hour, alarm.minute, alarm.repeatDays, now)
        ?: return "已过期"
    val minutes = (next - now) / 60000L
    val zdt = Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault())
    val time = String.format("%02d:%02d", zdt.hour, zdt.minute)
    return when {
        minutes < 1 -> "即将响铃"
        minutes < 60 -> "$minutes 分钟后"
        minutes < 24 * 60 -> "${minutes / 60} 小时后（$time）"
        else -> "下次 $time"
    }
}
