# 多搜索引擎浏览器

一个功能完整的 Android 浏览器应用，支持多个搜索引擎切换。

## 功能特性

### 首页
- 支持 7 种搜索引擎切换：百度、搜狗、B站、抖音、必应、豆包、千问
- 搜索框输入后可直接跳转到搜索结果
- 记住上次选择的搜索引擎

### 浏览器功能
- 完整的网页浏览体验
- 下拉刷新
- 网址栏输入
- 收藏网页功能

### 底部导航
1. **首页** - 搜索引擎选择和搜索入口
2. **多窗口** - 管理多个浏览窗口
3. **返回** - 返回上一页
4. **我的** - 个人中心

### 我的页面
- **浏览历史** - 查看和管理浏览记录
- **收藏内容** - 查看和管理收藏的网页
- **上传头像** - 选择并设置个人头像

## 技术栈

- Kotlin
- AndroidX
- Jetpack Navigation
- Room Database
- Material Design Components
- WebView
- Coroutines & Flow

## 构建项目

```bash
./gradlew assembleDebug
```

## 生成签名 APK

```bash
./gradlew assembleRelease
```

## 项目结构

```
app/
├── src/main/
│   ├── java/com/browser/app/
│   │   ├── data/           # 数据层
│   │   │   ├── dao/        # Room DAO
│   │   │   └── entity/     # 数据实体
│   │   ├── repository/     # 数据仓库
│   │   ├── ui/             # UI 层
│   │   │   ├── home/       # 首页
│   │   │   ├── webview/    # 网页浏览
│   │   │   ├── tabs/       # 多窗口
│   │   │   └── profile/    # 我的页面
│   │   └── utils/          # 工具类
│   └── res/                # 资源文件
└── build.gradle.kts
```

## License

MIT License