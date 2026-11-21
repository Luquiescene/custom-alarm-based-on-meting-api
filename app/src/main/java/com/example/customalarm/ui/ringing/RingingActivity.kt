package com.example.customalarm.ui.ringing

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.customalarm.di.AppGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全屏响铃 UI：仅负责展示与「停止/贪睡」按钮，实际播放/震动/唤醒由 [RingingService] 承担。
 */
class RingingActivity : ComponentActivity() {

    private var alarmId = -1L
    private var isTest = false
    private var scheduledAt = 0L
    private var actualTriggerAt = 0L

    private var remaining by mutableIntStateOf(0)
    private var label by mutableStateOf("")
    private var timeText by mutableStateOf("")
    private var snoozeAvailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLockScreenFlags()

        val container = AppGraph.container
        if (container == null) { finish(); return }

        alarmId = intent.getLongExtra(RingingContract.EXTRA_ALARM_ID, -1L)
        isTest = intent.getBooleanExtra(RingingContract.EXTRA_IS_TEST, false)
        scheduledAt = intent.getLongExtra(RingingContract.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        actualTriggerAt = intent.getLongExtra(RingingContract.EXTRA_ACTUAL_TRIGGER_AT, System.currentTimeMillis())

        setContent {
            RingingScreen(
                timeText = timeText,
                label = label,
                statusText = "正在响铃",
                remaining = remaining,
                snoozeAvailable = snoozeAvailable,
                onStop = { stop() },
                onSnooze = { snooze() }
            )
        }

        lifecycleScope.launch {
            if (isTest) {
                label = "测试响铃"
                timeText = "测试"
                remaining = 45
                snoozeAvailable = false
            } else {
                val a = container.alarmRepository.getById(alarmId)
                label = a?.label?.ifBlank { null } ?: "闹钟"
                timeText = if (a != null) String.format("%02d:%02d", a.hour, a.minute) else "闹钟"
                remaining = a?.ringDurationSeconds ?: 45
                snoozeAvailable = a?.snoozeEnabled ?: true
            }
            while (remaining > 0) {
                delay(1000)
                remaining--
            }
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLockScreenFlags()
    }

    private fun applyLockScreenFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        }
    }

    private fun stop() {
        sendServiceAction(RingingContract.ACTION_STOP)
        finish()
    }

    private fun snooze() {
        sendServiceAction(RingingContract.ACTION_SNOOZE)
        finish()
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, RingingService::class.java).apply {
            putExtra(RingingContract.EXTRA_ACTION, action)
            putExtra(RingingContract.EXTRA_ALARM_ID, alarmId)
            putExtra(RingingContract.EXTRA_IS_TEST, isTest)
            putExtra(RingingContract.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(RingingContract.EXTRA_ACTUAL_TRIGGER_AT, actualTriggerAt)
        }
        startService(intent)
    }
}

@Composable
private fun RingingScreen(
    timeText: String,
    label: String,
    statusText: String,
    remaining: Int,
    snoozeAvailable: Boolean,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    val bg = Color(0xFF0E0E12)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = timeText, color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(text = label, color = Color(0xFFCFCFD4), fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = statusText, color = Color(0xFF9A9AA0), fontSize = 16.sp)
            Spacer(Modifier.height(48.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (snoozeAvailable) {
                    OutlinedButton(
                        onClick = onSnooze,
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.width(120.dp).height(72.dp)
                    ) { Text("贪睡", fontSize = 20.sp) }
                }
                Button(
                    onClick = onStop,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
                    modifier = Modifier.width(160.dp).height(72.dp)
                ) { Text("停止", fontSize = 20.sp) }
            }
        }
    }
}
