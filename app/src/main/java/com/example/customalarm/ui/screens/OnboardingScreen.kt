package com.example.customalarm.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.customalarm.di.AppViewModelProvider
import com.example.customalarm.ui.viewmodel.SettingsViewModel
import com.example.customalarm.util.PermissionHelper

@Composable
fun OnboardingScreen(onDone: () -> Unit, onUsage: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("欢迎使用定制闹钟", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("先完成权限体检，再开始使用", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))

        PermissionCard(refresh, "通知权限", "响铃通知与锁屏提醒需要通知权限", PermissionHelper.hasPostNotifications(context)) {
            PermissionHelper.openNotificationSettings(context)
        }
        PermissionCard(refresh, "精确闹钟", "保证闹钟准点触发", PermissionHelper.canScheduleExactAlarms(context)) {
            PermissionHelper.openExactAlarmSettings(context)
        }
        PermissionCard(refresh, "全屏通知", "锁屏时也能全屏响铃", PermissionHelper.canUseFullScreenIntent(context)) {
            PermissionHelper.openFullScreenIntentSettings(context)
        }
        PermissionCard(refresh, "忽略电池优化", "避免被省电策略拦截", PermissionHelper.isIgnoringBatteryOptimizations(context)) {
            PermissionHelper.openBatteryOptimizationSettings(context)
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onUsage) { Text("查看使用说明") }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                vm.setOnboardingShown(true)
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("开始使用") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(refresh: Int, title: String, desc: String, granted: Boolean, onFix: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (granted) {
                Text("已授权", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            } else {
                OutlinedButton(onClick = onFix) { Text("去授权") }
            }
        }
    }
}
