/**
 * MultiSearch Browser · Web 版
 * 纯前端实现，localStorage 持久化数据，无后端依赖。
 * v2.0.0：安全加固 / a11y / safeStorage 复用 / 模块边界索引
 *
 * 模块边界索引（v2.0.0 保守拆分策略：保留单 IIFE，按以下边界未来可物理拆分为 ES Module）：
 *   - utils     L9-294   工具函数 + 常量（已委派 save/load 到 MSBUtils.safeStorage）
 *   - store     L295-683 数据访问层（localStorage CRUD）
 *   - theme     L684-706 主题（深色 + 主题色）
 *   - router    L707-737 路由
 *   - home      L738-856 首页 + 搜索历史联想
 *   - webview   L857-1332 WebView + 多标签页 + 字号 + 页内查找
 *   - reading   L1333-1615 阅读模式 + TTS
 *   - ai        L1616-2044 AI 摘要 + 划词翻译 + 截图
 *   - rss       L2045-2198 RSS 订阅
 *   - passwords L2199-2401 密码管理
 *   - security  L2402-2530 安全加固 + 错误监控
 *   - userscript L2531-2770 用户脚本 + 长图/PDF + 笔记导出
 *   - sync      L2771-2982 跨设备同步 + LLM 配置
 *   - windows   L2983-3117 多窗口
 *   - history   L3075-3238 历史 + 书签
 *   - profile   L3239-3714 我的 + 设置 + 暗黑定时 + 隐身 + 书签 HTML 互通 + 导入导出
 *   - nav       L3715-3814 底部导航 + AI 多源检索面板
 *   - parallel  L3815-3975 多引擎并行搜索 + 稍后阅读 + 划线笔记
 *   - stats     L3976-4431 聚合搜索 + 统计仪表盘 + 热力图 + 语音搜索 + 快捷键
 *   - init      L4432-    初始化入口
 */
