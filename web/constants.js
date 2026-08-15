/**
 * MultiSearch Browser · 常量定义
 * v2.1.0: 从 app.js 抽离，所有模块共享。
 * 依赖：无（纯数据）
 */
(function() {
    'use strict';

    // 7 个内置搜索引擎
    const ENGINES = [
        { id: 'baidu',    name: '百度',  searchUrl: 'https://www.baidu.com/s?wd=',    color: '#2932E1' },
        { id: 'sogou',    name: '搜狗',  searchUrl: 'https://www.sogou.com/web?query=', color: '#FF6600' },
        { id: 'bilibili', name: 'B站',   searchUrl: 'https://search.bilibili.com/all?keyword=', color: '#FB7299' },
        { id: 'douyin',   name: '抖音',  searchUrl: 'https://www.douyin.com/search/', color: '#161823' },
        { id: 'bing',     name: '必应',  searchUrl: 'https://cn.bing.com/search?q=',  color: '#0C8484' },
        { id: 'doubao',   name: '豆包',  searchUrl: 'https://www.doubao.com/search?q=', color: '#4E6EF2' },
        { id: 'qianwen',  name: '千问',  searchUrl: 'https://tongyi.aliyun.com/qianwen?q=', color: '#615CED' },
    ];

    // localStorage 键名集中管理
    const STORAGE_KEYS = {
        engine: 'msb_selected_engine',
        windows: 'msb_windows',
        bookmarks: 'msb_bookmarks',
        history: 'msb_history',
        avatar: 'msb_avatar',
        currentWindowId: 'msb_current_window_id',
        searchHistory: 'msb_search_history',
        theme: 'msb_theme',
        themeColor: 'msb_theme_color',
        parallelEngines: 'msb_parallel_engines',
        laterList: 'msb_later_list',
        bookmarkFolders: 'msb_bookmark_folders',
        notes: 'msb_notes',
        tabs: 'msb_tabs',
        activeTabId: 'msb_active_tab_id',
        scrollProgress: 'msb_scroll_progress',
        customEngines: 'msb_custom_engines',
        darkSchedule: 'msb_dark_schedule',
        rssFeeds: 'msb_rss_feeds',
        rssCache: 'msb_rss_cache',
        passwords: 'msb_passwords_encrypted',
        masterPwdHash: 'msb_master_pwd_hash',
        masterPwdSalt: 'msb_master_pwd_salt',
        syncConfig: 'msb_sync_config',
        llmConfig: 'msb_llm_config',
        aiSummaryMode: 'msb_ai_summary_mode',
        translateMode: 'msb_translate_mode',
        readingSettings: 'msb_reading_settings',
        pwdAutoLock: 'msb_pwd_auto_lock',
        translateCache: 'msb_translate_cache',
        errorLog: 'msb_error_log',
        userScripts: 'msb_user_scripts',
        adBlockEnabled: 'msb_ad_block_enabled',
        rssReadSet: 'msb_rss_read_set',
    };

    // 10 种主题色预设
    const THEME_COLORS = [
        { id: 'blue',   primary: '#2196F3', dark: '#1976D2', header: '#2196F3', headerDark: '#1A237E', label: '经典蓝' },
        { id: 'green',  primary: '#009688', dark: '#00796B', header: '#009688', headerDark: '#004D40', label: '森林绿' },
        { id: 'purple', primary: '#9C27B0', dark: '#7B1FA2', header: '#9C27B0', headerDark: '#4A148C', label: '典雅紫' },
        { id: 'orange', primary: '#FF9800', dark: '#F57C00', header: '#FF9800', headerDark: '#E65100', label: '活力橙' },
        { id: 'red',    primary: '#F44336', dark: '#D32F2F', header: '#F44336', headerDark: '#B71C1C', label: '热情红' },
        { id: 'teal',   primary: '#00BCD4', dark: '#0097A7', header: '#00BCD4', headerDark: '#006064', label: '青碧' },
        { id: 'indigo', primary: '#3F51B5', dark: '#303F9F', header: '#3F51B5', headerDark: '#1A237E', label: '靛蓝' },
        { id: 'pink',   primary: '#E91E63', dark: '#C2185B', header: '#E91E63', headerDark: '#880E4F', label: '樱花粉' },
        { id: 'brown',  primary: '#795548', dark: '#5D4037', header: '#795548', headerDark: '#3E2723', label: '复古棕' },
        { id: 'gray',   primary: '#607D8B', dark: '#455A64', header: '#607D8B', headerDark: '#263238', label: '极简灰' },
    ];

    // 7 引擎的多源研讨视角定义
    const AI_PERSPECTIVES = {
        baidu:    { icon: '🔍', label: '百度综合视角',  desc: q => `综合「${q}」的中文互联网权威资料、百科定义与资讯动态。` },
        sogou:    { icon: '💬', label: '搜狗微信视角',  desc: q => `聚合「${q}」相关的微信公众号深度长文与社交传播内容。` },
        bilibili: { icon: '📺', label: 'B 站视频视角',  desc: q => `提供「${q}」的视频教程、UP 主讲解与互动评论。` },
        douyin:   { icon: '🎵', label: '抖音热点视角',  desc: q => `捕捉「${q}」的短视频热点、用户实拍与趋势话题。` },
        bing:     { icon: '🌐', label: '必应国际视角',  desc: q => `提供「${q}」的国际搜索结果与英文权威资源。` },
        doubao:   { icon: '🤖', label: '豆包 AI 视角',  desc: q => `字节豆包 AI 对「${q}」的智能问答与多模态生成。` },
        qianwen:  { icon: '🧠', label: '千问推理视角',  desc: q => `阿里通义千问对「${q}」的深度推理与代码生成。` },
    };

    // 广告/追踪域名黑名单
    const AD_BLACKLIST = [
        'doubleclick.net', 'googlesyndication.com', 'googletagmanager.com',
        'googletagservices.com', 'google-analytics.com', 'adservice.google.com',
        'facebook.net', 'facebook.com/tr', 'amazon-adsystem.com', 'adsystem.com',
        'criteo.com', 'criteo.net', 'taboola.com', 'outbrain.com', 'adnxs.com',
        'pubmatic.com', 'rubiconproject.com', 'openx.net', 'quantserve.com',
        'scorecardresearch.com', 'hotjar.com', 'mixpanel.com', 'segment.io',
        'adroll.com', 'bing.com/ads', 'baidu.com/cpro', 'cnzz.com', 'umeng.com',
        'talkingdata.com',
    ];

    // 内置示例用户脚本
    const BUILTIN_SCRIPTS = [
        {
            name: '夜间模式',
            pattern: '.*',
            description: '为页面强制应用暗色主题',
            code: `(function() {
    var style = document.createElement('style');
    style.textContent = \`
        html { filter: invert(1) hue-rotate(180deg) !important; }
        img, video, iframe { filter: invert(1) hue-rotate(180deg) !important; }
    \`;
    document.head.appendChild(style);
})();`
        },
        {
            name: '隐藏广告元素',
            pattern: '.*',
            description: '基于 class/id 启发式隐藏广告元素',
            code: `(function() {
    var selectors = ['[class*="ad-"]', '[class*="ads-"]', '[class*="advert"]', '[id*="ad-"]', '[id*="ads-"]', '[id*="advert"]', '.ad', '.ads', '.advertisement', '[class*="banner"]'];
    selectors.forEach(function(s) {
        document.querySelectorAll(s).forEach(function(el) { el.style.display = 'none'; });
    });
})();`
        },
        {
            name: '自动滚动到正文',
            pattern: '.*',
            description: '页面加载后自动滚动到 article 元素',
            code: `(function() {
    var article = document.querySelector('article') || document.querySelector('.article') || document.querySelector('.content');
    if (article) {
        article.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
})();`
        }
    ];

    window.MSBConstants = { ENGINES, STORAGE_KEYS, THEME_COLORS, AI_PERSPECTIVES, AD_BLACKLIST, BUILTIN_SCRIPTS };
})();
