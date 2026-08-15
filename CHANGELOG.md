# Changelog

本项目所有重要变更均会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [2.1.0] - 2026-08-13

### Added
- 🔗 **双端数据互通**：Android WebView JS Bridge 打通 Room，Web 页面可直接读写原生书签 / 历史 / 笔记 / RSS / 窗口 / stats（P0-4）
- 📴 **真正离线可用**：Service Worker 升级为 stale-while-revalidate 策略，新增离线搜索回退页
- 🤖 **LLM API 接入**：通义千问 / DeepSeek / 豆包 / Custom 多 provider，支持 AI 摘要与对话
- 💾 **WebView 状态保留**：Fragment 重建不再丢失浏览历史，状态自动恢复
- ☁️ **跨设备同步实装**：WebDAV / GitHub Gist 真实双向同步 + 冲突合并策略
- 🔑 **密码自动填充**：Android Autofill 服务 + Web Credential API 联动
- 🧪 **测试覆盖率提升**：新增 DAO / Repository / ViewModel / Bridge 单元测试（P0-3）

### Changed
- 统一全端版本号为 2.1.0（build.gradle.kts versionCode=3 / sw.js CACHE_VERSION / WebAppInterface.getAppInfo / README badge / 设置页显示版本）
- Web 主应用 `app.js` 按模块边界保守拆分（P0-2），保留单 IIFE 结构并补充模块索引注释

## [2.0.0] - 2026-08-12

### Added
- 📡 **RSS 订阅**：订阅源管理 + 文章列表 + WorkManager 定时同步（批次 2）
- 📝 **划线笔记**：网页选中文本即存为笔记，JS Bridge 打通 Web↔Native（批次 2）
- 🔑 **密码管理（Android）**：Android Keystore + AES-GCM 硬件级加密存储（批次 2）
- 🐵 **用户脚本**：URL 模式匹配 + 脚本注入管理（批次 2）
- 📊 **数据统计**：历史 / 书签 / 窗口 / 笔记 / RSS 计数 + Top 站点（批次 2）
- detekt 静态代码分析集成（批次 4）
- version catalog 统一依赖版本（批次 4）
- ESLint Web 端代码规范（批次 4）
- Room `exportSchema = true`，导出 schema 用于 Migration 验证（批次 4）
- GitHub Actions Web CI（批次 4）
- Dependabot 依赖更新机器人（批次 4）

### Changed
- ProGuard 规则完善，release 混淆配置加固（批次 3）
- WebAppInterface a11y 可访问性改进（批次 3）
- save 复用 safeStorage 统一处理 QuotaExceededError（批次 3）
- README 全面更新，补充项目结构与安全加固说明（批次 3）
- `app.js` 模块边界索引注释（批次 3）
- 媒体查询响应式优化（批次 4）
- CORS 代理多 fallback，避免单一代理宕机（批次 4）
- SEO meta 完善：Open Graph / Twitter Card / JSON-LD（批次 4）

### Security
- **批次 1 安全加固（8 项）**：
  - `allowBackup=false` + `dataExtractionRules` 排除数据库 / 偏好
  - `network_security_config` 全面禁止明文 HTTP 流量
  - Android Keystore + AES-GCM 硬件级密码加密
  - JS Bridge origin 白名单注入，非信任域不暴露 `window.MSB`
  - WebView 第三方 Cookie 拦截
  - Web CSP：`script-src 'self' cdn.jsdelivr.net`，禁止内联脚本
  - CDN 脚本 SRI integrity 校验
  - 并行搜索 iframe 移除 `allow-same-origin`
- Room 显式 Migration(1, 2) 替代 destructive migration
- Release 签名：CI 通过 GitHub secrets 注入专用 keystore，未配置时 fallback debug

## [1.9.0] - 2026-08-12

### Added
- Room 2.6.1（KSP）持久化层 + 10 个 DAO / 10 个 Entity
- Hilt 2.48.1 依赖注入 + Hilt-Work 1.2.0
- WorkManager 后台任务（RSS 定时同步 + 清理）
- Android WebView 集成：进度条、下拉刷新、系统返回键优先退 WebView 历史、下载链接外抛

## [1.8.0] - 2026-08-12

### Added
- 🌐 i18n 国际化：自研 i18n.js，支持中文（zh-CN）/ English（en）
- 🎨 主题系统：自定义主题色 + 深色模式定时
- ⚡ 性能优化：缓存策略与渲染优化

## [1.7.0] - 2026-08-12

### Added
- PWA 增强：manifest.json + Service Worker 完善
- 协议处理器（Protocol Handler）注册
- 对话式摘要面板
- 词云可视化

## [1.6.0] - 2026-08-12

### Added
- 🐵 用户脚本注入管理
- 🛡 广告拦截：Service Worker 域名黑名单（40+ 追踪 / 广告域名）
- 📥 长图 / PDF 导出
- 📝 笔记导出
- 📡 RSS 与笔记 / 阅读模式联动

## [1.5.0] - 2026-08-12

### Added
- 体验深化：交互细节打磨
- 工程化升级：CI 测试 / lint 集成

## [1.4.0] - 2026-08-12

### Added
- P2 六项进阶方向落地（多引擎聚合、过滤等）

## [1.3.1] - 2026-08-12

### Fixed
- P1 七项体验优化

## [1.3.0] - 2026-08-12

### Added
- P0 五项功能增强

## [1.1.0] - 2026-08-12

### Added
- 7 项功能增强（多引擎并行搜索、结果聚合过滤等）

## [1.0.0] - 2026-03-26

### Added
- Android 多搜索引擎浏览器初始版本：7 个国内搜索引擎一键切换
- AI Multi-Engine Summarizer 面板
- Web Simulator 静态站点
- 真实多窗口管理：每窗口独立保留浏览状态，URL / 搜索区分，历史 UPSERT
- 浏览历史 + 书签收藏
- GitHub Actions CI：单元测试 / lint / Debug + Release APK 构建 / artifact 上传
- 安全配置：明文流量禁止、release 混淆

### Fixed
- P1 崩溃与 UX 缺陷修复（导航 / WebView / 书签 / 历史）
- 构建阻塞修复：Room KSP、wrapper jar、release 签名
- 剪贴板 writeText 异常 fallback