(function() {
    'use strict';

    // ============ 工具库别名（v1.5.0 起逐步委派到 window.MSBUtils） ============
    const U = window.MSBUtils || {};
    // v2.1.0: 从 constants.js / app-store.js 引入抽离的常量和数据层
    const { ENGINES, STORAGE_KEYS, THEME_COLORS, AI_PERSPECTIVES, AD_BLACKLIST, BUILTIN_SCRIPTS } = window.MSBConstants;
    const store = window.MSBStore;
    // v2.1.0: 跨设备同步模块已抽离到 app-sync.js
    const { syncNow, syncWebDAV, syncGist, mergeSyncData, applySyncData, exportDataRaw } = window.MSBSync;
    // v2.1.0: AI/LLM 模块（从 app-ai.js 引入）
    const { generateSummary, toggleTranslateMode, performTranslate, showLlmConfig, isTranslateActive } = window.MSBAi;
    // v1.8.0: i18n
    const i18n = window.MSBi18n;
    function t(key, fb) { return i18n ? i18n.t(key, fb) : (fb || key); }

    // ============ v1.7.0: PWA 启动处理 ============
    function handlePwaLaunch() {
        const url = new URL(window.location.href);
        const params = url.searchParams;

        // 1. share_target：接收其他 App 分享的文本/url
        const sharedTitle = params.get('title');
        const sharedText = params.get('text');
        const sharedUrl = params.get('url');
        if (sharedTitle || sharedText || sharedUrl) {
            const query = sharedUrl || sharedText || sharedTitle || '';
            if (query) {
                // 自动填入搜索框并跳转首页
                setTimeout(() => {
                    const input = $('#search-input');
                    if (input) {
                        input.value = query;
                        navigate('home');
                        showToast('📥 已接收分享内容');
                    }
                }, 300);
            }
            // 清理 URL 参数（避免刷新重复触发）
            url.searchParams.delete('title');
            url.searchParams.delete('text');
            url.searchParams.delete('url');
            window.history.replaceState({}, '', url.toString());
            return;
        }

        // 2. shortcuts：通过 ?page=xxx 进入
        const page = params.get('page');
        if (page) {
            const validPages = ['home', 'parallel', 'history', 'rss', 'bookmarks', 'later', 'notes', 'passwords', 'stats', 'settings', 'profile'];
            if (validPages.includes(page)) {
                setTimeout(() => navigate(page), 100);
            }
            url.searchParams.delete('page');
            window.history.replaceState({}, '', url.toString());
            return;
        }

        // 3. 协议处理器：web+msb://search?q=xxx → ?msb_query=xxx
        const msbQuery = params.get('msb_query');
        if (msbQuery) {
            setTimeout(() => {
                const input = $('#search-input');
                if (input) {
                    input.value = msbQuery;
                    navigate('home');
                    showToast(`🔗 协议调用：${msbQuery}`);
                }
            }, 300);
            url.searchParams.delete('msb_query');
            window.history.replaceState({}, '', url.toString());
        }
    }

    // v1.7.0: 注册自定义协议 web+msb://
    function registerCustomProtocol() {
        if (navigator.registerProtocolHandler) {
            try {
                const baseUrl = new URL('./index.html?msb_query=%s', window.location.href).href;
                navigator.registerProtocolHandler('web+msb', baseUrl, 'MultiSearch 搜索');
            } catch (e) {
                // 部分浏览器要求 https，忽略错误
                console.warn('[MSB] 协议注册失败:', e.message);
            }
        }
    }

    // v1.7.0: Launch Queue API（处理 file_handlers 等）
    function setupLaunchQueue() {
        if ('launchQueue' in window) {
            window.launchQueue.setConsumer((launchParams) => {
                // 处理文件
                if (launchParams.files && launchParams.files.length > 0) {
                    handleLaunchedFiles(launchParams.files);
                }
            });
        }
    }

    // v1.7.0: File Handling API（PWA 桌面端右键打开文件）
    function setupFileHandler() {
        // 此处仅做能力检测，实际文件通过 launchQueue 处理
        if ('launchQueue' in window) {
            console.log('[MSB] File Handler 已就绪');
        }
    }

    async function handleLaunchedFiles(files) {
        for (const fileHandle of files) {
            try {
                const file = await fileHandle.getFile();
                const ext = file.name.split('.').pop().toLowerCase();
                if (ext === 'json') {
                    // 导入数据备份
                    importData(file);
                    showToast(`📂 已导入文件：${file.name}`);
                } else if (ext === 'html' || ext === 'htm') {
                    // 导入书签 HTML
                    importBookmarksHtml(file);
                    showToast(`📂 已导入书签：${file.name}`);
                } else if (ext === 'md') {
                    // 在阅读模式中打开 Markdown
                    const text = await file.text();
                    const content = $('#reading-content');
                    if (content) {
                        content.innerHTML = renderMarkdown(text);
                        $('#reading-title').textContent = file.name;
                        $('#reading-mode').hidden = false;
                        navigate('webview');
                        showToast(`📄 已打开：${file.name}`);
                    }
                }
            } catch (e) {
                showToast(`❌ 打开文件失败：${e.message}`);
            }
        }
    }

    // v1.7.0: 简易 Markdown 渲染（用于打开 .md 文件）
    function renderMarkdown(text) {
        let html = escapeHtml(text);
        // 标题
        html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
        html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
        html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');
        // 加粗、斜体
        html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
        // 代码块
        html = html.replace(/```([\s\S]+?)```/g, '<pre>$1</pre>');
        html = html.replace(/`(.+?)`/g, '<code>$1</code>');
        // 引用
        html = html.replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>');
        // 列表
        html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
        html = html.replace(/(<li>.+<\/li>\n?)+/g, (m) => '<ul>' + m + '</ul>');
        // 链接
        html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
        // 段落（双换行）
        html = html.replace(/\n\n/g, '</p><p>');
        html = '<p>' + html + '</p>';
        return html;
    }

    let lastAiQuery = '';
    let currentFontSize = 16; // WebView 字号

    // ============ 工具函数 ============
    const $ = (sel) => document.querySelector(sel);
    const $$ = (sel) => document.querySelectorAll(sel);

    // v2.0.0: 复用 MSBUtils.safeStorage，统一处理 QuotaExceededError 与 JSON 序列化
    const load = (key, def = null) => U.safeStorage.get(key, def);
    const save = (key, val) => U.safeStorage.set(key, val);

    // v2.0.0: CORS 代理多 fallback，避免单一代理宕机时 RSS/聚合搜索全部失效
    const CORS_PROXIES = [
        'https://corsproxy.io/?url=',
        'https://api.allorigins.win/raw?url=',
        'https://api.codetabs.com/v1/proxy/?quest=',
    ];
    async function fetchWithCorsFallback(targetUrl, options) {
        for (const proxy of CORS_PROXIES) {
            try {
                const res = await fetch(proxy + encodeURIComponent(targetUrl), options);
                if (res.ok) return res;
            } catch {}
        }
        // 最后兜底：直接请求（CORS 可能失败但不阻塞）
        return fetch(targetUrl, options);
    }

    const formatTime = (ts) => {
        const d = new Date(ts);
        const pad = (n) => String(n).padStart(2, '0');
        return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    };

    const showToast = (msg) => {
        const t = $('#toast');
        t.textContent = msg;
        t.hidden = false;
        clearTimeout(showToast._t);
        showToast._t = setTimeout(() => { t.hidden = true; }, 1800);
    };

    const confirmDialog = (title, message) => new Promise((resolve) => {
        const modal = $('#modal');
        $('#modal-title').textContent = title;
        $('#modal-message').textContent = message;
        modal.hidden = false;
        const ok = $('#modal-ok'), cancel = $('#modal-cancel');
        const cleanup = () => { modal.hidden = true; ok.onclick = null; cancel.onclick = null; };
        ok.onclick = () => { cleanup(); resolve(true); };
        cancel.onclick = () => { cleanup(); resolve(false); };
    });

    const inputDialog = (title, defaultValue = '', autocomplete = 'off') => new Promise((resolve) => {
        const modal = $('#input-modal');
        $('#input-modal-title').textContent = title;
        const field = $('#input-modal-field');
        field.value = defaultValue;
        // 根据输入类型动态设置 autocomplete，让浏览器/系统提供对应的自动填充建议
        field.setAttribute('autocomplete', autocomplete);
        modal.hidden = false;
        setTimeout(() => { field.focus(); field.select(); }, 50);
        const ok = $('#input-modal-ok'), cancel = $('#input-modal-cancel');
        const cleanup = () => { modal.hidden = true; ok.onclick = null; cancel.onclick = null; field.onkeydown = null; };
        ok.onclick = () => { cleanup(); resolve(field.value.trim()); };
        cancel.onclick = () => { cleanup(); resolve(null); };
        field.onkeydown = (e) => {
            if (e.key === 'Enter') { cleanup(); resolve(field.value.trim()); }
            if (e.key === 'Escape') { cleanup(); resolve(null); }
        };
    });

    // v2.1.0: store 已抽离到 app-store.js，此处通过 window.MSBStore 引用
    // v2.1.0: 常量已抽离到 constants.js

    // ============ Credential Management API 渐进增强 ============
    // 浏览器不支持时静默降级，不影响现有密码管理功能
    const credApi = {
        // 保存凭据到浏览器凭据存储（与本地加密存储独立，互不影响）
        async store(site, username, password) {
            try {
                if (!navigator.credentials || !window.PasswordCredential) return;
                const cred = new PasswordCredential({
                    id: username || site,
                    password: password,
                    name: site
                });
                await navigator.credentials.store(cred);
            } catch (e) {
                // Credential API 不支持时静默降级
            }
        },
        // 从浏览器凭据存储静默获取凭据（用于触发浏览器自动填充联想）
        async get() {
            try {
                if (!navigator.credentials) return null;
                const cred = await navigator.credentials.get({
                    password: true,
                    mediation: 'silent'
                });
                return cred;
            } catch (e) {
                // Credential API 不支持时静默降级
                return null;
            }
        }
    };

    // 获取所有引擎（内置 + 自定义）
    function getAllEngines() {
        return [...ENGINES, ...store.getCustomEngines()];
    }

    // ============ 主题（深色 + 主题色） ============
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        const toggle = $('#dark-mode-toggle');
        if (toggle) toggle.checked = (theme === 'dark');
        applyThemeColor(store.getThemeColor());
        const meta = document.querySelector('meta[name="theme-color"]');
        if (meta) meta.content = theme === 'dark' ? getComputedStyle(document.documentElement).getPropertyValue('--header-bg').trim() || '#1A237E' : '#2196F3';
    }

    function applyThemeColor(colorId) {
        const c = THEME_COLORS.find(t => t.id === colorId) || THEME_COLORS[0];
        const root = document.documentElement;
        const isDark = root.getAttribute('data-theme') === 'dark';
        root.style.setProperty('--primary', c.primary);
        root.style.setProperty('--primary-dark', c.dark);
        root.style.setProperty('--header-bg', isDark ? c.headerDark : c.header);
    }

    function initTheme() {
        applyTheme(store.getTheme());
    }

    // ============ 路由 ============
    const routes = ['home', 'parallel', 'aggregated', 'windows', 'webview', 'profile', 'history', 'bookmarks', 'later', 'notes', 'rss', 'passwords', 'stats', 'settings'];
    let currentRoute = 'home';
    const routeStack = ['home'];

    function navigate(route, push = true) {
        if (!routes.includes(route)) return;
        if (push && route !== currentRoute) routeStack.push(route);
        currentRoute = route;
        routes.forEach(r => $(`#page-${r}`).classList.toggle('active', r === route));
        const tabMap = { home: 'home', windows: 'windows', profile: 'profile' };
        $$('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tabMap[route]));
        if (route === 'windows') renderWindows();
        if (route === 'history') { renderHistory(); $('#history-filter').value = ''; }
        if (route === 'bookmarks') { renderBookmarks(); $('#bookmarks-filter').value = ''; }
        if (route === 'later') renderLater();
        if (route === 'notes') renderNotes();
        if (route === 'rss') renderRss();
        if (route === 'passwords') renderPasswords();
        if (route === 'stats') renderStats();
        if (route === 'profile') renderProfile();
        if (route === 'settings') renderSettings();
        if (route === 'parallel') renderParallelPage();
        if (route === 'aggregated') renderAggregated();
        window.scrollTo(0, 0);
    }

    function navigateBack() {
        if (routeStack.length > 1) { routeStack.pop(); navigate(routeStack[routeStack.length - 1], false); }
    }

    // ============ 首页 ============
    function renderHome() {
        const selected = store.getSelectedEngine();
        const allEngines = getAllEngines();
        const chipsEl = $('#engine-chips');
        chipsEl.innerHTML = '';
        allEngines.forEach(engine => {
            const chip = document.createElement('button');
            chip.className = 'chip' + (engine.id === selected ? ' active' : '');
            chip.textContent = engine.name;
            chip.onclick = () => {
                store.setSelectedEngine(engine.id);
                renderHome();
            };
            chipsEl.appendChild(chip);
        });

        const quickEl = $('#quick-links');
        quickEl.innerHTML = '';
        allEngines.forEach(engine => {
            const link = document.createElement('div');
            link.className = 'quick-link';
            link.innerHTML = `
                <div class="ql-icon" style="background:${engine.color}">${engine.name[0]}</div>
                <div class="ql-name">${engine.name}</div>
            `;
            link.onclick = () => {
                store.setSelectedEngine(engine.id);
                renderHome();
                openWebview(engine.searchUrl, -1);
            };
            quickEl.appendChild(link);
        });

        // 并行搜索入口
        const parallelBtn = $('#parallel-search-btn');
        if (parallelBtn) {
            parallelBtn.onclick = () => {
                const q = $('#search-input').value.trim();
                if (q) $('#parallel-input').value = q;
                navigate('parallel');
            };
        }
    }

    function handleSearch() {
        const input = $('#search-input');
        const query = input.value.trim();
        if (!query) {
            showToast('请输入搜索内容');
            return;
        }
        // 离线检测：网络不可用时先查本地历史记录
        if (navigator.onLine === false) {
            const history = store.getSearchHistory();
            const matched = history.filter(q => q.toLowerCase().includes(query.toLowerCase()));
            if (matched.length > 0) {
                // 本地历史有匹配，显示离线结果提示
                showToast('离线结果：已从本地历史记录中找到 ' + matched.length + ' 条匹配项');
            } else {
                showToast('离线模式：本地历史无匹配，可能无法获取新结果');
            }
        }
        // 记录搜索历史
        store.addSearchHistory(query);
        // 更新 AI 多源研讨面板
        renderAiSummary(query);
        const allEngines = getAllEngines();
        const engine = allEngines.find(e => e.id === store.getSelectedEngine()) || allEngines[0];
        const url = engine.searchUrl + encodeURIComponent(query);
        openWebview(url, -1);
    }

    // ============ 搜索历史联想 ============
    function setupSearchSuggest() {
        const input = $('#search-input');
        const suggest = $('#search-suggest');

        input.addEventListener('focus', () => renderSearchSuggest());
        input.addEventListener('input', () => renderSearchSuggest(input.value.trim()));
        // 延迟隐藏，避免点击建议项时先失焦
        input.addEventListener('blur', () => {
            setTimeout(() => { suggest.hidden = true; }, 200);
        });

        function renderSearchSuggest(filter = '') {
            const list = store.getSearchHistory();
            const filtered = filter
                ? list.filter(q => q.toLowerCase().includes(filter.toLowerCase()))
                : list;
            if (filtered.length === 0 && !filter) {
                suggest.hidden = true;
                return;
            }
            suggest.innerHTML = '';
            filtered.slice(0, 10).forEach(q => {
                const item = document.createElement('div');
                item.className = 'suggest-item';
                item.innerHTML = `
                    <span class="si-icon">🕐</span>
                    <span class="si-text">${escapeHtml(q)}</span>
                    <span class="si-delete" data-q="${escapeHtml(q)}">✕</span>
                `;
                item.querySelector('.si-text').onclick = () => {
                    input.value = q;
                    suggest.hidden = true;
                    handleSearch();
                };
                item.querySelector('.si-delete').onclick = (e) => {
                    e.stopPropagation();
                    store.deleteSearchHistory(q);
                    renderSearchSuggest(filter);
                };
                suggest.appendChild(item);
            });
            if (filtered.length > 0) {
                const footer = document.createElement('div');
                footer.className = 'suggest-footer';
                footer.textContent = '清空搜索历史';
                footer.onclick = () => {
                    store.clearSearchHistory();
                    suggest.hidden = true;
                    showToast('已清空搜索历史');
                };
                suggest.appendChild(footer);
            }
            suggest.hidden = false;
        }
    }

    // ============ WebView（v1.3.0 多标签页） ============
    let currentWebview = { url: '', title: '', windowId: -1, tabId: null };
    let frameLoadTimeout = null;
    let scrollSaveTimer = null;

    function openWebview(url, windowId = -1) {
        // 取或建活跃 tab
        let tabId = store.getActiveTabId();
        const tabs = store.getTabs();
        const existing = tabs.find(t => t.id === tabId);
        if (!existing) {
            const tab = store.addTab(url, '');
            tabId = tab.id;
        } else {
            // 同一 tab 切换 url
            store.updateTab(tabId, { url, title: '' });
        }
        store.setActiveTabId(tabId);

        currentWebview = { url, title: '', windowId, tabId };
        $('#wv-url').value = url;
        $('#wv-fallback').hidden = true;
        $('#wv-frame').src = url;
        $('#wv-progress').hidden = false;

        clearTimeout(frameLoadTimeout);
        frameLoadTimeout = setTimeout(() => {
            try {
                const frame = $('#wv-frame');
                if (frame.contentDocument === null) {
                    showWebviewFallback(url);
                }
            } catch {
                showWebviewFallback(url);
            }
        }, 3000);

        updateBookmarkIcon();
        renderTabs();
        navigate('webview');
    }

    function showWebviewFallback(url) {
        $('#wv-progress').hidden = true;
        $('#wv-fallback').hidden = false;
        $('#wv-fallback-link').href = url;
    }

    function setupWebview() {
        const frame = $('#wv-frame');
        frame.addEventListener('load', () => {
            $('#wv-progress').hidden = true;
            let title = '';
            try { title = frame.contentDocument.title || ''; } catch { title = ''; }
            currentWebview.title = title;
            // 隐身模式不记录历史
            if (currentWebview.url && !currentWebview.incognito) {
                store.addHistory(title || currentWebview.url, currentWebview.url);
            }
            // 更新 tab 元数据
            if (currentWebview.tabId) {
                store.updateTab(currentWebview.tabId, {
                    url: currentWebview.url,
                    title: title || currentWebview.url,
                });
                renderTabs();
            }
            if (currentWebview.windowId >= 0) {
                store.updateWindow(currentWebview.windowId, {
                    url: currentWebview.url,
                    title: title || currentWebview.url,
                });
            }
            // 恢复阅读进度（隐身模式跳过）
            if (!currentWebview.incognito) {
                try {
                    const doc = frame.contentDocument;
                    if (doc && currentWebview.url) {
                        const saved = store.getScrollProgress(currentWebview.url);
                        if (saved > 0 && saved < 0.99) {
                            const max = doc.documentElement.scrollHeight - frame.clientHeight;
                            if (max > 0) {
                                frame.contentWindow.scrollTo(0, saved * max);
                                showToast(`已恢复到 ${Math.round(saved * 100)}%`);
                            }
                        }
                    }
                } catch {}
            }
            // v1.6.0: 注入用户脚本（仅同源可访问，跨域会抛错被 try/catch 兜底）
            injectUserScripts(frame, currentWebview.url);
        });

        // 阅读进度监听（节流保存，隐身模式跳过）
        frame.addEventListener('load', () => {
            if (currentWebview.incognito) return;
            try {
                const cw = frame.contentWindow;
                if (!cw) return;
                cw.addEventListener('scroll', () => {
                    if (!currentWebview.url) return;
                    clearTimeout(scrollSaveTimer);
                    scrollSaveTimer = setTimeout(() => {
                        const max = cw.document.documentElement.scrollHeight - cw.innerHeight;
                        if (max <= 0) return;
                        const progress = cw.scrollY / max;
                        store.setScrollProgress(currentWebview.url, progress);
                        const ind = $('#wv-scroll-ind');
                        if (ind) ind.textContent = Math.round(progress * 100) + '%';
                    }, 300);
                }, { passive: true });
            } catch {}
        });

        $('#wv-back').onclick = () => {
            if (frame.contentWindow && frame.contentWindow.history.length > 1) {
                try { frame.contentWindow.history.back(); return; } catch {}
            }
            navigateBack();
        };

        $('#wv-url').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const input = $('#wv-url').value.trim();
                if (input) {
                    openWebview(normalizeInput(input), currentWebview.windowId);
                }
            }
        });

        $('#wv-refresh').onclick = () => {
            $('#wv-fallback').hidden = true;
            $('#wv-progress').hidden = false;
            frame.src = currentWebview.url;
            clearTimeout(frameLoadTimeout);
            frameLoadTimeout = setTimeout(() => showWebviewFallback(currentWebview.url), 3000);
        };

        $('#wv-open-external').onclick = () => {
            if (currentWebview.url) window.open(currentWebview.url, '_blank', 'noopener');
        };

        $('#wv-bookmark').onclick = () => {
            const added = store.toggleBookmark(
                currentWebview.title || currentWebview.url,
                currentWebview.url
            );
            updateBookmarkIcon();
            showToast(added ? '已添加收藏' : '已取消收藏');
        };

        // 稍后阅读
        $('#wv-later').onclick = () => {
            const list = store.getLaterList();
            const exists = list.some(l => l.url === currentWebview.url);
            if (exists) {
                showToast('已在稍后阅读列表中');
            } else {
                store.addLater(currentWebview.title || currentWebview.url, currentWebview.url);
                showToast('📥 已加入稍后阅读');
            }
        };

        // 阅读模式
        $('#wv-read').onclick = () => enterReadingMode();

        // 字号调节
        $('#wv-font-dec').onclick = () => adjustWebviewFont(-1);
        $('#wv-font-inc').onclick = () => adjustWebviewFont(1);

        // 页内查找
        $('#wv-find').onclick = () => {
            const bar = $('#wv-find-bar');
            bar.hidden = !bar.hidden;
            if (!bar.hidden) { $('#wv-find-input').focus(); $('#wv-find-count').textContent = ''; }
        };

        // 新标签按钮
        $('#tab-new').onclick = () => {
            const engine = ENGINES.find(e => e.id === store.getSelectedEngine()) || ENGINES[0];
            openWebview(engine.searchUrl, -1);
        };
    }

    // ============ 多标签页 ============
    function renderTabs() {
        const listEl = $('#tabs-list');
        if (!listEl) return;
        const tabs = store.getTabs();
        const activeId = store.getActiveTabId();
        listEl.innerHTML = '';
        // pinned 排前面，保持相对顺序
        const sorted = [...tabs].sort((a, b) => {
            if (!!a.pinned !== !!b.pinned) return a.pinned ? -1 : 1;
            return 0;
        });
        // 若顺序变化则持久化
        if (sorted.some((t, i) => t.id !== tabs[i].id)) {
            store.saveTabs(sorted);
        }
        sorted.forEach((tab, idx) => {
            const chip = document.createElement('div');
            const classes = ['tab-chip'];
            if (tab.id === activeId) classes.push('active');
            if (tab.incognito) classes.push('incognito');
            if (tab.pinned) classes.push('pinned');
            if (tab.muted) classes.push('muted');
            chip.className = classes.join(' ');
            chip.dataset.tabId = tab.id;
            chip.draggable = true;
            const prefix = tab.incognito ? '🛡 ' : '';
            const title = tab.title || tab.url || '新标签';
            const shortTitle = tab.pinned ? '' : (title.length > 16 ? title.slice(0, 16) + '…' : title);
            chip.innerHTML = `
                <span class="tc-title">${escapeHtml(prefix + shortTitle)}</span>
                <button class="tc-close" aria-label="关闭">✕</button>
            `;
            chip.onclick = () => switchTab(tab.id);
            chip.querySelector('.tc-close').onclick = (e) => {
                e.stopPropagation();
                closeTab(tab.id);
            };
            // 右键菜单
            chip.addEventListener('contextmenu', (e) => {
                e.preventDefault();
                showTabContextMenu(e, tab);
            });
            // 拖拽排序
            chip.addEventListener('dragstart', (e) => {
                e.dataTransfer.setData('text/tab-id', String(tab.id));
                chip.classList.add('dragging');
            });
            chip.addEventListener('dragend', () => {
                chip.classList.remove('dragging');
                listEl.querySelectorAll('.tab-chip').forEach(c => c.classList.remove('drag-over'));
            });
            chip.addEventListener('dragover', (e) => {
                e.preventDefault();
                chip.classList.add('drag-over');
            });
            chip.addEventListener('dragleave', () => {
                chip.classList.remove('drag-over');
            });
            chip.addEventListener('drop', (e) => {
                e.preventDefault();
                const fromId = Number(e.dataTransfer.getData('text/tab-id'));
                const toId = tab.id;
                if (fromId && fromId !== toId) reorderTabs(fromId, toId);
            });
            listEl.appendChild(chip);
        });
    }

    function reorderTabs(fromId, toId) {
        const tabs = store.getTabs();
        const fromIdx = tabs.findIndex(t => t.id === fromId);
        const toIdx = tabs.findIndex(t => t.id === toId);
        if (fromIdx < 0 || toIdx < 0) return;
        const [moved] = tabs.splice(fromIdx, 1);
        tabs.splice(toIdx, 0, moved);
        store.saveTabs(tabs);
        renderTabs();
    }

    function showTabContextMenu(e, tab) {
        const menu = $('#tab-context-menu');
        menu.hidden = false;
        menu.style.left = Math.min(e.clientX, window.innerWidth - 180) + 'px';
        menu.style.top = Math.min(e.clientY, window.innerHeight - 280) + 'px';
        menu.dataset.tabId = tab.id;
        // 点击菜单项
        const handler = (ev) => {
            const btn = ev.target.closest('button[data-action]');
            if (!btn) return;
            const action = btn.dataset.action;
            const targetTabId = Number(menu.dataset.tabId);
            handleTabContextAction(action, targetTabId);
            menu.hidden = true;
            document.removeEventListener('click', handler);
        };
        setTimeout(() => document.addEventListener('click', handler), 0);
    }

    function handleTabContextAction(action, tabId) {
        const tabs = store.getTabs();
        const tab = tabs.find(t => t.id === tabId);
        if (!tab) return;
        const idx = tabs.findIndex(t => t.id === tabId);
        switch (action) {
            case 'close':
                closeTab(tabId);
                break;
            case 'close-others':
                tabs.filter(t => t.id !== tabId).forEach(t => store.deleteTab(t.id));
                switchTab(tabId);
                break;
            case 'close-right':
                tabs.slice(idx + 1).forEach(t => store.deleteTab(t.id));
                if (store.getActiveTabId() !== tabId) switchTab(tabId);
                else renderTabs();
                break;
            case 'close-left':
                tabs.slice(0, idx).forEach(t => store.deleteTab(t.id));
                if (store.getActiveTabId() !== tabId) switchTab(tabId);
                else renderTabs();
                break;
            case 'new-adjacent': {
                const list = store.getTabs();
                const newTab = { id: Date.now(), url: '', title: '新标签', timestamp: Date.now() };
                list.splice(idx + 1, 0, newTab);
                store.saveTabs(list);
                switchTab(newTab.id);
                $('#wv-url').focus();
                break;
            }
            case 'toggle-pin':
                store.updateTab(tabId, { pinned: !tab.pinned });
                renderTabs();
                showToast(tab.pinned ? '已取消固定' : '📌 已固定');
                break;
            case 'toggle-mute':
                store.updateTab(tabId, { muted: !tab.muted });
                renderTabs();
                showToast(tab.muted ? '已取消静音' : '🔇 已静音');
                break;
        }
    }

    function switchTab(tabId) {
        const tabs = store.getTabs();
        const tab = tabs.find(t => t.id === tabId);
        if (!tab) return;
        store.setActiveTabId(tabId);
        currentWebview = {
            url: tab.url,
            title: tab.title || '',
            windowId: -1,
            tabId,
            incognito: !!tab.incognito
        };
        $('#wv-url').value = tab.url;
        $('#wv-fallback').hidden = true;
        $('#wv-frame').src = tab.url;
        $('#wv-progress').hidden = false;
        // 隐身 toolbar 标识
        const tb = $('.webview-toolbar');
        if (tb) tb.classList.toggle('incognito', !!tab.incognito);
        clearTimeout(frameLoadTimeout);
        frameLoadTimeout = setTimeout(() => showWebviewFallback(tab.url), 3000);
        updateBookmarkIcon();
        renderTabs();
        navigate('webview');
    }

    function closeTab(tabId) {
        const tabs = store.getTabs();
        const idx = tabs.findIndex(t => t.id === tabId);
        if (idx < 0) return;
        store.deleteTab(tabId);
        const remaining = store.getTabs();
        if (remaining.length === 0) {
            // 关闭后没 tab，回到首页
            store.setActiveTabId(null);
            navigate('home');
            return;
        }
        // 如果关的是活跃 tab，切到相邻
        if (store.getActiveTabId() === tabId) {
            const next = remaining[Math.min(idx, remaining.length - 1)];
            switchTab(next.id);
        } else {
            renderTabs();
        }
        showToast('标签已关闭');
    }

    // ============ 字号调节 ============
    function adjustWebviewFont(delta) {
        currentFontSize = Math.max(12, Math.min(24, currentFontSize + delta));
        try {
            const doc = $('#wv-frame').contentDocument;
            if (doc && doc.body) {
                doc.body.style.fontSize = currentFontSize + 'px';
                showToast(`字号：${currentFontSize}px`);
            } else {
                showToast('当前页面无法调节字号');
            }
        } catch {
            showToast('跨域页面无法调节字号');
        }
    }

    // ============ 页内查找 ============
    let findMatches = [];
    let findIndex = -1;

    function setupFindInPage() {
        const input = $('#wv-find-input');
        input.addEventListener('input', () => performFind(input.value.trim()));
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') { e.preventDefault(); jumpFind(e.shiftKey ? -1 : 1); }
        });
        $('#wv-find-prev').onclick = () => jumpFind(-1);
        $('#wv-find-next').onclick = () => jumpFind(1);
        $('#wv-find-close').onclick = () => {
            $('#wv-find-bar').hidden = true;
            clearFindHighlight();
            findMatches = []; findIndex = -1;
        };
    }

    function performFind(query) {
        clearFindHighlight();
        findMatches = []; findIndex = -1;
        if (!query) { $('#wv-find-count').textContent = ''; return; }
        try {
            const doc = $('#wv-frame').contentDocument;
            if (!doc || !doc.body) return;
            const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT, {
                acceptNode: (node) => {
                    if (!node.nodeValue.trim()) return NodeFilter.FILTER_REJECT;
                    const p = node.parentElement;
                    if (!p) return NodeFilter.FILTER_REJECT;
                    const tag = p.tagName;
                    if (['SCRIPT', 'STYLE', 'NOSCRIPT'].includes(tag)) return NodeFilter.FILTER_REJECT;
                    return NodeFilter.FILTER_ACCEPT;
                }
            });
            const qLower = query.toLowerCase();
            const nodes = [];
            let n;
            while ((n = walker.nextNode())) nodes.push(n);
            nodes.forEach(node => {
                const text = node.nodeValue;
                const lower = text.toLowerCase();
                let i = 0;
                while ((i = lower.indexOf(qLower, i)) !== -1) {
                    const range = doc.createRange();
                    range.setStart(node, i);
                    range.setEnd(node, i + query.length);
                    const span = doc.createElement('span');
                    span.className = 'msb-find-mark';
                    span.style.background = '#FFEB3B';
                    span.style.color = '#000';
                    range.surroundContents(span);
                    findMatches.push(span);
                    i += query.length;
                }
            });
            if (findMatches.length > 0) {
                findIndex = 0;
                highlightCurrentFind();
            }
            $('#wv-find-count').textContent = findMatches.length > 0
                ? `${findIndex + 1}/${findMatches.length}`
                : '0/0';
        } catch {
            $('#wv-find-count').textContent = '跨域无法查找';
        }
    }

    function highlightCurrentFind() {
        findMatches.forEach((s, i) => {
            s.style.background = (i === findIndex) ? '#FF9800' : '#FFEB3B';
        });
        if (findMatches[findIndex]) {
            findMatches[findIndex].scrollIntoView({ block: 'center', behavior: 'smooth' });
        }
    }

    function jumpFind(delta) {
        if (findMatches.length === 0) return;
        findIndex = (findIndex + delta + findMatches.length) % findMatches.length;
        highlightCurrentFind();
        $('#wv-find-count').textContent = `${findIndex + 1}/${findMatches.length}`;
    }

    function clearFindHighlight() {
        try {
            const doc = $('#wv-frame').contentDocument;
            if (!doc) return;
            doc.querySelectorAll('span.msb-find-mark').forEach(span => {
                const parent = span.parentNode;
                while (span.firstChild) parent.insertBefore(span.firstChild, span);
                parent.removeChild(span);
                parent.normalize();
            });
        } catch {}
    }

    // ============ 阅读模式 ============
    let readingFontSize = 18;
    let ttsUtterance = null;
    let ttsPlaying = false;
    let autoScrollTimer = null;
    let readingSettingsOpen = false;

    function enterReadingMode() {
        let article = null;
        try {
            const doc = $('#wv-frame').contentDocument;
            if (doc) {
                article = doc.querySelector('article') || doc.querySelector('.article') || doc.querySelector('.content');
                if (!article) {
                    // 启发式：选文本最多的元素
                    const candidates = doc.querySelectorAll('div, main, section');
                    let maxLen = 0;
                    candidates.forEach(el => {
                        const len = (el.innerText || '').length;
                        if (len > maxLen && len < 20000) { maxLen = len; article = el; }
                    });
                }
            }
        } catch {}

        const overlay = $('#reading-mode');
        const content = $('#reading-content');
        const title = $('#reading-title');
        title.textContent = currentWebview.title || '阅读模式';
        if (article) {
            // 清理脚本/样式/广告
            article = article.cloneNode(true);
            // v1.6.0: 扩展广告元素过滤
            const adSelectors = [
                'script', 'style', 'iframe', 'nav', 'header', 'footer', 'aside',
                '.ad', '.ads', '.advertisement', '[class*="ad-"]', '[class*="ads-"]',
                '[class*="advert"]', '[id*="ad-"]', '[id*="ads-"]', '[id*="advert"]',
                '[class*="banner"]', '[class*="popup"]', '[class*="promo"]',
                '.sidebar', '.recommend', '.related', '.comments', '.share',
                'ins', 'embed'
            ];
            article.querySelectorAll(adSelectors.join(',')).forEach(el => el.remove());
            content.innerHTML = '';
            content.appendChild(article);
        } else {
            content.innerHTML = `<p style="text-align:center;color:var(--text-secondary);padding:48px 0">⚠️ 该页面无法提取正文（跨域限制或无 article 结构）<br><br>建议直接在原页面阅读。</p>`;
        }
        content.style.fontSize = readingFontSize + 'px';
        applyReadingSettings(content);
        overlay.hidden = false;
        // 重置 TTS / 自动滚动
        stopTts();
        stopAutoScroll();

        // 还原此 URL 已有的笔记高亮
        restoreNoteMarks(content);

        $('#reading-close').onclick = () => { stopTts(); stopAutoScroll(); overlay.hidden = true; };
        $('#reading-font-dec').onclick = () => {
            readingFontSize = Math.max(14, readingFontSize - 2);
            content.style.fontSize = readingFontSize + 'px';
        };
        $('#reading-font-inc').onclick = () => {
            readingFontSize = Math.min(28, readingFontSize + 2);
            content.style.fontSize = readingFontSize + 'px';
        };
        $('#reading-tts').onclick = () => {
            if (ttsPlaying) stopTts();
            else startTts(content);
        };
        $('#reading-summary').onclick = () => generateSummary(content);
        $('#reading-translate').onclick = () => toggleTranslateMode(content);
        $('#reading-screenshot').onclick = () => captureScreenshot(content);
        $('#reading-auto-scroll').onclick = () => toggleAutoScroll(content);
        $('#reading-settings').onclick = () => toggleReadingSettingsPanel();
        $('#reading-full-screenshot').onclick = () => captureFullScreenshot(content);
        $('#reading-pdf').onclick = () => exportPdf(content);
        setupReadingSettingsPanel(content);

        // 划词监听
        content.onmouseup = (e) => {
            const sel = content.ownerDocument.getSelection();
            const text = sel ? sel.toString().trim() : '';
            const pop = $('#note-popover');
            // 翻译模式下，选词即翻译
            if (isTranslateActive() && text.length > 0) {
                performTranslate(text, e);
                pop.hidden = true;
                return;
            }
            if (text.length > 0) {
                const range = sel.getRangeAt(0);
                const rect = range.getBoundingClientRect();
                pop.style.left = Math.max(8, rect.left + rect.width / 2 - 60) + 'px';
                pop.style.top = (rect.top - 40) + 'px';
                pop.hidden = false;
                pop.dataset.text = text;
            } else {
                pop.hidden = true;
            }
        };
    }

    // ============ v1.5.0: 阅读模式深化 ============
    function applyReadingSettings(content) {
        const s = store.getReadingSettings();
        content.dataset.font = s.fontFamily;
        content.dataset.rtheme = s.theme;
        content.style.setProperty('--reading-line-height', s.lineHeight);
        content.style.setProperty('--reading-para-gap', s.paraGap + 'px');
        content.style.lineHeight = s.lineHeight;
    }

    function toggleReadingSettingsPanel() {
        const panel = $('#reading-settings-panel');
        readingSettingsOpen = !readingSettingsOpen;
        panel.hidden = !readingSettingsOpen;
    }

    function setupReadingSettingsPanel(content) {
        const s = store.getReadingSettings();
        const panel = $('#reading-settings-panel');
        // 初始化 UI 值
        $('#reading-font-family').value = s.fontFamily;
        $('#reading-line-height').value = s.lineHeight;
        $('#reading-line-height-val').textContent = s.lineHeight;
        $('#reading-para-gap').value = s.paraGap;
        $('#reading-para-gap-val').textContent = s.paraGap;
        $('#reading-scroll-speed').value = s.scrollSpeed;
        const speedLabels = ['', '极慢', '慢', '中', '快', '极快'];
        $('#reading-scroll-speed-val').textContent = speedLabels[s.scrollSpeed];
        // 主题高亮
        panel.querySelectorAll('.rsp-theme-chip').forEach(chip => {
            chip.classList.toggle('active', chip.dataset.theme === s.theme);
        });

        // 事件绑定
        $('#reading-font-family').onchange = (e) => {
            s.fontFamily = e.target.value;
            store.setReadingSettings(s);
            content.dataset.font = s.fontFamily;
        };
        $('#reading-line-height').oninput = (e) => {
            s.lineHeight = parseFloat(e.target.value);
            store.setReadingSettings(s);
            content.style.setProperty('--reading-line-height', s.lineHeight);
            content.style.lineHeight = s.lineHeight;
            $('#reading-line-height-val').textContent = s.lineHeight;
        };
        $('#reading-para-gap').oninput = (e) => {
            s.paraGap = parseInt(e.target.value, 10);
            store.setReadingSettings(s);
            content.style.setProperty('--reading-para-gap', s.paraGap + 'px');
            $('#reading-para-gap-val').textContent = s.paraGap;
        };
        $('#reading-scroll-speed').oninput = (e) => {
            s.scrollSpeed = parseInt(e.target.value, 10);
            store.setReadingSettings(s);
            $('#reading-scroll-speed-val').textContent = speedLabels[s.scrollSpeed];
            // 如果正在自动滚动，重启以应用新速度
            if (autoScrollTimer) {
                stopAutoScroll();
                startAutoScroll(content);
            }
        };
        panel.querySelectorAll('.rsp-theme-chip').forEach(chip => {
            chip.onclick = () => {
                s.theme = chip.dataset.theme;
                store.setReadingSettings(s);
                content.dataset.rtheme = s.theme;
                panel.querySelectorAll('.rsp-theme-chip').forEach(c => c.classList.toggle('active', c === chip));
            };
        });
        // 点击面板外关闭
        document.addEventListener('mousedown', function closePanel(e) {
            if (!readingSettingsOpen) return;
            if (!panel.contains(e.target) && e.target.id !== 'reading-settings') {
                readingSettingsOpen = false;
                panel.hidden = true;
                document.removeEventListener('mousedown', closePanel);
            }
        });
    }

    function toggleAutoScroll(content) {
        if (autoScrollTimer) {
            stopAutoScroll();
        } else {
            startAutoScroll(content);
        }
    }

    function startAutoScroll(content) {
        const s = store.getReadingSettings();
        // 速度档 1-5 对应间隔 100/70/50/30/20 ms，每帧 1px
        const intervals = [0, 100, 70, 50, 30, 20];
        const interval = intervals[s.scrollSpeed] || 50;
        const btn = $('#reading-auto-scroll');
        btn.classList.add('auto-scroll-active');
        autoScrollTimer = setInterval(() => {
            content.scrollTop += 1;
            // 到底自动停止
            if (content.scrollTop + content.clientHeight >= content.scrollHeight - 2) {
                stopAutoScroll();
                showToast('已滚动到底部');
            }
        }, interval);
    }

    function stopAutoScroll() {
        if (autoScrollTimer) {
            clearInterval(autoScrollTimer);
            autoScrollTimer = null;
            const btn = $('#reading-auto-scroll');
            if (btn) btn.classList.remove('auto-scroll-active');
        }
    }

    // ============ TTS 朗读 ============
    function startTts(container) {
        if (!('speechSynthesis' in window)) {
            showToast('当前浏览器不支持语音合成');
            return;
        }
        const text = (container.innerText || '').trim();
        if (!text) {
            showToast('没有可朗读的内容');
            return;
        }
        // 截断至 30000 字符避免过长
        const truncated = text.length > 30000 ? text.slice(0, 30000) + '...' : text;
        const utter = new SpeechSynthesisUtterance(truncated);
        utter.lang = 'zh-CN';
        utter.rate = 1;
        utter.pitch = 1;
        const voices = window.speechSynthesis.getVoices();
        const zhVoice = voices.find(v => v.lang.startsWith('zh'));
        if (zhVoice) utter.voice = zhVoice;
        utter.onstart = () => {
            ttsPlaying = true;
            const btn = $('#reading-tts');
            if (btn) btn.classList.add('reading-tts-active');
            showToast('🔊 开始朗读');
        };
        utter.onend = () => {
            ttsPlaying = false;
            const btn = $('#reading-tts');
            if (btn) btn.classList.remove('reading-tts-active');
            showToast('朗读结束');
        };
        utter.onerror = () => {
            ttsPlaying = false;
            const btn = $('#reading-tts');
            if (btn) btn.classList.remove('reading-tts-active');
            showToast('朗读出错');
        };
        ttsUtterance = utter;
        window.speechSynthesis.speak(utter);
    }

    function stopTts() {
        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel();
        }
        ttsPlaying = false;
        const btn = $('#reading-tts');
        if (btn) btn.classList.remove('reading-tts-active');
    }

    // ============ v1.4.0: 截图分享 ============
    async function captureScreenshot(content) {
        if (typeof html2canvas === 'undefined') {
            showToast('截图库未加载');
            return;
        }
        showToast('📸 正在生成截图...');
        try {
            const canvas = await html2canvas(content, {
                backgroundColor: getComputedStyle(document.documentElement).getPropertyValue('--background').trim() || '#FFFFFF',
                scale: 2,
                useCORS: true,
                logging: false
            });
            canvas.toBlob((blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `screenshot-${new Date().toISOString().slice(0, 10)}.png`;
                a.click();
                URL.revokeObjectURL(url);
                showToast('✅ 截图已下载');
            }, 'image/png');
        } catch (err) {
            showToast('截图失败：' + err.message);
        }
    }

    // ============ v1.4.0: RSS 订阅 ============
    function renderRss(filter = '') {
        const items = store.getRssCache();
        const feeds = store.getRssFeeds();
        const readSet = store.getRssReadSet();
        const filtered = filter
            ? items.filter(i => (i.title + i.description).toLowerCase().includes(filter.toLowerCase()))
            : items;
        const listEl = $('#rss-list');
        const empty = $('#rss-empty');
        if (!listEl) return;
        if (filtered.length === 0) {
            listEl.innerHTML = '';
            if (empty) empty.hidden = false;
            return;
        }
        if (empty) empty.hidden = true;
        listEl.innerHTML = '';
        filtered.slice(0, 100).forEach(item => {
            const card = document.createElement('div');
            const isRead = readSet.has(item.guid || item.link);
            card.className = 'rss-item' + (isRead ? ' rss-read' : '');
            const desc = (item.description || '').replace(/<[^>]+>/g, '').slice(0, 120);
            card.innerHTML = `
                <div class="ri-row">
                    <span class="ri-source">${escapeHtml(item.source || 'RSS')}</span>
                    <button class="ri-later-btn" aria-label="加入稍后阅读" title="加入稍后阅读">📥</button>
                </div>
                <div class="ri-title">${escapeHtml(item.title)}</div>
                <div class="ri-desc">${escapeHtml(desc)}</div>
                <div class="ri-time">${item.pubDate ? formatTime(new Date(item.pubDate).getTime()) : ''}${isRead ? ' · 已读' : ''}</div>
            `;
            card.onclick = () => {
                if (item.link) {
                    openWebview(item.link, -1);
                    // v1.6.0: 标记已读
                    store.markRssRead(item.guid || item.link);
                    renderRss(filter);
                }
            };
            // 加入稍后阅读
            card.querySelector('.ri-later-btn').onclick = (e) => {
                e.stopPropagation();
                if (item.link) {
                    store.addLater(item.title, item.link, item.source || 'RSS');
                    store.markRssRead(item.guid || item.link);
                    showToast('📥 已加入稍后阅读');
                    renderRss(filter);
                }
            };
            listEl.appendChild(card);
        });
    }

    function setupRssPage() {
        $('#rss-filter').addEventListener('input', (e) => {
            renderRss(e.target.value.trim());
        });
        $('#rss-add-btn').onclick = async() => {
            const name = await inputDialog('订阅源名称', '');
            if (!name) return;
            const url = await inputDialog('RSS URL', '');
            if (!url) return;
            store.addRssFeed(name, url);
            showToast('已添加，正在抓取...');
            await refreshRss();
            renderRss();
        };
        // v1.6.0: 添加刷新按钮（复用 add-btn 旁边）
        // 通过长按或双击 add-btn 触发刷新（简化：在 page header 增加刷新语义）
        // 此处保持简单：每次进入 RSS 页面自动刷新
    }

    async function refreshRss() {
        const feeds = store.getRssFeeds();
        if (feeds.length === 0) return;
        const all = [];
        for (const feed of feeds) {
            try {
                const items = await fetchRssFeed(feed);
                all.push(...items);
            } catch (e) {
                console.error('RSS 抓取失败', feed.url, e);
            }
        }
        // 按时间倒序
        all.sort((a, b) => {
            const ta = a.pubDate ? new Date(a.pubDate).getTime() : 0;
            const tb = b.pubDate ? new Date(b.pubDate).getTime() : 0;
            return tb - ta;
        });
        store.setRssCache(all);
    }

    async function fetchRssFeed(feed) {
        const res = await fetchWithCorsFallback(feed.url);
        const text = await res.text();
        const parser = new DOMParser();
        const doc = parser.parseFromString(text, 'text/xml');
        const items = [];
        // RSS 2.0
        doc.querySelectorAll('item').forEach(item => {
            items.push({
                title: item.querySelector('title')?.textContent || '',
                link: item.querySelector('link')?.textContent || '',
                description: item.querySelector('description')?.textContent || '',
                pubDate: item.querySelector('pubDate')?.textContent || '',
                source: feed.name
            });
        });
        // Atom
        if (items.length === 0) {
            doc.querySelectorAll('entry').forEach(entry => {
                items.push({
                    title: entry.querySelector('title')?.textContent || '',
                    link: entry.querySelector('link')?.getAttribute('href') || '',
                    description: entry.querySelector('summary')?.textContent || entry.querySelector('content')?.textContent || '',
                    pubDate: entry.querySelector('published')?.textContent || entry.querySelector('updated')?.textContent || '',
                    source: feed.name
                });
            });
        }
        return items;
    }

    // ============ v1.4.0: 密码管理 ============
    let masterPwdSession = null; // 内存中保留，关闭页面失效
    let lastPwdActivity = 0; // v1.5.0: 自动锁屏时间戳
    let autoLockTimer = null;

    function renderPasswords(filter = '') {
        const listEl = $('#pwd-list');
        const empty = $('#pwd-empty');
        if (!listEl) return;
        // 未设置主密码
        if (!store.getMasterPwdHash()) {
            listEl.innerHTML = '';
            if (empty) {
                empty.hidden = false;
                empty.innerHTML = '🔒 请先在设置中设置主密码';
            }
            return;
        }
        // 未解锁
        if (!masterPwdSession) {
            listEl.innerHTML = '';
            if (empty) {
                empty.hidden = false;
                empty.innerHTML = '🔒 请输入主密码解锁<br><button id="pwd-unlock-btn" class="btn-primary" style="margin-top:12px">解锁</button>';
                $('#pwd-unlock-btn').onclick = () => showPwdUnlock();
            }
            return;
        }
        const list = store.loadPasswords(masterPwdSession) || [];
        const filtered = filter
            ? list.filter(p => (p.site + p.username).toLowerCase().includes(filter.toLowerCase()))
            : list;
        if (filtered.length === 0) {
            listEl.innerHTML = '';
            if (empty) { empty.hidden = false; empty.innerHTML = '暂无保存的密码<br>点击右上角添加'; }
            return;
        }
        if (empty) empty.hidden = true;
        listEl.innerHTML = '';
        filtered.forEach(p => {
            const card = document.createElement('div');
            card.className = 'pwd-item';
            const firstChar = (p.site || '?')[0].toUpperCase();
            card.innerHTML = `
                <div class="pi-icon">${firstChar}</div>
                <div class="pi-body">
                    <div class="pi-site">${escapeHtml(p.site)}</div>
                    <div class="pi-user">${escapeHtml(p.username || '')}</div>
                </div>
                <div class="pi-actions">
                    <button class="pi-btn" data-act="copy" aria-label="复制密码">📋</button>
                    <button class="pi-btn" data-act="edit" aria-label="编辑">✏</button>
                    <button class="pi-btn danger" data-act="del" aria-label="删除">✕</button>
                </div>
            `;
            card.querySelector('[data-act="copy"]').onclick = (e) => {
                e.stopPropagation();
                touchPwdActivity();
                navigator.clipboard.writeText(p.password).then(() => {
                    showToast('📋 密码已复制（30 秒后自动清空剪贴板）');
                    // v1.5.0: 30 秒后清空剪贴板
                    setTimeout(() => {
                        navigator.clipboard.writeText('').catch(() => {});
                        showToast('🧹 剪贴板已清空');
                    }, 30000);
                }).catch(() => {
                    showToast('❌ 无法访问剪贴板');
                });
            };
            card.querySelector('[data-act="edit"]').onclick = (e) => {
                e.stopPropagation();
                editPassword(p);
            };
            card.querySelector('[data-act="del"]').onclick = async(e) => {
                e.stopPropagation();
                const ok = await confirmDialog('删除密码', `确定要删除「${p.site}」的密码吗？`);
                if (ok) {
                    const newList = list.filter(x => x.id !== p.id);
                    store.savePasswords(newList, masterPwdSession);
                    renderPasswords(filter);
                }
            };
            listEl.appendChild(card);
        });
    }

    function setupPasswordsPage() {
        $('#pwd-filter').addEventListener('input', (e) => {
            renderPasswords(e.target.value.trim());
        });
        $('#pwd-add-btn').onclick = () => {
            if (!store.getMasterPwdHash()) {
                showToast('请先在设置中设置主密码');
                return;
            }
            if (!masterPwdSession) {
                showPwdUnlock(() => editPassword(null));
                return;
            }
            editPassword(null);
        };
        // 渐进增强：页面加载时尝试从浏览器凭据存储获取已保存的凭据
        // 触发浏览器/系统的自动填充联想，Credential API 不支持时静默降级
        // 注意：不自动合并到应用密码库，避免与本地加密存储产生重复
        credApi.get().then(cred => {
            if (cred) {
                // 浏览器凭据存储中存在凭据，可用于后续表单自动填充
                // 此处仅触发凭据预取，不修改应用内的密码列表
            }
        }).catch(() => {
            // 静默降级
        });
    }

    function editPassword(existing) {
        const site = existing?.site || '';
        const username = existing?.username || '';
        const password = existing?.password || '';
        const modal = $('#pwd-unlock-modal');
        $('#pwd-unlock-title').textContent = existing ? '编辑密码' : '添加密码';
        // 临时改造 modal 为多字段
        const inputEl = $('#pwd-unlock-input');
        // 用 prompt 链式输入更简单
        (async() => {
            // 为各输入字段设置合适的 autocomplete，让浏览器/系统提供自动填充建议
            const newSite = await inputDialog('站点名', site, 'off');
            if (newSite === null) return;
            const newUser = await inputDialog('用户名', username, 'username');
            if (newUser === null) return;
            const newPwd = await inputDialog('密码（留空自动生成）', password, 'new-password');
            if (newPwd === null) return;
            const finalPwd = newPwd || generatePassword(16);
            // v1.5.0: 显示密码强度
            const strength = evaluatePasswordStrength(finalPwd);
            if (strength.score < 2) {
                const proceed = await confirmDialog('密码强度提示', `当前密码强度：${strength.label}（评分 ${strength.score}/4）\n弱密码容易被破解，建议使用更复杂的密码。\n\n是否仍然使用此密码？`);
                if (!proceed) return;
            } else {
                showToast(`密码强度：${strength.label}`);
            }
            const list = store.loadPasswords(masterPwdSession) || [];
            if (existing) {
                const idx = list.findIndex(x => x.id === existing.id);
                if (idx >= 0) list[idx] = { ...list[idx], site: newSite, username: newUser, password: finalPwd };
            } else {
                list.push({ id: Date.now(), site: newSite, username: newUser, password: finalPwd });
            }
            store.savePasswords(list, masterPwdSession);
            // 渐进增强：同步到浏览器凭据存储，便于后续表单自动填充
            // 与本地加密存储独立，Credential API 不支持时静默降级
            credApi.store(newSite, newUser, finalPwd);
            renderPasswords();
            showToast('✅ 已保存');
        })();
    }

    function generatePassword(len = 16) {
        const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*';
        let pwd = '';
        for (let i = 0; i < len; i++) {
            pwd += chars[Math.floor(Math.random() * chars.length)];
        }
        return pwd;
    }

    function showPwdUnlock(callback) {
        const modal = $('#pwd-unlock-modal');
        $('#pwd-unlock-title').textContent = '🔑 输入主密码';
        const input = $('#pwd-unlock-input');
        input.value = '';
        modal.hidden = false;
        setTimeout(() => input.focus(), 50);
        const ok = $('#pwd-unlock-ok'), cancel = $('#pwd-unlock-cancel');
        const cleanup = () => { modal.hidden = true; ok.onclick = null; cancel.onclick = null; input.onkeydown = null; };
        ok.onclick = () => {
            const pwd = input.value;
            if (store.verifyMasterPwd(pwd)) {
                masterPwdSession = pwd;
                touchPwdActivity();
                cleanup();
                showToast('✅ 已解锁');
                if (callback) callback();
                else renderPasswords();
            } else {
                showToast('❌ 主密码错误');
            }
        };
        cancel.onclick = () => { cleanup(); };
        input.onkeydown = (e) => {
            if (e.key === 'Enter') ok.click();
            if (e.key === 'Escape') cancel.click();
        };
    }

    async function setupMasterPwd() {
        const existing = store.getMasterPwdHash();
        if (existing) {
            const ok = await confirmDialog('重设主密码', '重设主密码将清除所有已存密码，确定吗？');
            if (!ok) return;
        }
        const pwd = await inputDialog('设置主密码（至少 6 位）', '');
        if (!pwd || pwd.length < 6) {
            showToast('密码至少 6 位');
            return;
        }
        const confirm = await inputDialog('再次输入确认', '');
        if (pwd !== confirm) {
            showToast('两次输入不一致');
            return;
        }
        store.setMasterPwd(pwd);
        // 清除旧密码
        save(STORAGE_KEYS.passwords, null);
        masterPwdSession = pwd;
        touchPwdActivity();
        showToast('✅ 主密码已设置');
    }

    // ============ v1.5.0: 安全加固 ============
    function touchPwdActivity() {
        lastPwdActivity = Date.now();
        restartAutoLockTimer();
    }

    function restartAutoLockTimer() {
        if (autoLockTimer) clearTimeout(autoLockTimer);
        const minutes = store.getPwdAutoLock();
        if (!masterPwdSession || !minutes || minutes <= 0) return;
        autoLockTimer = setTimeout(() => {
            if (masterPwdSession && Date.now() - lastPwdActivity >= minutes * 60 * 1000 - 1000) {
                masterPwdSession = null;
                showToast('🔒 已自动锁屏，请重新输入主密码');
                // 如果当前在密码页，刷新视图
                if ($('#page-passwords').classList.contains('active')) renderPasswords();
            }
        }, minutes * 60 * 1000);
    }

    // 用户活动监听（仅在解锁状态下重置计时器）
    function setupActivityMonitor() {
        ['click', 'keydown', 'touchstart'].forEach(evt => {
            document.addEventListener(evt, () => {
                if (masterPwdSession) {
                    // 仅在密码页或相关操作时重置，避免全局频繁重置
                    // 简化：每次活动都更新
                    lastPwdActivity = Date.now();
                    restartAutoLockTimer();
                }
            }, { passive: true });
        });
    }

    // 密码强度评估：委派到 MSBUtils（v1.5.0 起统一工具库）
    function evaluatePasswordStrength(pwd) {
        if (U.evaluatePasswordStrength) return U.evaluatePasswordStrength(pwd);
        if (!pwd) return { score: 0, label: '空' };
        let score = 0;
        if (pwd.length >= 8) score++;
        if (pwd.length >= 12) score++;
        if (pwd.length >= 16) score++;
        const variety = [
            /[a-z]/.test(pwd), /[A-Z]/.test(pwd), /\d/.test(pwd), /[^a-zA-Z0-9]/.test(pwd)
        ].filter(Boolean).length;
        if (variety >= 2) score++;
        if (variety >= 3) score++;
        const weak = ['123456', 'password', 'qwerty', '111111', '000000', 'abc123'];
        if (weak.includes(pwd.toLowerCase())) score = 0;
        score = Math.min(4, score);
        const labels = ['弱', '一般', '中等', '强', '非常强'];
        const colors = ['#F44336', '#FF9800', '#FFC107', '#8BC34A', '#4CAF50'];
        return { score, label: labels[score], color: colors[score] };
    }

    // ============ v1.5.0: 错误监控 ============
    function setupErrorMonitor() {
        window.addEventListener('error', (e) => {
            store.addErrorLog({
                type: 'js',
                message: e.message || '未知错误',
                filename: e.filename || '',
                line: e.lineno || 0,
                col: e.colno || 0,
                stack: e.error && e.error.stack ? String(e.error.stack).slice(0, 500) : ''
            });
        });
        window.addEventListener('unhandledrejection', (e) => {
            const reason = e.reason;
            store.addErrorLog({
                type: 'promise',
                message: reason && reason.message ? reason.message : String(reason || '未处理的 Promise 拒绝'),
                filename: '',
                line: 0,
                col: 0,
                stack: reason && reason.stack ? String(reason.stack).slice(0, 500) : ''
            });
        });
    }

    function showErrorLog() {
        const log = store.getErrorLog();
        const modal = $('#pwd-unlock-modal'); // 复用通用 modal
        if (!modal) return;
        $('#pwd-unlock-title').textContent = `📜 错误日志（最近 ${log.length} 条）`;
        const body = modal.querySelector('.modal-body') || modal.querySelector('div');
        // 临时把 modal-body 替换为日志列表
        let logHtml = '';
        if (log.length === 0) {
            logHtml = '<div style="text-align:center;padding:32px;color:var(--text-secondary)">🎉 暂无错误日志</div>';
        } else {
            logHtml = '<div style="max-height:400px;overflow-y:auto">';
            log.forEach(entry => {
                logHtml += `
                    <div style="padding:8px 0;border-bottom:1px solid var(--divider);font-size:12px">
                        <div style="display:flex;justify-content:space-between;margin-bottom:4px">
                            <span style="color:${entry.type === 'promise' ? '#FF9800' : '#F44336'};font-weight:500">[${entry.type}] ${escapeHtml(entry.message)}</span>
                            <span style="color:var(--gray)">${new Date(entry.time).toLocaleString()}</span>
                        </div>
                        ${entry.filename ? `<div style="color:var(--text-secondary)">${escapeHtml(entry.filename)}:${entry.line}:${entry.col}</div>` : ''}
                        ${entry.stack ? `<pre style="margin-top:4px;color:var(--text-secondary);font-size:11px;white-space:pre-wrap;max-height:80px;overflow-y:auto">${escapeHtml(entry.stack)}</pre>` : ''}
                    </div>`;
            });
            logHtml += '</div>';
        }
        // 简化：直接用 modal 容器
        modal.querySelectorAll('input, .modal-actions').forEach(el => el.style.display = 'none');
        const logContainer = document.createElement('div');
        logContainer.id = 'error-log-content';
        logContainer.innerHTML = logHtml;
        if (!$('#error-log-content')) {
            modal.querySelector('.modal-card, .modal-body, div').appendChild(logContainer);
        } else {
            $('#error-log-content').innerHTML = logHtml;
        }
        modal.hidden = false;
        const ok = $('#pwd-unlock-ok');
        ok.textContent = '关闭';
        ok.style.display = '';
        ok.onclick = () => {
            modal.hidden = true;
            // 恢复显示
            modal.querySelectorAll('input, .modal-actions').forEach(el => el.style.display = '');
            ok.textContent = '确定';
            ok.onclick = null;
            if ($('#error-log-content')) $('#error-log-content').remove();
        };
    }

    // ============ v1.6.0: 用户脚本（油猴） ============
    function injectUserScripts(frame, url) {
        if (!url) return;
        const scripts = store.getUserScripts().filter(s => s.enabled);
        if (scripts.length === 0) return;
        try {
            const doc = frame.contentDocument;
            if (!doc) return; // 跨域无法访问
            scripts.forEach(script => {
                try {
                    const pattern = new RegExp(script.pattern || '.*');
                    if (!pattern.test(url)) return;
                    const tag = doc.createElement('script');
                    tag.textContent = script.code;
                    tag.dataset.msbScript = String(script.id);
                    (doc.head || doc.documentElement).appendChild(tag);
                } catch (e) {
                    console.warn('[MSB] 脚本注入失败:', script.name, e);
                }
            });
        } catch {
            // 跨域 iframe，无法注入（浏览器安全策略）
        }
    }

    function showUserScriptsManager() {
        const modal = $('#pwd-unlock-modal');
        $('#pwd-unlock-title').textContent = '🐵 用户脚本管理';
        // 隐藏输入区，显示脚本列表
        modal.querySelectorAll('input, .modal-actions').forEach(el => el.style.display = 'none');
        let list = $('#user-script-list');
        if (!list) {
            list = document.createElement('div');
            list.id = 'user-script-list';
            modal.querySelector('.modal-card, .modal-body, div').appendChild(list);
        }
        const renderScriptList = () => {
            const scripts = store.getUserScripts();
            let html = '<div style="max-height:320px;overflow-y:auto;margin-bottom:12px">';
            if (scripts.length === 0) {
                html += '<div style="text-align:center;padding:24px;color:var(--text-secondary)">暂无脚本，点击下方按钮添加</div>';
            } else {
                scripts.forEach(s => {
                    html += `
                        <div style="padding:10px;border-bottom:1px solid var(--divider)">
                            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
                                <strong>${escapeHtml(s.name)}</strong>
                                <label style="font-size:12px;color:var(--text-secondary)">
                                    <input type="checkbox" data-id="${s.id}" class="us-toggle" ${s.enabled ? 'checked' : ''}> 启用
                                </label>
                            </div>
                            <div style="font-size:11px;color:var(--gray)">pattern: ${escapeHtml(s.pattern || '.*')}</div>
                            ${s.description ? `<div style="font-size:12px;color:var(--text-secondary);margin-top:2px">${escapeHtml(s.description)}</div>` : ''}
                            <div style="margin-top:6px;display:flex;gap:8px">
                                <button class="btn-text-small" data-act="edit" data-id="${s.id}">编辑</button>
                                <button class="btn-text-small" data-act="del" data-id="${s.id}" style="color:var(--accent)">删除</button>
                            </div>
                        </div>`;
                });
            }
            html += '</div>';
            html += '<div style="display:flex;gap:8px;flex-wrap:wrap">';
            html += '<button class="btn-primary" id="us-add">＋ 添加脚本</button>';
            html += '<button class="btn-text" id="us-import-builtin">导入内置示例</button>';
            html += '<button class="btn-text" id="us-close">关闭</button>';
            html += '</div>';
            list.innerHTML = html;
            // 绑定事件
            list.querySelectorAll('.us-toggle').forEach(cb => {
                cb.onchange = (e) => {
                    store.updateUserScript(Number(e.target.dataset.id), { enabled: e.target.checked });
                };
            });
            list.querySelectorAll('[data-act="edit"]').forEach(btn => {
                btn.onclick = () => editUserScript(Number(btn.dataset.id), renderScriptList);
            });
            list.querySelectorAll('[data-act="del"]').forEach(btn => {
                btn.onclick = async() => {
                    if (await confirmDialog('删除脚本', '确定删除此用户脚本吗？')) {
                        store.deleteUserScript(Number(btn.dataset.id));
                        renderScriptList();
                    }
                };
            });
            $('#us-add').onclick = () => editUserScript(null, renderScriptList);
            $('#us-import-builtin').onclick = () => {
                BUILTIN_SCRIPTS.forEach(s => store.addUserScript(s));
                showToast(`已导入 ${BUILTIN_SCRIPTS.length} 个示例脚本`);
                renderScriptList();
            };
            $('#us-close').onclick = () => {
                modal.hidden = true;
                modal.querySelectorAll('input, .modal-actions').forEach(el => el.style.display = '');
                if ($('#user-script-list')) $('#user-script-list').remove();
            };
        };
        renderScriptList();
        modal.hidden = false;
    }

    async function editUserScript(existing, onDone) {
        const s = existing ? store.getUserScripts().find(x => x.id === existing) : null;
        const name = await inputDialog('脚本名称', s?.name || '');
        if (name === null) return;
        const pattern = await inputDialog('URL 匹配正则（默认 .* 匹配所有）', s?.pattern || '.*');
        if (pattern === null) return;
        const description = await inputDialog('描述（可选）', s?.description || '');
        if (description === null) return;
        const code = await inputDialog('脚本代码（JavaScript）', s?.code || '');
        if (code === null) return;
        if (!name.trim() || !code.trim()) {
            showToast('名称和代码不能为空');
            return;
        }
        if (existing) {
            store.updateUserScript(existing, { name: name.trim(), pattern: pattern.trim() || '.*', description: description.trim(), code });
        } else {
            store.addUserScript({ name: name.trim(), pattern: pattern.trim() || '.*', description: description.trim(), code });
        }
        showToast('✅ 脚本已保存');
        if (onDone) onDone();
    }

    // ============ v1.6.0: 长图导出 + PDF 导出 ============
    async function captureFullScreenshot(content) {
        if (typeof html2canvas === 'undefined') {
            showToast('截图库未加载');
            return;
        }
        showToast('📸 正在生成长图...');
        try {
            // 临时移除滚动限制以截取完整内容
            const oldOverflow = content.style.overflowY;
            const oldMaxHeight = content.style.maxHeight;
            content.style.overflowY = 'visible';
            content.style.maxHeight = 'none';
            const canvas = await html2canvas(content, {
                scale: 2,
                useCORS: true,
                backgroundColor: getComputedStyle(content).backgroundColor || '#ffffff',
                scrollY: 0,
                windowWidth: content.scrollWidth
            });
            content.style.overflowY = oldOverflow;
            content.style.maxHeight = oldMaxHeight;
            canvas.toBlob((blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `msb-长图-${new Date().toISOString().slice(0, 10)}.png`;
                a.click();
                URL.revokeObjectURL(url);
                showToast('✅ 长图已下载');
            }, 'image/png');
        } catch (e) {
            showToast('❌ 截图失败：' + e.message);
            store.addErrorLog({ type: 'js', message: '长图截图失败: ' + e.message, filename: 'app.js', line: 0, col: 0, stack: e.stack || '' });
        }
    }

    async function exportPdf(content) {
        if (typeof html2canvas === 'undefined') {
            showToast('截图库未加载');
            return;
        }
        // 动态加载 jsPDF
        if (typeof window.jspdf === 'undefined') {
            await loadScript('https://cdn.jsdelivr.net/npm/jspdf@2.5.1/dist/jspdf.umd.min.js');
        }
        if (typeof window.jspdf === 'undefined') {
            showToast('PDF 库加载失败');
            return;
        }
        showToast('📄 正在生成 PDF...');
        try {
            const oldOverflow = content.style.overflowY;
            const oldMaxHeight = content.style.maxHeight;
            content.style.overflowY = 'visible';
            content.style.maxHeight = 'none';
            const canvas = await html2canvas(content, { scale: 2, useCORS: true, backgroundColor: '#ffffff' });
            content.style.overflowY = oldOverflow;
            content.style.maxHeight = oldMaxHeight;
            const { jsPDF } = window.jspdf;
            const imgData = canvas.toDataURL('image/png');
            const pdfW = 210; // A4 mm
            const pdfH = (canvas.height * pdfW) / canvas.width;
            const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: [pdfW, pdfH] });
            pdf.addImage(imgData, 'PNG', 0, 0, pdfW, pdfH);
            pdf.save(`msb-文档-${new Date().toISOString().slice(0, 10)}.pdf`);
            showToast('✅ PDF 已下载');
        } catch (e) {
            showToast('❌ PDF 生成失败：' + e.message);
            store.addErrorLog({ type: 'js', message: 'PDF 生成失败: ' + e.message, filename: 'app.js', line: 0, col: 0, stack: e.stack || '' });
        }
    }

    function loadScript(src) {
        return new Promise((resolve) => {
            const s = document.createElement('script');
            s.src = src;
            s.onload = resolve;
            s.onerror = resolve;
            document.head.appendChild(s);
        });
    }

    // ============ v1.6.0: 笔记 Markdown 导出 ============
    function exportNotesMarkdown() {
        const notes = store.getNotes();
        if (notes.length === 0) {
            showToast('暂无笔记可导出');
            return;
        }
        // 按 sourceUrl 分组
        const groups = {};
        notes.forEach(n => {
            const key = n.sourceUrl || '未分类';
            if (!groups[key]) groups[key] = { title: n.sourceTitle || key, items: [] };
            groups[key].items.push(n);
        });
        let md = `# MultiSearch Browser · 划线笔记\n\n导出时间：${new Date().toLocaleString()}\n总计 ${notes.length} 条\n\n---\n\n`;
        Object.entries(groups).forEach(([url, g]) => {
            md += `## ${g.title}\n\n来源：${url}\n\n`;
            g.items.forEach((n, i) => {
                md += `### ${i + 1}. ${n.text.slice(0, 50)}${n.text.length > 50 ? '…' : ''}\n\n`;
                md += `> ${n.text.replace(/\n/g, '\n> ')}\n\n`;
                md += `- 时间：${new Date(n.timestamp).toLocaleString()}\n\n`;
            });
            md += '---\n\n';
        });
        const blob = new Blob([md], { type: 'text/markdown;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `msb-笔记-${new Date().toISOString().slice(0, 10)}.md`;
        a.click();
        URL.revokeObjectURL(url);
        showToast(`✅ 已导出 ${notes.length} 条笔记`);
    }

    // ============ v1.4.0: 跨设备同步 ============
    function showSyncConfig() {
        const modal = $('#sync-config-modal');
        const cfg = store.getSyncConfig();
        $('#sync-type').value = cfg.type || 'off';
        $('#sync-url').value = cfg.url || '';
        $('#sync-user').value = cfg.user || '';
        $('#sync-pass').value = cfg.pass || '';
        modal.hidden = false;
        $('#sync-save').onclick = () => {
            const newCfg = {
                type: $('#sync-type').value,
                url: $('#sync-url').value.trim(),
                user: $('#sync-user').value.trim(),
                pass: $('#sync-pass').value
            };
            store.setSyncConfig(newCfg);
            modal.hidden = true;
            showToast('✅ 同步配置已保存');
        };
        $('#sync-cancel').onclick = () => { modal.hidden = true; };
    }

    // 还原已存笔记高亮
    function restoreNoteMarks(container) {
        const url = currentWebview.url;
        if (!url) return;
        const notes = store.getNotes().filter(n => n.sourceUrl === url);
        if (notes.length === 0) return;
        const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, {
            acceptNode: (node) => node.nodeValue.trim() ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT
        });
        const nodes = [];
        let n;
        while ((n = walker.nextNode())) nodes.push(n);
        notes.forEach(note => {
            const text = note.text;
            if (!text) return;
            nodes.forEach(node => {
                const idx = node.nodeValue.indexOf(text);
                if (idx >= 0 && idx + text.length <= node.nodeValue.length) {
                    try {
                        const range = document.createRange();
                        range.setStart(node, idx);
                        range.setEnd(node, idx + text.length);
                        const mark = document.createElement('mark');
                        mark.className = 'msb-note-mark';
                        mark.dataset.noteId = note.id;
                        range.surroundContents(mark);
                    } catch {}
                }
            });
        });
    }

    function setupNotesPopover() {
        const pop = $('#note-popover');
        const addBtn = $('#note-add-btn');
        addBtn.onclick = () => {
            const text = pop.dataset.text;
            if (!text) { pop.hidden = true; return; }
            store.addNote(text, currentWebview.title || '', currentWebview.url);
            // 高亮当前选区
            try {
                const sel = $('#reading-content').ownerDocument.getSelection();
                if (sel.rangeCount > 0) {
                    const range = sel.getRangeAt(0);
                    const mark = document.createElement('mark');
                    mark.className = 'msb-note-mark';
                    range.surroundContents(mark);
                    sel.removeAllRanges();
                }
            } catch {}
            pop.hidden = true;
            showToast('📝 笔记已添加');
        };
        // 点击空白处隐藏
        document.addEventListener('mousedown', (e) => {
            if (pop.hidden) return;
            if (!pop.contains(e.target) && !e.target.closest('.reading-content')) {
                pop.hidden = true;
            }
        });
    }

    function updateBookmarkIcon() {
        const btn = $('#wv-bookmark');
        const bookmarked = store.isBookmarked(currentWebview.url);
        btn.textContent = bookmarked ? '★' : '☆';
        btn.style.color = bookmarked ? 'var(--accent)' : 'var(--text-primary)';
    }

    function normalizeInput(input) {
        if (/^https?:\/\//i.test(input)) return input;
        const looksLikeDomain = input.includes('.') && !input.includes(' ');
        if (looksLikeDomain) return 'https://' + input;
        const allEngines = getAllEngines();
        const engine = allEngines.find(e => e.id === store.getSelectedEngine()) || allEngines[0];
        return engine.searchUrl + encodeURIComponent(input);
    }

    // ============ 多窗口 ============
    let dragSrcId = null;

    function findEngine(url) {
        return getAllEngines().find(e => url && url.startsWith(e.searchUrl));
    }

    function renderWindows() {
        const list = store.getWindows();
        $('#window-count').textContent = `${list.length} 个窗口`;
        $('#windows-empty').hidden = list.length > 0;
        const listEl = $('#windows-list');
        listEl.innerHTML = '';
        list.forEach(win => {
            const card = document.createElement('div');
            card.className = 'list-item-card';
            card.draggable = true;
            card.dataset.winId = win.id;
            const engine = findEngine(win.url);
            const iconChar = engine ? engine.name[0] : '🌐';
            const iconColor = engine ? engine.color : '#757575';
            card.innerHTML = `
                <div class="lic-icon" style="background:${iconColor}">${iconChar}</div>
                <div class="lic-body">
                    <div class="lic-title">${escapeHtml(win.title || '无标题')}</div>
                    <div class="lic-url">${escapeHtml(win.url)}</div>
                    <div class="lic-time">${formatTime(win.timestamp)}</div>
                </div>
                <button class="lic-btn" data-act="rename" aria-label="重命名">✏</button>
                <button class="lic-btn danger" data-act="close" aria-label="关闭">✕</button>
            `;
            card.querySelector('.lic-body').onclick = () => openWebview(win.url, win.id);
            card.querySelector('[data-act="rename"]').onclick = async(e) => {
                e.stopPropagation();
                const newTitle = await inputDialog('重命名窗口', win.title || '');
                if (newTitle !== null && newTitle !== win.title) {
                    store.updateWindow(win.id, { title: newTitle });
                    renderWindows();
                    showToast('已重命名');
                }
            };
            card.querySelector('[data-act="close"]').onclick = (e) => {
                e.stopPropagation();
                store.deleteWindow(win.id);
                renderWindows();
                showToast('已关闭窗口');
            };
            // 拖拽事件
            card.addEventListener('dragstart', (e) => {
                dragSrcId = win.id;
                card.classList.add('dragging');
                e.dataTransfer.effectAllowed = 'move';
            });
            card.addEventListener('dragend', () => {
                card.classList.remove('dragging');
                listEl.querySelectorAll('.list-item-card').forEach(c => c.classList.remove('drag-over'));
            });
            card.addEventListener('dragover', (e) => {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                card.classList.add('drag-over');
            });
            card.addEventListener('dragleave', () => {
                card.classList.remove('drag-over');
            });
            card.addEventListener('drop', (e) => {
                e.preventDefault();
                card.classList.remove('drag-over');
                const srcId = dragSrcId;
                const dstId = win.id;
                if (srcId === null || srcId === dstId) return;
                const arr = store.getWindows();
                const srcIdx = arr.findIndex(w => w.id === srcId);
                const dstIdx = arr.findIndex(w => w.id === dstId);
                if (srcIdx < 0 || dstIdx < 0) return;
                const [moved] = arr.splice(srcIdx, 1);
                arr.splice(dstIdx, 0, moved);
                store.reorderWindows(arr);
                renderWindows();
                showToast('已重排窗口');
            });
            listEl.appendChild(card);
        });
    }

    function setupWindowsPage() {
        $('#add-window-btn').onclick = () => {
            const win = store.addWindow('新窗口', 'https://www.baidu.com');
            openWebview('https://www.baidu.com', win.id);
        };
    }

    // ============ 历史 ============
    function renderHistory(filter = '') {
        const list = store.getHistory();
        const filtered = filter
            ? list.filter(h => (h.title + h.url).toLowerCase().includes(filter.toLowerCase()))
            : list;
        $('#history-empty').hidden = filtered.length > 0;
        const listEl = $('#history-list');
        listEl.innerHTML = '';
        filtered.forEach(h => {
            const card = document.createElement('div');
            card.className = 'list-item-card';
            const engine = findEngine(h.url);
            const iconChar = engine ? engine.name[0] : '🌐';
            const iconColor = engine ? engine.color : '#757575';
            card.innerHTML = `
                <div class="lic-icon" style="background:${iconColor}">${iconChar}</div>
                <div class="lic-body">
                    <div class="lic-title">${escapeHtml(h.title || '无标题')}</div>
                    <div class="lic-url">${escapeHtml(h.url)}</div>
                    <div class="lic-time">${formatTime(h.timestamp)}</div>
                </div>
            `;
            card.onclick = () => openWebview(h.url, -1);
            card.oncontextmenu = async(e) => {
                e.preventDefault();
                const ok = await confirmDialog('删除记录', '确定要删除这条记录吗？');
                if (ok) { store.deleteHistory(h.id); renderHistory(filter); }
            };
            listEl.appendChild(card);
        });
    }

    function setupHistoryPage() {
        $('#clear-history-btn').onclick = async() => {
            const ok = await confirmDialog('清空历史', '确定要清空所有浏览历史吗？此操作不可撤销。');
            if (ok) { store.clearHistory(); renderHistory(); showToast('已清空历史'); }
        };
        $('#history-filter').addEventListener('input', (e) => {
            renderHistory(e.target.value.trim());
        });
    }

    // ============ 书签（支持文件夹分组） ============
    function renderBookmarks(filter = '') {
        const list = store.getBookmarks();
        const folders = store.getBookmarkFolders();
        const filtered = filter
            ? list.filter(b => (b.title + b.url).toLowerCase().includes(filter.toLowerCase()))
            : list;
        $('#bookmarks-empty').hidden = filtered.length > 0;
        const listEl = $('#bookmarks-list');
        listEl.innerHTML = '';

        // 过滤模式下平铺
        if (filter) {
            filtered.forEach(b => listEl.appendChild(buildBookmarkCard(b, filter)));
            return;
        }

        // 未分类
        const uncategorized = filtered.filter(b => !b.folder);
        if (uncategorized.length > 0) {
            const header = document.createElement('div');
            header.className = 'folder-header';
            header.innerHTML = `<span>📄 未分类</span><span class="count">${uncategorized.length}</span>`;
            listEl.appendChild(header);
            const items = document.createElement('div');
            items.className = 'folder-items';
            uncategorized.forEach(b => items.appendChild(buildBookmarkCard(b, filter)));
            listEl.appendChild(items);
        }

        // 各文件夹
        folders.forEach(folder => {
            const items = filtered.filter(b => b.folder === folder);
            if (items.length === 0 && filter) return;
            const header = document.createElement('div');
            header.className = 'folder-header';
            header.innerHTML = `<span>📁 ${escapeHtml(folder)}</span><span class="count">${items.length}</span>`;
            header.style.cursor = 'pointer';
            const itemsBox = document.createElement('div');
            itemsBox.className = 'folder-items';
            items.forEach(b => itemsBox.appendChild(buildBookmarkCard(b, filter, folder)));
            let collapsed = false;
            header.onclick = () => {
                collapsed = !collapsed;
                itemsBox.hidden = collapsed;
                header.querySelector('span').textContent = collapsed
                    ? `📁 ${escapeHtml(folder)} ▶`
                    : `📁 ${escapeHtml(folder)}`;
            };
            // 删除文件夹按钮
            const delBtn = document.createElement('button');
            delBtn.className = 'lic-btn danger';
            delBtn.textContent = '✕';
            delBtn.style.marginLeft = '8px';
            delBtn.onclick = async(e) => {
                e.stopPropagation();
                const ok = await confirmDialog('删除文件夹', `删除文件夹「${folder}」？内部书签将移到未分类。`);
                if (ok) {
                    store.deleteBookmarkFolder(folder);
                    renderBookmarks(filter);
                    showToast('文件夹已删除');
                }
            };
            header.appendChild(delBtn);
            listEl.appendChild(header);
            listEl.appendChild(itemsBox);
        });
    }

    function buildBookmarkCard(b, filter, folder) {
        const card = document.createElement('div');
        card.className = 'list-item-card';
        const engine = findEngine(b.url);
        const iconChar = engine ? engine.name[0] : '🌐';
        const iconColor = engine ? engine.color : '#FF5722';
        card.innerHTML = `
            <div class="lic-icon" style="background:${iconColor}">${iconChar}</div>
            <div class="lic-body">
                <div class="lic-title">${escapeHtml(b.title || '无标题')}</div>
                <div class="lic-url">${escapeHtml(b.url)}</div>
            </div>
            <button class="lic-btn" data-act="move" aria-label="移动到文件夹">📁</button>
            <button class="lic-btn danger" data-act="del" aria-label="删除">✕</button>
        `;
        card.querySelector('.lic-body').onclick = () => openWebview(b.url, -1);
        card.querySelector('[data-act="move"]').onclick = async(e) => {
            e.stopPropagation();
            const folders = store.getBookmarkFolders();
            const options = ['未分类', ...folders];
            const sel = await inputDialog(`移动「${b.title || '该书签'}」到文件夹（输入名称）`, b.folder || '');
            if (sel === null) return;
            if (sel === '未分类') {
                store.moveBookmark(b.url, '');
            } else {
                if (!folders.includes(sel)) store.addBookmarkFolder(sel);
                store.moveBookmark(b.url, sel);
            }
            renderBookmarks(filter);
            showToast('已移动');
        };
        card.querySelector('[data-act="del"]').onclick = async(e) => {
            e.stopPropagation();
            const ok = await confirmDialog('删除收藏', '确定要删除这个收藏吗？');
            if (ok) { store.deleteBookmark(b.url); renderBookmarks(filter); }
        };
        return card;
    }

    function setupBookmarksPage() {
        $('#bookmarks-filter').addEventListener('input', (e) => {
            renderBookmarks(e.target.value.trim());
        });
        $('#add-folder-btn').onclick = async() => {
            const name = await inputDialog('新建书签文件夹', '');
            if (!name) return;
            store.addBookmarkFolder(name);
            renderBookmarks();
            showToast('文件夹已创建');
        };
    }

    // ============ 我的 ============
    function renderProfile() {
        const avatar = store.getAvatar();
        $('#avatar-img').src = avatar || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><rect width="80" height="80" fill="%23F5F5F5"/><text x="40" y="48" text-anchor="middle" font-size="32" fill="%239E9E9E">👤</text></svg>';
    }

    function setupProfilePage() {
        $('#avatar-input').onchange = (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = () => {
                store.setAvatar(reader.result);
                renderProfile();
                showToast('头像已上传');
            };
            reader.readAsDataURL(file);
        };

        $$('[data-action]').forEach(el => {
            el.onclick = () => {
                const action = el.dataset.action;
                if (action === 'history') navigate('history');
                else if (action === 'bookmarks') navigate('bookmarks');
                else if (action === 'later') navigate('later');
                else if (action === 'notes') navigate('notes');
                else if (action === 'rss') navigate('rss');
                else if (action === 'passwords') navigate('passwords');
                else if (action === 'stats') navigate('stats');
                else if (action === 'settings') navigate('settings');
            };
        });
    }

    // ============ 设置页 ============
    function renderSettings() {
        // 默认引擎下拉（含自定义引擎）
        const select = $('#setting-default-engine');
        select.innerHTML = '';
        getAllEngines().forEach(e => {
            const opt = document.createElement('option');
            opt.value = e.id;
            opt.textContent = e.name + (e.custom ? ' (自定义)' : '');
            opt.selected = e.id === store.getSelectedEngine();
            select.appendChild(opt);
        });
        // 深色模式开关
        $('#dark-mode-toggle').checked = store.getTheme() === 'dark';

        // 暗黑定时
        const schedSel = $('#dark-mode-schedule');
        if (schedSel) schedSel.value = store.getDarkSchedule();

        // 主题色选择器
        const picker = $('#theme-color-picker');
        if (picker) {
            picker.innerHTML = '';
            const current = store.getThemeColor();
            THEME_COLORS.forEach(c => {
                const dot = document.createElement('button');
                dot.className = 'theme-color-dot' + (c.id === current ? ' active' : '');
                dot.style.background = c.primary;
                dot.title = c.id;
                dot.onclick = () => {
                    store.setThemeColor(c.id);
                    applyThemeColor(c.id);
                    renderSettings();
                    showToast('主题色已更新');
                };
                picker.appendChild(dot);
            });
        }

        // 自定义引擎列表
        renderCustomEngineList();

        // v1.4.0: AI 摘要模式 / 翻译模式
        const aiSel = $('#ai-summary-mode');
        if (aiSel) aiSel.value = store.getAiSummaryMode();
        const trSel = $('#translate-mode');
        if (trSel) trSel.value = store.getTranslateMode();
    }

    function renderCustomEngineList() {
        const listEl = $('#custom-engine-list');
        if (!listEl) return;
        const customEngines = store.getCustomEngines();
        listEl.innerHTML = '';
        if (customEngines.length === 0) {
            listEl.innerHTML = '<div style="font-size:12px;color:var(--text-secondary);padding:8px;text-align:center">暂无自定义引擎，点击「添加」</div>';
            return;
        }
        customEngines.forEach(e => {
            const item = document.createElement('div');
            item.className = 'custom-engine-item';
            item.innerHTML = `
                <div class="ce-info">
                    <div class="ce-name">${escapeHtml(e.name)}</div>
                    <div class="ce-url">${escapeHtml(e.searchUrl)}</div>
                </div>
                <span class="ce-del" data-id="${e.id}">✕</span>
            `;
            item.querySelector('.ce-del').onclick = async(ev) => {
                ev.stopPropagation();
                const ok = await confirmDialog('删除引擎', `确定要删除自定义引擎「${e.name}」吗？`);
                if (ok) {
                    store.deleteCustomEngine(e.id);
                    renderCustomEngineList();
                    renderSettings();
                    renderHome();
                    showToast('已删除');
                }
            };
            listEl.appendChild(item);
        });
    }

    function setupSettingsPage() {
        $('#setting-default-engine').onchange = (e) => {
            store.setSelectedEngine(e.target.value);
            showToast('默认引擎已更新');
        };
        $('#dark-mode-toggle').onchange = (e) => {
            // 手动切换会关闭暗黑定时
            if (store.getDarkSchedule() !== 'off') {
                store.setDarkSchedule('off');
                $('#dark-mode-schedule').value = 'off';
            }
            const theme = e.target.checked ? 'dark' : 'light';
            store.setTheme(theme);
            applyTheme(theme);
        };
        $('#dark-mode-schedule').onchange = (e => {
            store.setDarkSchedule(e.target.value);
            applyDarkSchedule();
            showToast('暗黑定时已更新');
        });
        $('#add-engine-btn').onclick = async() => {
            const name = await inputDialog('引擎名称（如：知乎）', '');
            if (!name) return;
            const searchUrl = await inputDialog('搜索 URL 模板（关键词用 {q} 占位）', 'https://www.zhihu.com/search?q={q}');
            if (!searchUrl) return;
            if (!searchUrl.includes('{q}')) {
                showToast('URL 必须包含 {q} 占位符');
                return;
            }
            // 替换占位符为实际拼接形式
            const finalUrl = searchUrl.replace('{q}', '');
            const color = await inputDialog('主题色（#RRGGBB，可留空）', '#757575');
            store.addCustomEngine(name, finalUrl, color || '#757575');
            renderCustomEngineList();
            renderSettings();
            renderHome();
            showToast('自定义引擎已添加');
        };
        $('#import-file').onchange = (e) => { if (e.target.files[0]) importData(e.target.files[0]); e.target.value = ''; };
        $('#import-bookmarks-file').onchange = (e) => { if (e.target.files[0]) importBookmarksHtml(e.target.files[0]); e.target.value = ''; };

        // v1.4.0: AI / 翻译模式
        $('#ai-summary-mode').onchange = (e) => {
            store.setAiSummaryMode(e.target.value);
            showToast('AI 摘要模式已更新');
        };
        $('#translate-mode').onchange = (e) => {
            store.setTranslateMode(e.target.value);
            showToast('翻译模式已更新');
        };

        $$('[data-action]').forEach(el => {
            if (el.id === 'setting-dark-mode') return;
            el.onclick = async() => {
                const action = el.dataset.action;
                if (action === 'export') exportData();
                else if (action === 'import') $('#import-file').click();
                else if (action === 'export-bookmarks-html') exportBookmarksHtml();
                else if (action === 'import-bookmarks-html') $('#import-bookmarks-file').click();
                else if (action === 'clear-all') clearAllData();
                else if (action === 'open-incognito') openIncognito();
                else if (action === 'config-llm') showLlmConfig();
                else if (action === 'config-sync') showSyncConfig();
                else if (action === 'sync-now') syncNow();
                else if (action === 'config-master-pwd') setupMasterPwd();
                else if (action === 'clear-translate-cache') {
                    if (await confirmDialog('清空翻译缓存', '将清除所有已缓存的翻译结果，下次查询将重新请求 API。确定吗？')) {
                        store.clearTranslateCache();
                        showToast('翻译缓存已清空');
                    }
                }
                else if (action === 'view-error-log') showErrorLog();
                else if (action === 'clear-error-log') {
                    if (await confirmDialog('清空错误日志', '确定清除所有错误日志吗？')) {
                        store.clearErrorLog();
                        showToast('错误日志已清空');
                    }
                }
                // v1.6.0
                else if (action === 'manage-scripts') showUserScriptsManager();
                else if (action === 'export-notes-md') exportNotesMarkdown();
                else if (action === 'toggle-ad-block') {
                    const newVal = !store.getAdBlockEnabled();
                    store.setAdBlockEnabled(newVal);
                    showToast(newVal ? '✅ 广告拦截已开启' : '⚠️ 广告拦截已关闭');
                    renderSettings();
                }
            };
        });
        // v1.6.0: 广告拦截开关初始化
        const adSwitch = $('#ad-block-toggle');
        if (adSwitch) adSwitch.checked = store.getAdBlockEnabled();
    }

    // ============ 暗黑定时 ============
    function applyDarkSchedule() {
        const mode = store.getDarkSchedule();
        if (mode === 'off') return;
        if (mode === 'auto') {
            const mq = window.matchMedia('(prefers-color-scheme: dark)');
            const theme = mq.matches ? 'dark' : 'light';
            store.setTheme(theme);
            applyTheme(theme);
            mq.addEventListener('change', (e) => {
                const t = e.matches ? 'dark' : 'light';
                store.setTheme(t);
                applyTheme(t);
            });
        } else if (mode === 'time') {
            const h = new Date().getHours();
            const theme = (h >= 18 || h < 6) ? 'dark' : 'light';
            store.setTheme(theme);
            applyTheme(theme);
            // 每分钟检查一次
            clearInterval(applyDarkSchedule._timer);
            applyDarkSchedule._timer = setInterval(() => {
                const hh = new Date().getHours();
                const t = (hh >= 18 || hh < 6) ? 'dark' : 'light';
                if (store.getTheme() !== t) {
                    store.setTheme(t);
                    applyTheme(t);
                }
            }, 60000);
        }
    }

    // ============ 隐身窗口 ============
    function openIncognito() {
        const allEngines = getAllEngines();
        const engine = allEngines.find(e => e.id === store.getSelectedEngine()) || allEngines[0];
        // 创建隐身 tab
        const tab = store.addTab(engine.searchUrl, '🛡 隐身');
        store.updateTab(tab.id, { incognito: true });
        store.setActiveTabId(tab.id);
        currentWebview = { url: engine.searchUrl, title: '🛡 隐身', windowId: -1, tabId: tab.id, incognito: true };
        $('#wv-url').value = engine.searchUrl;
        $('#wv-fallback').hidden = true;
        $('#wv-frame').src = engine.searchUrl;
        $('#wv-progress').hidden = false;
        // 标记 toolbar
        const tb = $('.webview-toolbar');
        if (tb) tb.classList.add('incognito');
        clearTimeout(frameLoadTimeout);
        frameLoadTimeout = setTimeout(() => showWebviewFallback(engine.searchUrl), 3000);
        updateBookmarkIcon();
        renderTabs();
        navigate('webview');
        showToast('🛡 已进入隐身模式（不记录历史）');
    }

    // ============ 书签 HTML 互通 ============
    function exportBookmarksHtml() {
        const bookmarks = store.getBookmarks();
        const folders = store.getBookmarkFolders();
        const now = new Date().getTime();
        const folderMap = {};
        folders.forEach(f => folderMap[f] = []);
        const uncategorized = [];
        bookmarks.forEach(b => {
            if (b.folder && folderMap[b.folder]) {
                folderMap[b.folder].push(b);
            } else {
                uncategorized.push(b);
            }
        });
        const formatTime = (ts) => new Date(ts).toISOString();
        const renderLink = (b) => `        <DT><A HREF="${escapeHtml(b.url)}" ADD_DATE="${Math.floor(b.timestamp / 1000)}" LAST_MODIFIED="${Math.floor(b.timestamp / 1000)}">${escapeHtml(b.title || b.url)}</A>`;
        const lines = [
            '<!DOCTYPE NETSCAPE-Bookmark-file-1>',
            '<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">',
            '<TITLE>Bookmarks</TITLE>',
            '<H1>Bookmarks</H1>',
            '<DL><p>',
            '    <DT><H3 ADD_DATE="' + Math.floor(now / 1000) + '" LAST_MODIFIED="' + Math.floor(now / 1000) + '">MultiSearch Browser 书签</H3>',
            '    <DL><p>',
        ];
        if (uncategorized.length > 0) {
            lines.push('        <DT><H3>未分类</H3>');
            lines.push('        <DL><p>');
            uncategorized.forEach(b => lines.push(renderLink(b)));
            lines.push('        </DL><p>');
        }
        folders.forEach(f => {
            if (folderMap[f].length === 0) return;
            lines.push(`        <DT><H3>${escapeHtml(f)}</H3>`);
            lines.push('        <DL><p>');
            folderMap[f].forEach(b => lines.push(renderLink(b)));
            lines.push('        </DL><p>');
        });
        lines.push('    </DL><p>');
        lines.push('</DL><p>');

        const blob = new Blob([lines.join('\n')], { type: 'text/html' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `bookmarks-${new Date().toISOString().slice(0, 10)}.html`;
        a.click();
        URL.revokeObjectURL(url);
        showToast('书签 HTML 已导出');
    }

    async function importBookmarksHtml(file) {
        const text = await file.text();
        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(text, 'text/html');
            const links = doc.querySelectorAll('A');
            if (links.length === 0) {
                showToast('未找到书签');
                return;
            }
            const ok = await confirmDialog('导入书签', `从「${file.name}」发现 ${links.length} 个书签，确定导入吗？`);
            if (!ok) return;
            // 找出当前文件夹上下文
            let imported = 0;
            const allH3 = doc.querySelectorAll('H3');
            allH3.forEach(h3 => {
                const folderName = h3.textContent.trim();
                if (folderName && folderName !== 'Bookmarks' && folderName !== 'MultiSearch Browser 书签') {
                    store.addBookmarkFolder(folderName);
                }
            });
            links.forEach(a => {
                const href = a.getAttribute('HREF') || a.getAttribute('href');
                const title = a.textContent.trim();
                if (!href || !href.startsWith('http')) return;
                // 找父 DL 上的 H3
                let folder = '';
                let p = a.parentElement;
                while (p) {
                    const prevH3 = p.previousElementSibling;
                    if (prevH3 && prevH3.tagName === 'H3') {
                        const fname = prevH3.textContent.trim();
                        if (fname && fname !== 'Bookmarks' && fname !== 'MultiSearch Browser 书签' && fname !== '未分类') {
                            folder = fname;
                        }
                        break;
                    }
                    p = p.parentElement;
                }
                // 检查重复
                const exists = store.getBookmarks().some(b => b.url === href);
                if (!exists) {
                    const list = store.getBookmarks();
                    list.unshift({
                        id: Date.now() + imported,
                        title: title || href,
                        url: href,
                        timestamp: Date.now(),
                        folder
                    });
                    store.saveBookmarks(list);
                    imported++;
                }
            });
            showToast(`✅ 导入 ${imported} 个书签`);
            renderBookmarks();
        } catch (err) {
            showToast('导入失败：文件格式无效');
        }
    }

    async function clearAllData() {
        const ok = await confirmDialog('清除所有数据', '将清除所有书签、历史、窗口、搜索历史、笔记和头像。此操作不可撤销，确定继续吗？');
        if (ok) {
            store.clearAllData();
            initTheme();
            renderHome();
            renderProfile();
            renderAiSummary('');
            renderTabs();
            showToast('所有数据已清除');
        }
    }

    // ============ 数据导出/导入 ============
    function exportData() {
        const data = {
            version: '1.4.0',
            exportTime: new Date().toISOString(),
            engine: store.getSelectedEngine(),
            windows: store.getWindows(),
            bookmarks: store.getBookmarks(),
            bookmarkFolders: store.getBookmarkFolders(),
            history: store.getHistory(),
            searchHistory: store.getSearchHistory(),
            avatar: store.getAvatar(),
            theme: store.getTheme(),
            themeColor: store.getThemeColor(),
            parallelEngines: store.getParallelEngines(),
            laterList: store.getLaterList(),
            // v1.3.0
            notes: store.getNotes(),
            tabs: store.getTabs(),
            activeTabId: store.getActiveTabId(),
            scrollProgress: load(STORAGE_KEYS.scrollProgress, {}),
            // v1.3.1
            customEngines: store.getCustomEngines(),
            darkSchedule: store.getDarkSchedule(),
            // v1.4.0（不导出密码加密数据，安全考虑）
            rssFeeds: store.getRssFeeds(),
            rssCache: store.getRssCache(),
            syncConfig: store.getSyncConfig(),
            llmConfig: store.getLlmConfig(),
            aiSummaryMode: store.getAiSummaryMode(),
            translateMode: store.getTranslateMode(),
        };
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `multisearch-backup-${new Date().toISOString().slice(0, 10)}.json`;
        a.click();
        URL.revokeObjectURL(url);
        showToast('数据已导出');
    }

    async function importData(file) {
        if (!file) return;
        const text = await file.text();
        try {
            const data = JSON.parse(text);
            if (!data.version) throw new Error('无效的备份文件');
            const ok = await confirmDialog('导入数据', `将覆盖当前所有数据（${file.name}），确定导入吗？`);
            if (!ok) return;
            if (data.engine) store.setSelectedEngine(data.engine);
            if (data.windows) store.saveWindows(data.windows);
            if (data.bookmarks) store.saveBookmarks(data.bookmarks);
            if (data.bookmarkFolders) save(STORAGE_KEYS.bookmarkFolders, data.bookmarkFolders);
            if (data.history) store.saveHistory(data.history);
            if (data.searchHistory) save(STORAGE_KEYS.searchHistory, data.searchHistory);
            if (data.avatar) store.setAvatar(data.avatar);
            if (data.theme) { store.setTheme(data.theme); applyTheme(data.theme); }
            if (data.themeColor) { store.setThemeColor(data.themeColor); applyThemeColor(data.themeColor); }
            if (data.parallelEngines) store.setParallelEngines(data.parallelEngines);
            if (data.laterList) save(STORAGE_KEYS.laterList, data.laterList);
            if (data.notes) save(STORAGE_KEYS.notes, data.notes);
            if (data.tabs) store.saveTabs(data.tabs);
            if (data.activeTabId) store.setActiveTabId(data.activeTabId);
            if (data.scrollProgress) save(STORAGE_KEYS.scrollProgress, data.scrollProgress);
            if (data.customEngines) save(STORAGE_KEYS.customEngines, data.customEngines);
            if (data.darkSchedule) { store.setDarkSchedule(data.darkSchedule); applyDarkSchedule(); }
            // v1.4.0
            if (data.rssFeeds) save(STORAGE_KEYS.rssFeeds, data.rssFeeds);
            if (data.rssCache) save(STORAGE_KEYS.rssCache, data.rssCache);
            if (data.syncConfig) store.setSyncConfig(data.syncConfig);
            if (data.llmConfig) store.setLlmConfig(data.llmConfig);
            if (data.aiSummaryMode) store.setAiSummaryMode(data.aiSummaryMode);
            if (data.translateMode) store.setTranslateMode(data.translateMode);
            renderHome();
            renderProfile();
            renderTabs();
            showToast('数据导入成功');
        } catch (err) {
            showToast('导入失败：文件格式无效');
        }
    }

    // ============ 工具 ============
    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // ============ 底部导航 ============
    function setupBottomNav() {
        $$('.tab').forEach(tab => {
            tab.onclick = () => {
                const target = tab.dataset.tab;
                if (target === 'back') {
                    navigateBack();
                } else {
                    navigate(target);
                }
            };
        });
    }

    function setupBackButtons() {
        $$('[data-back]').forEach(btn => {
            btn.onclick = () => navigate(btn.dataset.back);
        });
    }

    // ============ AI 多源检索研讨面板 ============
    function renderAiSummary(query) {
        lastAiQuery = query || '';
        const body = $('#ai-summary-body');
        const meta = $('#ai-summary-meta');
        body.innerHTML = '';
        if (lastAiQuery) {
            meta.textContent = `当前关键词：「${lastAiQuery}」 · 点击任一视角块用该引擎搜索`;
        } else {
            meta.textContent = '输入关键词后搜索，自动生成 7 引擎多视角研讨';
        }
        // 内置 7 引擎有研讨视角，自定义引擎用通用模板
        getAllEngines().forEach(engine => {
            const p = AI_PERSPECTIVES[engine.id] || {
                icon: '🔗',
                label: `${engine.name} 视角`,
                desc: q => `使用「${engine.name}」检索「${q}」的相关结果。`
            };
            const q = lastAiQuery || '关键词';
            const block = document.createElement('div');
            block.className = 'ai-block';
            block.innerHTML = `
                <div class="ai-block-title" style="color:${engine.color}">${p.icon} ${p.label}</div>
                <div class="ai-block-desc">${escapeHtml(p.desc(q))}</div>
            `;
            block.onclick = () => {
                const url = lastAiQuery
                    ? engine.searchUrl + encodeURIComponent(lastAiQuery)
                    : engine.searchUrl;
                openWebview(url, -1);
            };
            body.appendChild(block);
        });
    }

    function copyAiReport() {
        const q = lastAiQuery || $('#search-input').value.trim() || '当前话题';
        const allEngines = getAllEngines();
        const lines = allEngines.map(e => {
            const p = AI_PERSPECTIVES[e.id] || {
                icon: '🔗',
                label: `${e.name} 视角`,
                desc: qq => `使用「${e.name}」检索「${qq}」的相关结果。`
            };
            return `- ${p.icon} ${p.label}：${p.desc(q)}`;
        });
        const text = `【AI 多源检索研讨报告 - ${q}】\n${lines.join('\n')}`;
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(
                () => showToast('📋 研讨报告已复制到剪贴板'),
                () => fallbackCopy(text)
            );
        } else {
            fallbackCopy(text);
        }
    }

    function fallbackCopy(text) {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand('copy'); showToast('📋 研讨报告已复制'); }
        catch { showToast('复制失败，请手动选择文本'); }
        document.body.removeChild(ta);
    }

    // ============ 多引擎并行搜索 ============
    function renderParallelPage() {
        const grid = $('#parallel-grid');
        const selected = store.getParallelEngines();
        grid.innerHTML = '';
        getAllEngines().forEach(engine => {
            const card = document.createElement('div');
            card.className = 'parallel-card' + (selected.includes(engine.id) ? ' active' : '');
            card.innerHTML = `
                <div class="parallel-card-header" style="background:${engine.color}">
                    <span>${engine.name}</span>
                    <span class="toggle">${selected.includes(engine.id) ? '✓' : ''}</span>
                </div>
                <iframe data-engine="${engine.id}" sandbox="allow-scripts allow-forms allow-popups allow-popups-to-escape-sandbox" referrerpolicy="no-referrer"></iframe>
            `;
            card.querySelector('.parallel-card-header').onclick = () => {
                const list = store.getParallelEngines();
                const idx = list.indexOf(engine.id);
                if (idx >= 0) list.splice(idx, 1);
                else list.push(engine.id);
                store.setParallelEngines(list);
                renderParallelPage();
                // 重新加载已有查询
                const q = $('#parallel-input').value.trim();
                if (q) runParallelSearch(q);
            };
            grid.appendChild(card);
        });
    }

    function setupParallelSearch() {
        $('#parallel-go').onclick = () => {
            const q = $('#parallel-input').value.trim();
            if (!q) { showToast('请输入关键词'); return; }
            runParallelSearch(q);
        };
        $('#parallel-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') $('#parallel-go').click();
        });
        $('#parallel-config-btn').onclick = () => {
            showToast('点击各引擎卡片顶部即可切换');
        };
    }

    function runParallelSearch(query) {
        const selected = store.getParallelEngines();
        if (selected.length === 0) { showToast('请至少选择 1 个引擎'); return; }
        store.addSearchHistory(query);
        const allEngines = getAllEngines();
        $('#parallel-grid').querySelectorAll('iframe').forEach(frame => {
            const id = frame.dataset.engine;
            if (selected.includes(id)) {
                const engine = allEngines.find(e => e.id === id);
                frame.src = engine.searchUrl + encodeURIComponent(query);
            } else {
                frame.src = 'about:blank';
            }
        });
    }

    // ============ 稍后阅读 ============
    function renderLater() {
        const list = store.getLaterList();
        const listEl = $('#later-list');
        const empty = $('#later-empty');
        if (!listEl) return;
        if (list.length === 0) {
            listEl.innerHTML = '';
            if (empty) empty.hidden = false;
            return;
        }
        if (empty) empty.hidden = true;
        listEl.innerHTML = '';
        list.forEach(l => {
            const card = document.createElement('div');
            card.className = 'list-item-card later-item';
            const engine = findEngine(l.url);
            const iconChar = engine ? engine.name[0] : '📥';
            const iconColor = engine ? engine.color : '#9C27B0';
            card.innerHTML = `
                <div class="lic-icon" style="background:${iconColor}">${iconChar}</div>
                <div class="lic-body">
                    <div class="lic-title">${escapeHtml(l.title || '无标题')}</div>
                    <div class="lic-url">${escapeHtml(l.url)}</div>
                    <div class="lic-time">${formatTime(l.timestamp)}</div>
                </div>
                <button class="lic-btn" data-act="read" aria-label="阅读">📖</button>
                <button class="lic-btn danger" data-act="del" aria-label="删除">✕</button>
            `;
            card.querySelector('[data-act="read"]').onclick = (e) => {
                e.stopPropagation();
                openWebview(l.url, -1);
            };
            card.querySelector('[data-act="del"]').onclick = (e) => {
                e.stopPropagation();
                store.deleteLater(l.id);
                renderLater();
                showToast('已删除');
            };
            listEl.appendChild(card);
        });
    }

    function setupLaterPage() {
        // 后续可加筛选/清空等
    }

    // ============ 划线笔记页 ============
    function renderNotes(filter = '') {
        const list = store.getNotes();
        const filtered = filter
            ? list.filter(n => (n.text + n.source).toLowerCase().includes(filter.toLowerCase()))
            : list;
        const listEl = $('#notes-list');
        const empty = $('#notes-empty');
        if (!listEl) return;
        if (filtered.length === 0) {
            listEl.innerHTML = '';
            if (empty) empty.hidden = false;
            return;
        }
        if (empty) empty.hidden = true;
        listEl.innerHTML = '';
        filtered.forEach(n => {
            const card = document.createElement('div');
            card.className = 'note-item';
            card.innerHTML = `
                <div class="ni-text">${escapeHtml(n.text)}</div>
                <div class="ni-meta">
                    <span class="ni-source">📄 ${escapeHtml(n.source || n.sourceUrl || '未知来源')}</span>
                    <span>${formatTime(n.timestamp)} · <span class="ni-del" data-id="${n.id}">删除</span></span>
                </div>
            `;
            card.querySelector('.ni-del').onclick = (e) => {
                e.stopPropagation();
                store.deleteNote(n.id);
                renderNotes(filter);
                showToast('已删除');
            };
            if (n.sourceUrl) {
                card.style.cursor = 'pointer';
                card.onclick = () => openWebview(n.sourceUrl, -1);
            }
            listEl.appendChild(card);
        });
    }

    function setupNotesPage() {
        $('#notes-filter').addEventListener('input', (e) => {
            renderNotes(e.target.value.trim());
        });
        $('#clear-notes-btn').onclick = async() => {
            const ok = await confirmDialog('清空笔记', '确定要清空所有笔记吗？此操作不可撤销。');
            if (ok) {
                store.clearNotes();
                renderNotes();
                showToast('笔记已清空');
            }
        };
    }

    // ============ 聚合搜索 ============
    let aggregatedCache = [];
    const CORS_PROXY = 'https://corsproxy.io/?url=';

    function renderAggregated() {
        const listEl = $('#aggregated-list');
        const empty = $('#aggregated-empty');
        const meta = $('#aggregated-meta');
        if (!listEl) return;
        if (aggregatedCache.length === 0) {
            listEl.innerHTML = '';
            if (empty) empty.hidden = false;
            if (meta) meta.textContent = '';
            return;
        }
        if (empty) empty.hidden = true;
        if (meta) meta.textContent = `${aggregatedCache.length} 条`;
        listEl.innerHTML = '';
        aggregatedCache.forEach(item => {
            const card = document.createElement('div');
            card.className = 'aggregated-item';
            card.innerHTML = `
                <span class="ai-engine-tag" style="background:${item.engineColor}">${escapeHtml(item.engineName)}</span>
                <div class="ai-title">${escapeHtml(item.title)}</div>
                <div class="ai-url">${escapeHtml(item.url)}</div>
            `;
            card.onclick = () => openWebview(item.url, -1);
            listEl.appendChild(card);
        });
    }

    function setupAggregatedPage() {
        $('#aggregated-filter').addEventListener('input', (e) => {
            const filter = e.target.value.trim().toLowerCase();
            const listEl = $('#aggregated-list');
            listEl.querySelectorAll('.aggregated-item').forEach(card => {
                const text = card.textContent.toLowerCase();
                card.style.display = (!filter || text.includes(filter)) ? '' : 'none';
            });
        });
        $('#parallel-aggregate-btn').onclick = async() => {
            const q = $('#parallel-input').value.trim();
            if (!q) { showToast('请输入关键词'); return; }
            await runAggregatedSearch(q);
        };
    }

    async function runAggregatedSearch(query) {
        const selected = store.getParallelEngines();
        if (selected.length === 0) { showToast('请先在并行搜索中选择引擎'); return; }
        showToast(`⏳ 正在聚合 ${selected.length} 个引擎...`);
        store.addSearchHistory(query);
        const allEngines = getAllEngines();
        const all = [];
        const tasks = selected.map(async(id) => {
            const engine = allEngines.find(e => e.id === id);
            if (!engine) return;
            const results = await fetchEngineResults(engine, query);
            all.push(...results);
        });
        await Promise.all(tasks);
        // 去重：URL 规范化
        const seen = new Set();
        const deduped = [];
        all.forEach(item => {
            const key = item.url.replace(/[#?].*$/, '').replace(/\/$/, '');
            if (seen.has(key)) return;
            seen.add(key);
            deduped.push(item);
        });
        aggregatedCache = deduped;
        renderAggregated();
        navigate('aggregated');
        showToast(`✅ 聚合 ${deduped.length} 条结果`);
    }

    async function fetchEngineResults(engine, query) {
        const url = engine.searchUrl + encodeURIComponent(query);
        try {
            const res = await fetchWithCorsFallback(url, {
                headers: { 'Accept': 'text/html' }
            });
            const html = await res.text();
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            // 通用提取：找所有外链
            const links = [];
            const selectors = [
                'a.result[href]', 'a.c-container[href]', 'a[href*="http"]',
                '.result h3 a', '.c-container h3 a', 'a.news_title',
                '.result-op h3 a', '.b_algo h2 a', '.b_caption p a'
            ];
            const seen = new Set();
            doc.querySelectorAll('a').forEach(a => {
                const href = a.getAttribute('href') || '';
                // 百度跳转
                if (href.startsWith('http://www.baidu.com/link') || href.includes('baidu.com/link')) {
                    // 无法解码，跳过
                    return;
                }
                if (!href.startsWith('http')) return;
                if (seen.has(href)) return;
                const title = (a.innerText || a.textContent || '').trim();
                if (title.length < 4) return;
                if (title.includes('百度') && title.length < 8) return;
                seen.add(href);
                links.push({
                    title: title.slice(0, 100),
                    url: href,
                    engineName: engine.name,
                    engineColor: engine.color
                });
            });
            return links.slice(0, 10);
        } catch (e) {
            showToast(`❌ ${engine.name} 抓取失败`);
            return [];
        }
    }

    // ============ 数据统计仪表盘 ============
    function renderStats() {
        const history = store.getHistory();
        const bookmarks = store.getBookmarks();
        const windows = store.getWindows();
        const laterList = store.getLaterList();
        const searchHistory = store.getSearchHistory();

        const setStat = (id, val) => {
            const el = $('#' + id);
            if (el) el.textContent = val;
        };
        setStat('stat-history', history.length);
        setStat('stat-bookmarks', bookmarks.length);
        setStat('stat-windows', windows.length);
        setStat('stat-later', laterList.length);
        setStat('stat-search', searchHistory.length);

        // 7 天趋势
        drawTrendChart(history);
        // 引擎占比
        drawEnginePie(history);
        // 热力图
        drawHeatmap(history);

        // Top 站点
        const sites = {};
        history.forEach(h => {
            try {
                const host = new URL(h.url).hostname;
                sites[host] = (sites[host] || 0) + 1;
            } catch {}
        });
        const topSites = Object.entries(sites).sort((a, b) => b[1] - a[1]).slice(0, 10);
        const topEl = $('#stats-top-sites');
        if (topEl) {
            topEl.innerHTML = topSites.length === 0
                ? '<div class="empty-state">暂无数据</div>'
                : topSites.map(([host, count]) => `
                    <div class="list-item-card">
                        <div class="lic-icon" style="background:var(--primary)">🌐</div>
                        <div class="lic-body">
                            <div class="lic-title">${escapeHtml(host)}</div>
                            <div class="lic-url">访问 ${count} 次</div>
                        </div>
                    </div>
                `).join('');
        }
    }

    function drawTrendChart(history) {
        const canvas = $('#chart-trend');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const W = canvas.width = canvas.offsetWidth * 2;
        const H = canvas.height = 240;
        ctx.scale(1, 1);
        ctx.clearRect(0, 0, W, H);

        // 近 7 天
        const days = [];
        const labels = [];
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        for (let i = 6; i >= 0; i--) {
            const d = new Date(today);
            d.setDate(d.getDate() - i);
            const next = new Date(d);
            next.setDate(d.getDate() + 1);
            const count = history.filter(h => h.timestamp >= d.getTime() && h.timestamp < next.getTime()).length;
            days.push(count);
            labels.push(`${d.getMonth() + 1}/${d.getDate()}`);
        }
        const max = Math.max(...days, 1);
        const padding = 40;
        const chartW = W - padding * 2;
        const chartH = H - padding * 2;
        // 坐标轴
        ctx.strokeStyle = '#E0E0E0';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(padding, padding);
        ctx.lineTo(padding, H - padding);
        ctx.lineTo(W - padding, H - padding);
        ctx.stroke();
        // 折线
        ctx.strokeStyle = getComputedStyle(document.documentElement).getPropertyValue('--primary').trim() || '#2196F3';
        ctx.lineWidth = 3;
        ctx.beginPath();
        days.forEach((v, i) => {
            const x = padding + (chartW / (days.length - 1)) * i;
            const y = H - padding - (v / max) * chartH;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });
        ctx.stroke();
        // 点
        ctx.fillStyle = ctx.strokeStyle;
        days.forEach((v, i) => {
            const x = padding + (chartW / (days.length - 1)) * i;
            const y = H - padding - (v / max) * chartH;
            ctx.beginPath();
            ctx.arc(x, y, 5, 0, Math.PI * 2);
            ctx.fill();
            // 数值
            ctx.fillStyle = '#757575';
            ctx.font = '20px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(v, x, y - 12);
            ctx.fillStyle = ctx.strokeStyle;
            // 日期标签
            ctx.fillStyle = '#9E9E9E';
            ctx.font = '18px sans-serif';
            ctx.fillText(labels[i], x, H - padding + 24);
            ctx.fillStyle = ctx.strokeStyle;
        });
    }

    function drawEnginePie(history) {
        const canvas = $('#chart-engine');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const W = canvas.width = canvas.offsetWidth * 2;
        const H = canvas.height = 240;
        ctx.clearRect(0, 0, W, H);

        const counts = {};
        const allEngines = getAllEngines();
        allEngines.forEach(e => counts[e.id] = 0);
        history.forEach(h => {
            const engine = findEngine(h.url);
            if (engine) counts[engine.id]++;
        });
        const total = Object.values(counts).reduce((a, b) => a + b, 0);
        if (total === 0) {
            ctx.fillStyle = '#9E9E9E';
            ctx.font = '20px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('暂无数据', W / 2, H / 2);
            return;
        }
        const cx = W / 2, cy = H / 2, r = 90;
        let start = -Math.PI / 2;
        allEngines.forEach(e => {
            const v = counts[e.id];
            if (v === 0) return;
            const end = start + (v / total) * Math.PI * 2;
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.arc(cx, cy, r, start, end);
            ctx.closePath();
            ctx.fillStyle = e.color;
            ctx.fill();
            start = end;
        });
        // 中心空心
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--background').trim() || '#FAFAFA';
        ctx.beginPath();
        ctx.arc(cx, cy, r * 0.55, 0, Math.PI * 2);
        ctx.fill();
        // 中心文字
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--text-primary').trim() || '#212121';
        ctx.font = 'bold 28px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(total, cx, cy - 10);
        ctx.font = '18px sans-serif';
        ctx.fillText('总访问', cx, cy + 18);
    }

    // ============ 历史热力图（近 12 周，GitHub 风格） ============
    function drawHeatmap(history) {
        const container = $('#heatmap');
        if (!container) return;
        container.innerHTML = '';

        // 按日聚合
        const dayMap = {};
        history.forEach(h => {
            const d = new Date(h.timestamp);
            d.setHours(0, 0, 0, 0);
            const key = d.getTime();
            dayMap[key] = (dayMap[key] || 0) + 1;
        });

        // 近 12 周 = 84 天
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const days = [];
        for (let i = 83; i >= 0; i--) {
            const d = new Date(today);
            d.setDate(d.getDate() - i);
            const key = d.getTime();
            const count = dayMap[key] || 0;
            days.push({ date: d, count });
        }

        // 最大值
        const max = Math.max(...days.map(d => d.count), 1);

        days.forEach(d => {
            const cell = document.createElement('div');
            let level = '';
            if (d.count > 0) {
                const ratio = d.count / max;
                if (ratio > 0.75) level = 'l4';
                else if (ratio > 0.5) level = 'l3';
                else if (ratio > 0.25) level = 'l2';
                else level = 'l1';
            }
            cell.className = 'heatmap-cell ' + level;
            const dateStr = `${d.date.getFullYear()}-${String(d.date.getMonth() + 1).padStart(2, '0')}-${String(d.date.getDate()).padStart(2, '0')}`;
            cell.dataset.tip = `${dateStr} · ${d.count} 次`;
            container.appendChild(cell);
        });

        // 图例
        const legend = document.createElement('div');
        legend.className = 'heatmap-legend';
        legend.innerHTML = `
            <span>少</span>
            <div class="heatmap-cell"></div>
            <div class="heatmap-cell l1"></div>
            <div class="heatmap-cell l2"></div>
            <div class="heatmap-cell l3"></div>
            <div class="heatmap-cell l4"></div>
            <span>多</span>
        `;
        container.appendChild(legend);
    }

    // ============ 语音搜索 ============
    let recognition = null;
    function setupVoiceSearch() {
        const btn = $('#search-voice');
        if (!btn) return;
        const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SR) {
            btn.onclick = () => showToast('当前浏览器不支持语音搜索');
            return;
        }
        recognition = new SR();
        recognition.lang = 'zh-CN';
        recognition.continuous = false;
        recognition.interimResults = false;

        btn.onclick = () => {
            try {
                recognition.start();
                btn.classList.add('recording');
                showToast('🎤 请说话...');
            } catch {
                showToast('已在录音中');
            }
        };
        recognition.onresult = (e) => {
            const text = e.results[0][0].transcript;
            $('#search-input').value = text;
            showToast(`识别：${text}`);
            handleSearch();
        };
        recognition.onerror = (e) => {
            showToast('语音识别失败：' + (e.error || '未知错误'));
        };
        recognition.onend = () => {
            btn.classList.remove('recording');
        };
    }

    // ============ 全局快捷键 ============
    function setupGlobalShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Ctrl+K / Cmd+K → 聚焦搜索框
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
                e.preventDefault();
                navigate('home');
                setTimeout(() => $('#search-input').focus(), 50);
                return;
            }
            // Ctrl+D / Cmd+D → 收藏当前页
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'd') {
                e.preventDefault();
                if (currentRoute === 'webview' && currentWebview.url) {
                    const added = store.toggleBookmark(
                        currentWebview.title || currentWebview.url,
                        currentWebview.url
                    );
                    updateBookmarkIcon();
                    showToast(added ? '已添加收藏' : '已取消收藏');
                } else {
                    showToast('请先打开一个网页');
                }
                return;
            }
            // Ctrl+T / Cmd+T → 新标签
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 't') {
                e.preventDefault();
                const engine = getAllEngines().find(e => e.id === store.getSelectedEngine()) || ENGINES[0];
                openWebview(engine.searchUrl, -1);
                return;
            }
            // Ctrl+W / Cmd+W → 关闭当前标签
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'w') {
                e.preventDefault();
                if (currentWebview.tabId) closeTab(currentWebview.tabId);
                return;
            }
            // Esc → 返回上一页 / 关闭阅读模式
            if (e.key === 'Escape') {
                const reading = $('#reading-mode');
                if (reading && !reading.hidden) {
                    stopTts();
                    reading.hidden = true;
                    return;
                }
                const findBar = $('#wv-find-bar');
                if (findBar && !findBar.hidden) {
                    findBar.hidden = true;
                    clearFindHighlight();
                    return;
                }
                const pop = $('#note-popover');
                if (pop && !pop.hidden) { pop.hidden = true; return; }
                if (routeStack.length > 1) navigateBack();
                return;
            }
            // / → 快速搜索（非输入框聚焦时）
            if (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) {
                e.preventDefault();
                navigate('home');
                setTimeout(() => $('#search-input').focus(), 50);
                return;
            }
        });
    }

    // ============ 初始化 ============
    function init() {
        initTheme();
        applyDarkSchedule();
        renderHome();
        renderAiSummary('');
        setupWebview();
        setupFindInPage();
        setupWindowsPage();
        setupHistoryPage();
        setupBookmarksPage();
        setupProfilePage();
        setupSettingsPage();
        setupSearchSuggest();
        setupBottomNav();
        setupBackButtons();
        setupParallelSearch();
        setupAggregatedPage();
        setupVoiceSearch();
        setupLaterPage();
        setupNotesPage();
        setupNotesPopover();
        setupGlobalShortcuts();
        setupRssPage();
        setupPasswordsPage();
        setupActivityMonitor();
        setupErrorMonitor();
        handlePwaLaunch();
        registerCustomProtocol();
        setupLaunchQueue();
        setupFileHandler();
        // v1.8.0: i18n 初始化
        if (i18n) {
            i18n.apply();
            const langSel = $('#language-select');
            if (langSel) {
                langSel.value = i18n.lang;
                langSel.onchange = (e) => {
                    i18n.switch(e.target.value);
                    showToast(e.target.value === 'en' ? 'Language: English' : '语言：中文');
                };
            }
        }
        renderTabs();

        $('#search-btn').onclick = handleSearch;
        $('#search-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') handleSearch();
        });
        $('#ai-copy-btn').onclick = copyAiReport;

        // 浏览器返回键支持
        window.addEventListener('popstate', () => navigateBack());
    }

    // v2.1.0: 暴露 app 内部函数供 app-sync.js / app-ai.js 跨模块调用
    // （同步/AI 仅在用户触发时运行，此时下列函数均已定义）
    window.MSBApp = {
        save, load,
        showToast,
        fetchWithCorsFallback,
        applyTheme, applyThemeColor, applyDarkSchedule,
        renderHome, renderProfile, renderTabs,
        navigate, handleSearch,
    };

    document.addEventListener('DOMContentLoaded', init);
})();
