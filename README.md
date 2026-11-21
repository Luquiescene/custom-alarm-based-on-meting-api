# 定制闹钟

一个原生 Android 定制闹钟 App：从 Meting API（网易云音乐 / QQ 音乐 / 酷狗音乐）拉取歌单并**在线流式播放**为铃声，支持锁屏全屏响铃、贪睡、曲目轮播、深浅色主题。

- 应用名：定制闹钟
- 包名：`com.example.customalarm`
- minSdk 26 / targetSdk 35 / compileSdk 35
- 架构：Kotlin 2.x + Jetpack Compose（Material 3），MVVM + Repository，手写 `AppContainer` 依赖注入
- 协议：[Apache License 2.0](LICENSE)

## 特性

- 支持网易云 / QQ 音乐 / 酷狗歌单，以及自定义音频直链、自定义 Meting 模板
- 响铃时在线解析并流式播放，**不缓存音频**；VIP / 版权曲目解析失败时降级为仅震动
- 前台服务响铃：锁屏 / 后台可响、熄屏点亮屏幕、全屏响铃页
- 贪睡、曲目轮播（nextIndex）、响铃日志、API 能力探测与校验
- DataStore 主题切换（跟随系统 / 深色 / 浅色）

## 技术栈

| 模块 | 方案 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository（无 Hilt，手写 AppContainer） |
| 存储 | Room（闹钟 / 歌单 / 响铃日志）+ DataStore Preferences |
| 网络 | OkHttp + kotlinx.serialization |
| 播放 | Media3 ExoPlayer（USAGE_ALARM） |
| 响铃 | 前台服务 `RingingService` + 全屏 `RingingActivity` |
| 调度 | `AlarmManager.setAlarmClock` |

## 构建

环境要求：JDK 17、Android SDK（`platforms/android-35`、`build-tools/35.0.0`、`platform-tools`），`ANDROID_HOME` 或 `local.properties` 指向 SDK。

```bash
# Windows：生成本地 SDK 路径（按需）
echo "sdk.dir=D\:\\Android\\sdk" > local.properties
# 或 export ANDROID_HOME=/path/to/sdk

./gradlew assembleDebug          # 构建 debug APK
./gradlew testDebugUnitTest      # 单元测试
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 安装

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 使用流程

1. 首次启动完成权限体检（通知 / 精确闹钟 / 全屏通知 / 电池优化）。
2. 进「设置 → API 配置」点「测试连通性」，等待校验通过。
3. 回首页点「+」新建闹钟，选时间、重复、铃声来源与歌单 ID，保存。
4. 启用后响铃时在线解析并流式播放当前曲目（不下载缓存）。

> **歌单 ID 获取**：在音乐软件的歌单内点「分享」→「复制链接」，链接中 `id=` 后面的数字即为歌单 ID。

详见 [docs/USAGE.md](docs/USAGE.md)。

## 权限清单

`INTERNET`、`ACCESS_NETWORK_STATE`、`POST_NOTIFICATIONS`、`SCHEDULE_EXACT_ALARM`、`USE_EXACT_ALARM`、`USE_FULL_SCREEN_INTENT`、`SYSTEM_ALERT_WINDOW`、`WAKE_LOCK`、`RECEIVE_BOOT_COMPLETED`、`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`、`VIBRATE`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`。

## 已知限制

- 自动化验证仅覆盖编译与单元测试；真机行为请在目标机型上自行验证。
- 锁屏 / 后台响铃依赖全屏通知、精确闹钟、显示在其他应用之上等权限；国产 ROM 需允许自启动 / 后台并锁定最近任务，否则强杀进程后可能不响。
- 在线播放依赖响铃时刻的网络；网易云 VIP / 版权曲目解析不出音频时会降级为仅震动。
- Android 14+ 侧载应用的 `USE_FULL_SCREEN_INTENT` 可能被降级为横幅，全屏拉起依赖「显示在其他应用之上」兜底，或把应用设为系统默认闹钟。

## 作者

- 开发者：**Luquiescene**
- 博客：<https://luq-blog.xyz/posts/2026-11-21-custom-alarm>
- GitHub：<https://github.com/Luquiescene>
- 仓库：<https://github.com/Luquiescene/custom-alarm-based-on-meting-api>
- 邮箱：<luquiescene@163.com>

## 开源协议

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

## 致谢

感谢以下开源项目：

- [Kotlin](https://kotlinlang.org/)（JetBrains，Apache-2.0）
- [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io/)（Google，Apache-2.0）
- [AndroidX](https://developer.android.com/jetpack/androidx)（Lifecycle / Navigation / Room / DataStore / Media3，Apache-2.0）
- [OkHttp](https://square.github.io/okhttp/)（Square，Apache-2.0）
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)（JetBrains，Apache-2.0）
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)（JetBrains，Apache-2.0）
- [Meting](https://github.com/metowolf/Meting)（metowolf，MIT）
- [JUnit](https://junit.org/)（EPL-1.0，仅测试）

## 目录结构

```
app/src/main/java/com/example/customalarm/
├── CustomAlarmApp.kt        # Application，初始化 AppContainer
├── alarm/                   # AlarmScheduler / AlarmReceiver / RescheduleReceiver
├── data/
│   ├── local/               # Room 实体、DAO、Database
│   ├── prefs/               # DataStore 设置
│   ├── net/                 # Meting API + 解析 + 能力探测
│   ├── download/            # 音频内容判定（Content-Type）
│   └── repository/          # Alarm / Playlist 仓库
├── di/                      # AppContainer / AppGraph
├── domain/                  # 纯逻辑（可单测）
└── ui/                      # 主题、导航、各页面、全屏响铃页
```
