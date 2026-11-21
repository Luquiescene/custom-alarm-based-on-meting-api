# 定制闹钟：一个用歌单做铃声的原生 Android 闹钟

> 把网易云 / QQ 音乐 / 酷狗的歌单变成你的起床铃声——在线流式播放、锁屏全屏响铃、熄屏点亮、贪睡与曲目轮播，全部原生化实现。

- 项目仓库：<https://github.com/Luquiescene/custom-alarm-based-on-meting-api>
- 开源协议：[Apache License 2.0](https://github.com/Luquiescene/custom-alarm-based-on-meting-api/blob/main/LICENSE)
- 作者：Luquiescene
- 技术栈：Kotlin 2.x + Jetpack Compose（Material 3）、MVVM、Room、DataStore、OkHttp、Media3 ExoPlayer

---

## 为什么做这个

市面上大多数闹钟只能选系统铃声或本地音乐文件。我想做一个能用**在线歌单**当铃声的闹钟：选一个喜欢的歌单，每次响铃时随机或按顺序播一首，起床也能每天换口味。

这个项目的定位是一个**技术向的完整 Android 应用**，从零手写，不依赖 Hilt 等重量级框架，适合用来学习 Jetpack Compose、前台服务、精确闹钟、媒体播放、权限适配等工程实践。

---

## 核心功能

- **歌单铃声**：支持网易云 / QQ 音乐 / 酷狗歌单，也支持自定义音频直链和自定义 Meting 模板。
- **在线播放，零缓存**：响铃时通过 Meting API 在线解析真实音频地址并流式播放，不下载、不缓存音频文件。
- **锁屏全屏响铃**：锁屏 / 息屏状态下点亮屏幕并弹出全屏响铃页，前台服务保证后台 / 进程被清理时仍能响铃。
- **贪睡与曲目轮播**：可配置贪睡间隔与次数；响铃曲目按 `nextIndex` 轮播，下次自动换下一首。
- **能力探测与校验**：内置 Meting 端点连通性探测，展示各平台“能力矩阵”，校验通过前闹钟不启用（闸门策略）。
- **响铃日志**：记录每次响铃的曲目、播放结果与错误，方便排查“只震动不响”之类的问题。
- **深浅色主题**：跟随系统 / 深色 / 浅色，DataStore 持久化。

---

## 技术选型

| 模块 | 方案 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository（手写 `AppContainer` 依赖注入，无 Hilt） |
| 存储 | Room（闹钟 / 歌单 / 日志）+ DataStore Preferences |
| 网络 | OkHttp + kotlinx.serialization（手动解析 JSON 数组） |
| 播放 | Media3 ExoPlayer（`USAGE_ALARM`） |
| 响铃 | 前台服务 `RingingService` + 全屏 `RingingActivity` |
| 调度 | `AlarmManager.setAlarmClock`（精确闹钟） |

---

## 架构与响铃链路

应用采用单模块 `app` + 手写依赖注入，整体分层：

```
ui（Compose 页面 / 全屏响铃页）
  → viewmodel（状态 + 业务调度）
  → repository（Alarm / Playlist）
  → data（Room / DataStore / Meting API / 能力探测）
  → domain（纯逻辑，可单测）
```

响铃时走的是一条“三保险”链路，核心思路是**前台服务负责持续出声，UI 只负责展示**：

1. `AlarmScheduler` 用 `setAlarmClock` 注册精确闹钟，到点触发 `AlarmReceiver`。
2. `AlarmReceiver` 先拉起全屏 `RingingActivity`，再发全屏通知，最后启动前台服务 `RingingService`。
3. `RingingService` 获取屏幕唤醒锁（`SCREEN_BRIGHT_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP`，点亮熄屏），从 Room 读取闹钟与曲目，经 Meting `type=url` 解析真实音频地址，交给 Media3 流式播放，同时震动、自动停止 / 贪睡、写入响铃日志。

这样设计的好处是：即使全屏页被系统拦截，前台服务仍能持续响铃；即使服务被厂商省电策略延迟，全屏页与通知也提供了兜底入口。

---

## 使用方式

1. 首次启动完成权限体检（通知 / 精确闹钟 / 全屏通知 / 显示在其他应用之上 / 电池优化）。
2. 进「设置 → API 配置」点「测试连通性」，等待校验通过。
3. 回首页点「+」新建闹钟，选时间、重复、铃声来源与歌单 ID，保存并启用。

> **歌单 ID 获取**：在音乐软件的歌单内点「分享」→「复制链接」，链接中 `id=` 后面的数字即为歌单 ID。

---

## 几个值得记录的坑

1. **“只震动不响”的真相**：Media3 在 `USAGE_ALARM` 下若开启自动音频焦点会直接抛 `IllegalStateException`（只有 `USAGE_MEDIA/USAGE_GAME` 支持自动焦点），异常被吞掉后就表现为只震动。解决方法是 `setAudioAttributes(attrs, handleAudioFocus = false)`。

2. **Android 14+ 侧载应用的全屏通知降级**：`USE_FULL_SCREEN_INTENT` 只对 Play 上声明为“闹钟 / 通话”的应用默认授权，侧载包即使手动打开也可能被降级为横幅。这里的兜底是“显示在其他应用之上”权限 + 直接 `startActivity` 拉起全屏页。

3. **熄屏不亮屏**：`PARTIAL_WAKE_LOCK` 只保 CPU 不亮屏，必须用 `SCREEN_BRIGHT_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP`（等价于已废弃的 `FULL_WAKE_LOCK`）才能把屏幕点亮。

4. **国产 ROM 的省电策略**：即便用 `setAlarmClock`（后台启动前台服务的官方豁免），小米 / 华为等仍可能延迟广播或强杀进程，需要在系统管家里允许自启动、后台，并在最近任务里把应用上锁。

---

## 已知限制

- 在线播放依赖响铃时刻的网络；无网络或 VIP / 版权曲目解析失败时会降级为**仅震动**。
- 公共 Meting 端点随时可能变动（如 `i-meto` 已失效），建议自建 Meting 实例或使用自定义直链。
- 自动化验证覆盖编译与单元测试（22 项），真机行为请在目标机型上自行验证。

---

## 开源地址

代码、构建说明与使用文档都在 GitHub，欢迎 Star、提 Issue、一起改进：

👉 **<https://github.com/Luquiescene/custom-alarm-based-on-meting-api>**

本项目采用 **Apache License 2.0** 协议开源。
