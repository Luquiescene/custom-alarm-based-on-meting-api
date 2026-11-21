package com.example.customalarm.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class UsageSection(val title: String, val steps: List<String>)

private val usageSections = listOf(
    UsageSection(
        "① 填 API 与校验",
        listOf(
            "打开「设置 → API 配置」。",
            "默认已选第一个 API 地址，可直接点「测试连通性」。",
            "等待探测完成，看到「校验通过」即可。",
            "若默认地址不通，切换其余预设或填自定义地址再测。"
        )
    ),
    UsageSection(
        "② 新建闹钟",
        listOf(
            "回到首页，点右下角「+」新建闹钟。",
            "选好时间、标签与重复（工作日/每天/仅一次）。",
            "设置贪睡、响铃时长、震动与音量渐强。",
            "选铃声来源与歌单 ID 后点「保存」。"
        )
    ),
    UsageSection(
        "③ 填歌单 ID",
        listOf(
            "铃声来源选网易云/QQ/酷狗时，需填歌单 ID。",
            "歌单 ID 是 1–20 位纯数字。",
            "获取方法：在音乐软件的歌单内点「分享」→「复制链接」，链接中 id= 后面的数字即为歌单 ID。",
            "点「加载歌单预览」可看曲目数与前 3 首。",
            "保存时会再次校验，空歌单会拒绝保存。"
        )
    ),
    UsageSection(
        "④ 在线播放",
        listOf(
            "保存启用后，响铃时 App 在线解析并流式播放当前曲目，不下载、不缓存音频。",
            "响铃按 Meting 协议分两步：type=playlist 拉歌单 → 响铃时 type=url 解析真实音频地址。",
            "VIP/版权受限曲目解析不出音频时，响铃会降级为仅震动。",
            "响铃依赖网络，无网络或解析失败时仅震动。"
        )
    ),
    UsageSection(
        "⑤ 响铃权限与厂商设置",
        listOf(
            "首次启动按引导授权通知、精确闹钟、全屏通知权限。",
            "建议在设置里开启「忽略电池优化」，避免被省电策略拦截。",
            "小米/华为/OPPO/vivo/三星需在系统管家里允许自启动与后台。",
            "在最近任务里把应用上锁（下拉锁定），避免被划掉。"
        )
    ),
    UsageSection(
        "⑥ 常见问题",
        listOf(
            "到点只震动不响：在线解析失败（多为 VIP/版权曲目或无网络），换非付费歌单或改用自定义直链。",
            "锁屏不响/不亮屏：检查全屏通知、精确闹钟、显示在其他应用之上权限，并允许自启动。",
            "校验不过：换一个 API 地址再测，确认网络能访问该地址。",
            "切换 API 后需重新测试，旧结果会作废。"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("使用说明") },
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
            usageSections.forEach { section ->
                Text(section.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
                section.steps.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
                HorizontalDivider(Modifier.padding(top = 6.dp))
            }
        }
    }
}
