package com.example.customalarm.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.customalarm.data.local.RingLogEntity
import com.example.customalarm.di.AppViewModelProvider
import com.example.customalarm.ui.viewmodel.DebugViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit) {
    val vm: DebugViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val logs by vm.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调试") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Button(onClick = { vm.scheduleTestRing() }, modifier = Modifier.padding(16.dp)) {
                Text("10 秒后测试响铃（在线播放）")
            }

            Text("最近 20 条响铃日志", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            if (logs.isEmpty()) {
                Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                LazyColumn {
                    items(logs) { log -> LogRow(log) }
                }
            }
        }
    }
}

@Composable
private fun LogRow(log: RingLogEntity) {
    val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row {
            Text("预定 ${fmt.format(Date(log.scheduledAt))}  触发 ${fmt.format(Date(log.actualTriggerAt))}", fontSize = 12.sp)
        }
        Text(
            "曲目：${log.trackTitle.ifBlank { "—" }}  结果：${log.playResult}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        if (!log.error.isNullOrBlank()) {
            Text("错误：${log.error}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
    HorizontalDivider()
}
