# MultiSearch Browser 🔎

<p align="center">
  <b>Android + Web 双端多搜索引擎浏览器 · Kotlin/Jetpack + 原生 JS PWA</b>
</p>

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-7F52FF?style=flat-square&logo=kotlin)
![Android](https://img.shields.io/badge/Android-min%2024-3DDC84?style=flat-square&logo=android)
![Hilt](https://img.shields.io/badge/Hilt-2.48.1-24B6EF?style=flat-square)
![Room](https://img.shields.io/badge/Room-2.6.1-6DB33F?style=flat-square)
![PWA](https://img.shields.io/badge/Web-PWA-5A0FC8?style=flat-square)
![Version](https://img.shields.io/badge/version-2.0.0-blue?style=flat-square)

---

## 🌟 功能

### Android 端
- 🔀 **7 个国内搜索引擎一键切换**：百度 / 搜狗 / B站 / 抖音 / 必应 / 豆包 / 千问
- 🗂 **真实多窗口管理**：每窗口独立保留浏览状态，自动回写 URL/标题
- 🔖 **书签收藏**：一键收藏，长按删除
- 📜 **浏览历史**：自动记录，URL UPSERT 不打乱顺序，支持一键清空
- 🧭 **WebView 体验**：进度条、下拉刷新、系统返回键优先退 WebView 历史、下载链接外抛
- 👤 **头像上传**：相册选图，Coil 加载与缓存
- 📡 **RSS 订阅**：订阅源管理 + 文章列表（v2.0.0）
- 📝 **划线笔记**：网页选中文本即存为笔记，JS Bridge 打通 Web↔Native（v2.0.0）
- 🔑 **密码管理**：Android Keystore + AES-GCM 硬件级加密存储（v2.0.0）
- 🐵 **用户脚本**：URL 模式匹配 + 脚本注入管理（v2.0.0）
- 📊 **数据统计**：历史/书签/窗口/笔记/RSS 计数 + Top 站点（v2.0.0）
- 🛡 **安全加固**：Keystore 加密、JS Bridge origin 白名单、禁用备份、第三方 Cookie 拦截（v2.0.0）

### Web 端（PWA）
- ⚡ **多引擎并行搜索**：7 引擎同屏并排，结果聚合过滤
- 🤖 **AI 多源检索研讨面板**：多引擎结果自动汇总
- 📖 **阅读模式**：正文提取 + TTS 朗读 + AI 摘要 + 划词翻译 + 截图/长图/PDF 导出
- 🗂 **多标签页**：右键菜单（关闭其他/右侧/左侧、固定、静音）
- 📊 **历史统计仪表盘**：7 天趋势 + 引擎占比 + 12 周热力图
- 🔖 **书签分组**：文件夹管理 + Chrome HTML 导入导出
- 🎙 **语音搜索** + 🔍 **页内查找** + 📥 **稍后阅读**
- 🎨 **自定义主题色** + 🌙 **深色模式定时** + 🕵 **隐身窗口**
- ⌨️ **全局快捷键**：Ctrl+K 搜索 / Ctrl+D 收藏 / Esc 返回
- 🌐 **i18n**：中文 / English
- 🛡 **广告拦截** + 🐵 **用户脚本** + 📡 **RSS**
- 🔑 **密码管理**：PBKDF2(100000) + AES-256-CBC 加密
- ☁️ **跨设备同步**：WebDAV / GitHub Gist
- 📴 **Service Worker 离线** + 📦 **PWA 安装**

## 🏗 技术栈

### Android

| 层 | 选型 |
|---|---|
| 语言 | Kotlin 1.9.20 |
| 最低 SDK | 24（Android 7.0） |
| 目标 SDK | 34（Android 14） |
| UI | ViewBinding + ConstraintLayout + Material Components |
| 导航 | Navigation Component + Safe Args（单 Activity + 11 Fragment） |
| 持久化 | Room 2.6.1（KSP）+ Migration 1→2 |
| DI | Hilt 2.48.1 + Hilt-Work 1.2.0 |
| 异步 | Coroutines + Flow + StateFlow + repeatOnLifecycle |
| 后台任务 | WorkManager（RSS 定时同步 + 清理） |
| 图片 | Coil 2.5.0 |
| 网络 | OkHttp 4.12.0 |
| 序列化 | Gson 2.10.1 |
| 加密 | Android Keystore + AES-GCM |
| 构建 | Gradle 8.2 + AGP 8.2.0 |

### Web

| 层 | 选型 |
|---|---|
| 语言 | 原生 JavaScript（IIFE + ES Module 混合） |
| 样式 | 原生 CSS + CSS 变量主题系统 |
| 存储 | localStorage（safeStorage 包装） |
| 加密 | CryptoJS 4.2.0（PBKDF2 + AES-256-CBC） |
| 离线 | Service Worker + Cache Storage |
| PWA | manifest.json + Service Worker |
| i18n | 自研 i18n.js（zh-CN / en） |
| CDN | jsdelivr（SRI 校验） |

## 🚀 构建

### Android

需要 JDK 17 与 Android SDK 34。

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（未配置 CI secrets 时复用 debug 签名）
./gradlew assembleRelease

# 单元测试
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

产物路径：
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

### Web

纯静态站点，无需构建。本地预览：

```bash
cd web
python3 -m http.server 8000
# 访问 http://localhost:8000/
```

## 🔒 v2.0.0 安全加固

| 项 | 措施 |
|---|---|
| 备份 | `allowBackup=false` + `dataExtractionRules` 排除数据库/偏好 |
| 明文流量 | `network_security_config` 全面禁止明文 HTTP |
| 密码存储（Android） | Android Keystore + AES-GCM 硬件级加密 |
| 密码存储（Web） | PBKDF2 100000 迭代 + AES-256-CBC + 随机 IV |
| JS Bridge | origin 白名单注入，非信任域不暴露 `window.MSB` |
| WebView | 第三方 Cookie 拦截 |
| Web CSP | `script-src 'self' cdn.jsdelivr.net`，禁止内联脚本 |
| Web SRI | CDN 脚本 integrity 校验 |
| iframe sandbox | 并行搜索 iframe 移除 `allow-same-origin` |
| Room | 显式 Migration(1, 2) 替代 destructive migration |
| Release 签名 | CI 通过 GitHub secrets 注入专用 keystore |

## 📦 CI

GitHub Actions（[`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml)）在 push/PR 时自动：
1. 跑单元测试
2. 跑 lint
3. 构建 Debug + Release APK
4. 上传 artifact
5. push 到 main/master 时自动创建 Release（配置 secrets 时用专用签名，否则 fallback debug）

## 📁 项目结构

```
MultiSearchBrowser/
├── app/src/main/java/com/browser/app/
│   ├── BrowserApplication.kt        # @HiltAndroidApp
│   ├── MainActivity.kt              # 单 Activity + 底部 Tab
│   ├── data/
│   │   ├── BrowserDatabase.kt       # Room + Migration 1→2
│   │   ├── dao/                     # 10 个 DAO
│   │   └── entity/                  # 10 个 Entity
│   ├── di/
│   │   └── AppModule.kt             # Hilt providers
│   ├── repository/                  # 数据仓库层
│   ├── ui/
│   │   ├── home/                    # 首页
│   │   ├── tabs/                    # 多窗口
│   │   ├── webview/                 # WebView + WebAppInterface
│   │   └── profile/                 # 我的 + 历史/书签/RSS/笔记/密码/脚本/统计
│   ├── utils/
│   │   ├── CryptoUtils.kt           # Keystore + AES-GCM
│   │   ├── NavExt.kt                # Safe Args 导航扩展
│   │   └── ...
│   └── work/                        # WorkManager（RSS 同步/清理）
├── app/src/main/res/
│   ├── layout/                      # 20+ 布局
│   ├── navigation/nav_graph.xml     # 11 节点 + Safe Args
│   └── xml/                         # 安全配置 + 备份规则
├── web/
│   ├── index.html                   # PWA 入口
│   ├── app.js                       # 主应用逻辑
│   ├── utils.js                     # MSBUtils 工具库
│   ├── i18n.js                      # 国际化
│   ├── styles.css                   # 样式 + 主题
│   ├── sw.js                        # Service Worker
│   ├── sw-register.js               # SW 注册
│   └── manifest.json                # PWA manifest
└── .github/workflows/
    └── android-ci.yml               # CI
```

## 📝 后续规划

- [ ] version catalog 统一依赖版本
- [ ] detekt + ktlint 代码规范
- [ ] Web 单元测试（utils.js 纯函数）
- [ ] Android Room DAO 测试
- [ ] 统一网络层（Hilt 提供 OkHttpClient 单例）
- [ ] Web CORS 代理多 fallback
