/**
 * MultiSearch Browser · Web 版
 * 纯前端实现，localStorage 持久化数据，无后端依赖。
 * 复刻 Android 端核心功能：7 搜索引擎 / 多窗口 / 书签 / 历史 / 头像。
 * v1.1.0 增强：搜索联想 / 设置页 / 深色模式 / 列表过滤 / 窗口重命名 / 数据导出导入 / PWA
 */
(function () {
    'use strict';

    // ============ 常量 ============
    const ENGINES = [
        { id: 'baidu',    name: '百度',  searchUrl: 'https://www.baidu.com/s?wd=',    color: '#2932E1' },
        { id: 'sogou',    name: '搜狗',  searchUrl: 'https://www.sogou.com/web?query=', color: '#FF6600' },
        { id: 'bilibili', name: 'B站',   searchUrl: 'https://search.bilibili.com/all?keyword=', color: '#FB7299' },
        { id: 'douyin',   name: '抖音',  searchUrl: 'https://www.douyin.com/search/', color: '#161823' },
        { id: 'bing',     name: '必应',  searchUrl: 'https://cn.bing.com/search?q=',  color: '#0C8484' },
        { id: 'doubao',   name: '豆包',  searchUrl: 'https://www.doubao.com/search?q=', color: '#4E6EF2' },
        { id: 'qianwen',  name: '千问',  searchUrl: 'https://tongyi.aliyun.com/qianwen?q=', color: '#615CED' },
    ];

    const STORAGE_KEYS = {
        engine: 'msb_selected_engine',
        windows: 'msb_windows',
        bookmarks: 'msb_bookmarks',
        history: 'msb_history',
        avatar: 'msb_avatar',
        currentWindowId: 'msb_current_window_id',
        searchHistory: 'msb_search_history',
        theme: 'msb_theme',
    };

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

    // 记录最近一次搜索关键词，用于 AI 面板渲染与复制
    let lastAiQuery = '';

    // ============ 工具函数 ============
    const $ = (sel) => document.querySelector(sel);
    const $$ = (sel) => document.querySelectorAll(sel);

    const load = (key, def = null) => {
        try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : def; }
        catch { return def; }
    };
    const save = (key, val) => localStorage.setItem(key, JSON.stringify(val));

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
        const cleanup = () => {
            modal.hidden = true;
            ok.onclick = null;
            cancel.onclick = null;
        };
        ok.onclick = () => { cleanup(); resolve(true); };
        cancel.onclick = () => { cleanup(); resolve(false); };
    });

    /** 输入对话框，用于窗口重命名等场景 */
    const inputDialog = (title, defaultValue = '') => new Promise((resolve) => {
        const modal = $('#input-modal');
        $('#input-modal-title').textContent = title;
        const field = $('#input-modal-field');
        field.value = defaultValue;
        modal.hidden = false;
        setTimeout(() => { field.focus(); field.select(); }, 50);
        const ok = $('#input-modal-ok'), cancel = $('#input-modal-cancel');
        const cleanup = () => {
            modal.hidden = true;
            ok.onclick = null;
            cancel.onclick = null;
            field.onkeydown = null;
        };
        ok.onclick = () => { cleanup(); resolve(field.value.trim()); };
        cancel.onclick = () => { cleanup(); resolve(null); };
        field.onkeydown = (e) => {
            if (e.key === 'Enter') { cleanup(); resolve(field.value.trim()); }
            if (e.key === 'Escape') { cleanup(); resolve(null); }
        };
    });

    // ============ 数据访问层 ============
    const store = {
        getSelectedEngine: () => load(STORAGE_KEYS.engine, 'baidu'),
        setSelectedEngine: (id) => save(STORAGE_KEYS.engine, id),

        // 窗口
        getWindows: () => load(STORAGE_KEYS.windows, []),
        saveWindows: (list) => save(STORAGE_KEYS.windows, list),
        addWindow: (title, url) => {
            const list = store.getWindows();
            const win = { id: Date.now(), title, url, timestamp: Date.now() };
            list.unshift(win);
            store.saveWindows(list);
            return win;
        },
        updateWindow: (id, patch) => {
            const list = store.getWindows();
            const idx = list.findIndex(w => w.id === id);
            if (idx >= 0) {
                list[idx] = { ...list[idx], ...patch, timestamp: Date.now() };
                store.saveWindows(list);
            }
        },
        deleteWindow: (id) => {
            store.saveWindows(store.getWindows().filter(w => w.id !== id));
        },

        // 书签
        getBookmarks: () => load(STORAGE_KEYS.bookmarks, []),
        saveBookmarks: (list) => save(STORAGE_KEYS.bookmarks, list),
        isBookmarked: (url) => store.getBookmarks().some(b => b.url === url),
        toggleBookmark: (title, url) => {
            const list = store.getBookmarks();
            const idx = list.findIndex(b => b.url === url);
            if (idx >= 0) {
                list.splice(idx, 1);
                store.saveBookmarks(list);
                return false;
            }
            list.unshift({ id: Date.now(), title, url, timestamp: Date.now() });
            store.saveBookmarks(list);
            return true;
        },
        deleteBookmark: (url) => {
            store.saveBookmarks(store.getBookmarks().filter(b => b.url !== url));
        },

        // 历史（UPSERT 语义）
        getHistory: () => load(STORAGE_KEYS.history, []),
        saveHistory: (list) => save(STORAGE_KEYS.history, list),
        addHistory: (title, url) => {
            const list = store.getHistory();
            const idx = list.findIndex(h => h.url === url);
            if (idx >= 0) {
                list[idx] = { ...list[idx], title, timestamp: Date.now() };
            } else {
                list.unshift({ id: Date.now(), title, url, timestamp: Date.now() });
            }
            if (list.length > 500) list.length = 500;
            store.saveHistory(list);
        },
        deleteHistory: (id) => {
            store.saveHistory(store.getHistory().filter(h => h.id !== id));
        },
        clearHistory: () => save(STORAGE_KEYS.history, []),

        // 头像
        getAvatar: () => load(STORAGE_KEYS.avatar, null),
        setAvatar: (dataUrl) => save(STORAGE_KEYS.avatar, dataUrl),

        // 搜索历史（用于搜索联想）
        getSearchHistory: () => load(STORAGE_KEYS.searchHistory, []),
        addSearchHistory: (query) => {
            const list = store.getSearchHistory().filter(q => q !== query);
            list.unshift(query);
            if (list.length > 20) list.length = 20;
            save(STORAGE_KEYS.searchHistory, list);
        },
        deleteSearchHistory: (query) => {
            save(STORAGE_KEYS.searchHistory, store.getSearchHistory().filter(q => q !== query));
        },
        clearSearchHistory: () => save(STORAGE_KEYS.searchHistory, []),

        // 主题
        getTheme: () => load(STORAGE_KEYS.theme, 'light'),
        setTheme: (theme) => save(STORAGE_KEYS.theme, theme),

        // 清除所有数据
        clearAllData: () => {
            Object.values(STORAGE_KEYS).forEach(key => localStorage.removeItem(key));
        },
    };

    // ============ 深色模式 ============
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        const toggle = $('#dark-mode-toggle');
        if (toggle) toggle.checked = (theme === 'dark');
        // 同步 theme-color meta
        const meta = document.querySelector('meta[name="theme-color"]');
        if (meta) meta.content = theme === 'dark' ? '#1A237E' : '#2196F3';
    }

    function initTheme() {
        applyTheme(store.getTheme());
    }

    function toggleTheme() {
        const current = store.getTheme();
        const next = current === 'dark' ? 'light' : 'dark';
        store.setTheme(next);
        applyTheme(next);
    }

    // ============ 路由 ============
    const routes = ['home', 'windows', 'webview', 'profile', 'history', 'bookmarks', 'settings'];
    let currentRoute = 'home';
    let routeStack = ['home'];

    function navigate(route, push = true) {
        if (!routes.includes(route)) return;
        if (push && route !== currentRoute) routeStack.push(route);
        currentRoute = route;
        routes.forEach(r => {
            $(`#page-${r}`).classList.toggle('active', r === route);
        });
        const tabMap = { home: 'home', windows: 'windows', profile: 'profile' };
        $$('.tab').forEach(t => {
            t.classList.toggle('active', t.dataset.tab === tabMap[route]);
        });
        if (route === 'windows') renderWindows();
        if (route === 'history') { renderHistory(); $('#history-filter').value = ''; }
        if (route === 'bookmarks') { renderBookmarks(); $('#bookmarks-filter').value = ''; }
        if (route === 'profile') renderProfile();
        if (route === 'settings') renderSettings();
        window.scrollTo(0, 0);
    }

    function navigateBack() {
        if (routeStack.length > 1) {
            routeStack.pop();
            navigate(routeStack[routeStack.length - 1], false);
        }
    }

    // ============ 首页 ============
    function renderHome() {
        const selected = store.getSelectedEngine();
        const chipsEl = $('#engine-chips');
        chipsEl.innerHTML = '';
        ENGINES.forEach(engine => {
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
        ENGINES.forEach(engine => {
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
    }

    function handleSearch() {
        const input = $('#search-input');
        const query = input.value.trim();
        if (!query) {
            showToast('请输入搜索内容');
            return;
        }
        // 记录搜索历史
        store.addSearchHistory(query);
        // 更新 AI 多源研讨面板
        renderAiSummary(query);
        const engine = ENGINES.find(e => e.id === store.getSelectedEngine()) || ENGINES[0];
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

    // ============ WebView ============
    let currentWebview = { url: '', title: '', windowId: -1 };
    let frameLoadTimeout = null;

    function openWebview(url, windowId = -1) {
        currentWebview = { url, title: '', windowId };
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
            if (currentWebview.url) {
                store.addHistory(title || currentWebview.url, currentWebview.url);
            }
            if (currentWebview.windowId >= 0) {
                store.updateWindow(currentWebview.windowId, {
                    url: currentWebview.url,
                    title: title || currentWebview.url,
                });
            }
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
        const engine = ENGINES.find(e => e.id === store.getSelectedEngine()) || ENGINES[0];
        return engine.searchUrl + encodeURIComponent(input);
    }

    // ============ 多窗口 ============
    function renderWindows() {
        const list = store.getWindows();
        $('#window-count').textContent = `${list.length} 个窗口`;
        $('#windows-empty').hidden = list.length > 0;
        const listEl = $('#windows-list');
        listEl.innerHTML = '';
        list.forEach(win => {
            const card = document.createElement('div');
            card.className = 'list-item-card';
            const engine = ENGINES.find(e => win.url.startsWith(e.searchUrl));
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
            card.querySelector('[data-act="rename"]').onclick = async (e) => {
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
            const engine = ENGINES.find(e => h.url.startsWith(e.searchUrl));
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
            card.oncontextmenu = async (e) => {
                e.preventDefault();
                const ok = await confirmDialog('删除记录', '确定要删除这条记录吗？');
                if (ok) { store.deleteHistory(h.id); renderHistory(filter); }
            };
            listEl.appendChild(card);
        });
    }

    function setupHistoryPage() {
        $('#clear-history-btn').onclick = async () => {
            const ok = await confirmDialog('清空历史', '确定要清空所有浏览历史吗？此操作不可撤销。');
            if (ok) { store.clearHistory(); renderHistory(); showToast('已清空历史'); }
        };
        $('#history-filter').addEventListener('input', (e) => {
            renderHistory(e.target.value.trim());
        });
    }

    // ============ 书签 ============
    function renderBookmarks(filter = '') {
        const list = store.getBookmarks();
        const filtered = filter
            ? list.filter(b => (b.title + b.url).toLowerCase().includes(filter.toLowerCase()))
            : list;
        $('#bookmarks-empty').hidden = filtered.length > 0;
        const listEl = $('#bookmarks-list');
        listEl.innerHTML = '';
        filtered.forEach(b => {
            const card = document.createElement('div');
            card.className = 'list-item-card';
            const engine = ENGINES.find(e => b.url.startsWith(e.searchUrl));
            const iconChar = engine ? engine.name[0] : '🌐';
            const iconColor = engine ? engine.color : '#FF5722';
            card.innerHTML = `
                <div class="lic-icon" style="background:${iconColor}">${iconChar}</div>
                <div class="lic-body">
                    <div class="lic-title">${escapeHtml(b.title || '无标题')}</div>
                    <div class="lic-url">${escapeHtml(b.url)}</div>
                </div>
            `;
            card.onclick = () => openWebview(b.url, -1);
            card.oncontextmenu = async (e) => {
                e.preventDefault();
                const ok = await confirmDialog('删除收藏', '确定要删除这个收藏吗？');
                if (ok) { store.deleteBookmark(b.url); renderBookmarks(filter); }
            };
            listEl.appendChild(card);
        });
    }

    function setupBookmarksPage() {
        $('#bookmarks-filter').addEventListener('input', (e) => {
            renderBookmarks(e.target.value.trim());
        });
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
                else if (action === 'settings') navigate('settings');
            };
        });
    }

    // ============ 设置页 ============
    function renderSettings() {
        // 默认引擎下拉
        const select = $('#setting-default-engine');
        select.innerHTML = '';
        ENGINES.forEach(e => {
            const opt = document.createElement('option');
            opt.value = e.id;
            opt.textContent = e.name;
            opt.selected = e.id === store.getSelectedEngine();
            select.appendChild(opt);
        });
        // 深色模式开关
        $('#dark-mode-toggle').checked = store.getTheme() === 'dark';
    }

    function setupSettingsPage() {
        // 默认引擎切换
        $('#setting-default-engine').onchange = (e) => {
            store.setSelectedEngine(e.target.value);
            showToast('默认引擎已切换');
        };
        // 深色模式
        $('#dark-mode-toggle').onchange = (e) => {
            store.setTheme(e.target.checked ? 'dark' : 'light');
            applyTheme(store.getTheme());
        };
        // 导出数据
        $('[data-action="export"]').onclick = () => exportData();
        // 导入数据
        $('[data-action="import"]').onclick = () => $('#import-file').click();
        $('#import-file').onchange = (e) => importData(e.target.files[0]);
        // 清除所有数据
        $('[data-action="clear-all"]').onclick = async () => {
            const ok = await confirmDialog('清除所有数据', '将清除所有书签、历史、窗口、搜索历史和头像。此操作不可撤销，确定继续吗？');
            if (ok) {
                store.clearAllData();
                initTheme();
                renderHome();
                renderProfile();
                renderAiSummary('');
                showToast('所有数据已清除');
            }
        };
    }

    // ============ 数据导出/导入 ============
    function exportData() {
        const data = {
            version: '1.1.0',
            exportTime: new Date().toISOString(),
            engine: store.getSelectedEngine(),
            windows: store.getWindows(),
            bookmarks: store.getBookmarks(),
            history: store.getHistory(),
            searchHistory: store.getSearchHistory(),
            avatar: store.getAvatar(),
            theme: store.getTheme(),
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
            if (data.history) store.saveHistory(data.history);
            if (data.searchHistory) save(STORAGE_KEYS.searchHistory, data.searchHistory);
            if (data.avatar) store.setAvatar(data.avatar);
            if (data.theme) { store.setTheme(data.theme); applyTheme(data.theme); }
            renderHome();
            renderProfile();
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
        ENGINES.forEach(engine => {
            const p = AI_PERSPECTIVES[engine.id];
            if (!p) return;
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
        const lines = ENGINES.map(e => {
            const p = AI_PERSPECTIVES[e.id];
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

    // ============ 初始化 ============
    function init() {
        initTheme();
        renderHome();
        renderAiSummary('');
        setupWebview();
        setupWindowsPage();
        setupHistoryPage();
        setupBookmarksPage();
        setupProfilePage();
        setupSettingsPage();
        setupSearchSuggest();
        setupBottomNav();
        setupBackButtons();

        $('#search-btn').onclick = handleSearch;
        $('#search-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') handleSearch();
        });
        $('#ai-copy-btn').onclick = copyAiReport;

        // 浏览器返回键支持
        window.addEventListener('popstate', () => navigateBack());
    }

    document.addEventListener('DOMContentLoaded', init);
})();
