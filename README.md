# MultiSearch Browser 🔎

<p align="center">
  <b>Android 多搜索引擎浏览器 · Kotlin + Jetpack</b>
</p>

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-7F52FF?style=flat-square&logo=kotlin)
![Android](https://img.shields.io/badge/Android-min%2024-3DDC84?style=flat-square&logo=android)
![Jetpack](https://img.shields.io/badge/Jetpack-Navigation%20%7C%20Room%20%7C%20ViewBinding-4285F4?style=flat-square)

---

## 🌟 功能

- 🔀 **7 个国内搜索引擎一键切换**：百度 / 搜狗 / B站 / 抖音 / 必应 / 豆包 / 千问
- 🗂 **真实多窗口管理**：每个窗口独立保留浏览状态，浏览过程自动回写当前 URL/标题
- 🔖 **书签收藏**：一键收藏，长按删除
- 📜 **浏览历史**：自动记录，URL UPSERT 不打乱顺序，支持一键清空
- 🧭 **WebView 体验**：进度条、下拉刷新、系统返回键优先退 WebView 历史、下载链接外抛、JS 弹窗处理
- 👤 **头像上传**：相册选图，Coil 加载与缓存
- 🛡 **安全**：network_security_config 默认拒绝明文，混合内容兼容模式

## 🏗 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin 1.9.20 |
| 最低 SDK | 24（Android 7.0） |
| 目标 SDK | 34（Android 14） |
| UI | ViewBinding + ConstraintLayout + Material Components |
| 导航 | Navigation Component (single activity + 6 fragments) |
| 持久化 | Room 2.6.1（KSP 注解处理） |
| 异步 | Coroutines + Flow + repeatOnLifecycle |
| 图片 | Coil 2.5.0 |
| 网络 | OkHttp 4.12.0（保留作为后续 API 客户端） |
| 序列化 | Gson 2.10.1 |
| 构建 | Gradle 8.2 + AGP 8.2.0 |

## 🚀 构建

需要 JDK 17 与 Android SDK 34。

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（临时复用 debug 签名，便于 CI 产物可安装）
./gradlew assembleRelease

# 单元测试
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

产物路径：
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

## 📦 CI

GitHub Actions（[`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml)）在 push/PR 时自动：
1. 跑单元测试
2. 跑 lint
3. 构建 Debug + Release APK
4. 上传 artifact
5. push 到 main/master 时自动创建 Release

## 📁 项目结构

```
app/src/main/java/com/browser/app/
├── BrowserApplication.kt        # Application，初始化 Database
├── MainActivity.kt              # 单 Activity + 底部 Tab
├── data/
│   ├── BrowserDatabase.kt       # Room 数据库
│   ├── dao/                     # BookmarkDao / HistoryDao / WindowDao
│   └── entity/                  # BookmarkEntity / HistoryEntity / WindowEntity
├── repository/                  # 数据仓库层
├── ui/
│   ├── home/                    # 首页：搜索引擎选择 + 搜索
│   ├── tabs/                    # 多窗口管理
│   ├── webview/                 # WebView 浏览页
│   └── profile/                 # 我的：头像/历史/书签/设置
└── utils/
    ├── NavExt.kt                # 全局导航扩展
    ├── PreferenceManager.kt     # SharedPreferences 封装
    └── SearchEngine.kt          # 搜索引擎定义
```

## 📝 后续规划

- [ ] 引入 Hilt 替代手动 DI
- [ ] 引入 ViewModel + ViewModelFactory，让数据存活于配置变更
- [ ] 多窗口切换时保留 WebView 状态（目前每次进入重新加载）
- [ ] 设置页：搜索引擎默认值、首页快捷入口编辑、清除缓存
- [ ] 暗黑主题
- [ ] 端到端测试
