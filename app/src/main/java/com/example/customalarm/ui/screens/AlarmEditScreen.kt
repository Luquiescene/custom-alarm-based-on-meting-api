package com.example.customalarm.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.customalarm.data.prefs.CapabilityEntry
import com.example.customalarm.di.AppViewModelProvider
import com.example.customalarm.domain.AlarmDefaults
import com.example.customalarm.domain.RepeatRule
import com.example.customalarm.domain.RingSource
import com.example.customalarm.ui.viewmodel.AlarmEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(alarmId: Long, onBack: () -> Unit) {
    val vm: AlarmEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val is24 = remember { DateFormat.is24HourFormat(context) }

    LaunchedEffect(alarmId) { vm.load(alarmId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId >= 0) "编辑闹钟" else "新建闹钟") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = { vm.save(onBack) }, enabled = !vm.saving) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
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
            TimeSelector(vm.hour, vm.minute, is24, onHourChange = { vm.hour = it }, onMinuteChange = { vm.minute = it })

            SectionTitle("标签")
            OutlinedTextField(
                value = vm.label, onValueChange = { vm.label = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：起床") },
                singleLine = true
            )

            SectionTitle("重复")
            RepeatSelector(vm.repeatDays, onDaysChange = { vm.repeatDays = it })

            SectionTitle("贪睡")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("启用贪睡", Modifier.weight(1f))
                Switch(checked = vm.snoozeEnabled, onCheckedChange = { vm.snoozeEnabled = it })
            }
            if (vm.snoozeEnabled) {
                StepperRow("间隔（分钟）", vm.snoozeInterval, 1, 30) { vm.snoozeInterval = it }
                StepperRow("最多次数", vm.snoozeMax, 1, 10) { vm.snoozeMax = it }
            }

            SectionTitle("响铃时长（${vm.ringDuration} 秒）")
            Slider(
                value = vm.ringDuration.toFloat(),
                onValueChange = { vm.ringDuration = it.toInt() },
                valueRange = 10f..300f
            )

            SectionTitle("震动与音量")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("震动", Modifier.weight(1f))
                Switch(checked = vm.vibrate, onCheckedChange = { vm.vibrate = it })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("音量渐强", Modifier.weight(1f))
                Switch(checked = vm.volumeRamp, onCheckedChange = { vm.volumeRamp = it })
            }

            SectionTitle("铃声来源")
            SourceSelector(
                current = vm.ringSource,
                matrix = settings.capabilityMatrix,
                onSelect = { vm.onRingSourceChange(it) }
            )

            if (vm.ringSource == RingSource.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = vm.customUrl, onValueChange = { vm.onCustomUrlChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("音频直链 或 自定义 Meting 地址") },
                    singleLine = true
                )
            }

            SectionTitle("歌单 ID")
            val playlistDisabled = vm.isCustomDirect()
            OutlinedTextField(
                value = vm.playlistId, onValueChange = { vm.onPlaylistIdChange(it.filter(Char::isDigit)) },
                modifier = Modifier.fillMaxWidth().alpha(if (playlistDisabled) 0.4f else 1f),
                enabled = !playlistDisabled,
                placeholder = { Text("1–20 位数字") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (!playlistDisabled) {
                Text(
                    "提示：歌单内点分享→复制链接，id= 后面的数字即为歌单 ID",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (playlistDisabled) {
                Text("自定义直链模式下歌单 ID 已禁用", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                OutlinedButton(
                    onClick = { vm.fetchPreview() },
                    enabled = !vm.previewLoading,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(if (vm.previewLoading) "加载中…" else "加载歌单预览")
                }
                if (vm.previewSongs.isNotEmpty()) {
                    Text(
                        "共 ${vm.previewSongs.size} 首 · 前 3 首：${vm.previewSongs.take(3).joinToString(" | ") { it.title.ifBlank { it.songId } }}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            vm.saveError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.save(onBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !vm.saving
            ) { Text(if (vm.saving) "保存中…" else "保存") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )
}

@Composable
private fun TimeSelector(hour: Int, minute: Int, is24: Boolean, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        val hourDisplay = if (is24) hour else ((hour + 11) % 12) + 1
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("时", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Stepper(hourDisplay.toString(), onInc = { onHourChange((hour + 1) % 24) }, onDec = { onHourChange((hour + 23) % 24) })
        }
        Text(":", fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("分", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Stepper(String.format("%02d", minute), onInc = { onMinuteChange((minute + 1) % 60) }, onDec = { onMinuteChange((minute + 59) % 60) })
        }
        if (!is24) {
            Spacer(Modifier.width(16.dp))
            Text(if (hour < 12) "AM" else "PM", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Stepper(value: String, onInc: () -> Unit, onDec: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("−", fontSize = 28.sp, modifier = Modifier.clickable(onClick = onDec).padding(horizontal = 12.dp))
        Text(value, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("＋", fontSize = 28.sp, modifier = Modifier.clickable(onClick = onInc).padding(horizontal = 12.dp))
    }
}

@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Text("−", fontSize = 22.sp, modifier = Modifier.clickable { onValue((value - 1).coerceAtLeast(min)) }.padding(horizontal = 10.dp))
        Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("＋", fontSize = 22.sp, modifier = Modifier.clickable { onValue((value + 1).coerceAtMost(max)) }.padding(horizontal = 10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatSelector(days: Set<Int>, onDaysChange: (Set<Int>) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = days.isEmpty(),
            onClick = { onDaysChange(emptySet()) },
            label = { Text("仅一次") }
        )
        RepeatRule.ALL_DAYS.forEach { d ->
            FilterChip(
                selected = d in days,
                onClick = {
                    onDaysChange(if (d in days) days - d else days + d)
                },
                label = { Text(RepeatRule.weekdayName(d)) }
            )
        }
    }
    Text("展示为：${RepeatRule.summary(days)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceSelector(current: RingSource, matrix: List<CapabilityEntry>, onSelect: (RingSource) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RingSource.entries.forEach { source ->
                val cap = matrix.firstOrNull { it.server == source.server }
                val disabled = source != RingSource.CUSTOM && source != RingSource.NETEASE && cap != null && !cap.passed
                DropdownMenuItem(
                    text = {
                        Text(if (disabled) "${source.label}（当前 API 不支持）" else source.label)
                    },
                    enabled = !disabled,
                    onClick = {
                        onSelect(source)
                        expanded = false
                    }
                )
            }
        }
    }
}
