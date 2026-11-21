package com.example.customalarm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.customalarm.BuildConfig

private val acknowledgements = listOf(
    "Kotlin（JetBrains）" to "Apache-2.0",
    "Jetpack Compose / Material 3（Google）" to "Apache-2.0",
    "AndroidX：Lifecycle / Navigation / Room / DataStore / Media3" to "Apache-2.0",
    "OkHttp（Square）" to "Apache-2.0",
    "kotlinx.serialization（JetBrains）" to "Apache-2.0",
    "kotlinx.coroutines（JetBrains）" to "Apache-2.0",
    "Meting API（metowolf）" to "MIT",
    "JUnit（仅测试）" to "EPL-1.0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
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
            SectionHeader("应用")
            LabelRow("名称", "定制闹钟")
            LabelRow("版本", BuildConfig.VERSION_NAME)

            SectionHeader("开发者")
            LabelRow("作者", "Luquiescene")

            SectionHeader("联系方式")
            LinkRow("博客", "https://luq-blog.xyz/2026-11-21-custom-alarm") { uriHandler.openUri("https://luq-blog.xyz/2026-11-21-custom-alarm") }
            LinkRow("GitHub", "Luquiescene") { uriHandler.openUri("https://github.com/Luquiescene") }
            LinkRow("仓库", "Luquiescene/custom-alarm-based-on-meting-api") { uriHandler.openUri("https://github.com/Luquiescene/custom-alarm-based-on-meting-api") }
            LinkRow("邮箱", "luquiescene@163.com") { uriHandler.openUri("mailto:luquiescene@163.com") }

            SectionHeader("开源协议")
            Text("Apache License 2.0", fontSize = 14.sp)
            Text("详见项目根目录 LICENSE 文件。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionHeader("致谢")
            acknowledgements.forEach { (name, license) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), fontSize = 14.sp)
                    Text(license, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                "定制闹钟 v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
}

@Composable
private fun LabelRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(72.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp)
    }
}

@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.width(72.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    }
}
