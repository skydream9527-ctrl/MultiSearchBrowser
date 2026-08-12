/**
 * MultiSearch Browser · Web 版
 * 纯前端实现，localStorage 持久化数据，无后端依赖。
 * 复刻 Android 端核心功能：7 搜索引擎 / 多窗口 / 书签 / 历史 / 头像。
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
    };

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

    // ============ 数据访问层（模拟 Room DAO） ============
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
            // 限制历史最多 500 条
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
    };

    // ============ 路由 ============
    const routes = ['home', 'windows', 'webview', 'profile', 'history', 'bookmarks'];
    let currentRoute = 'home';
    let routeStack = ['home']; // 用于系统返回键

    function navigate(route, push = true) {
        if (!routes.includes(route)) return;
        if (push && route !== currentRoute) routeStack.push(route);
        currentRoute = route;
        routes.forEach(r => {
            $(`#page-${r}`).classList.toggle('active', r === route);
        });
        // 同步底部 tab 高亮
        const tabMap = { home: 'home', windows: 'windows', profile: 'profile' };
        $$('.tab').forEach(t => {
            t.classList.toggle('active', t.dataset.tab === tabMap[route]);
        });
        // 渲染对应页面
        if (route === 'windows') renderWindows();
        if (route === 'history') renderHistory();
        if (route === 'bookmarks') renderBookmarks();
        if (route === 'profile') renderProfile();
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
        const engine = ENGINES.find(e => e.id === store.getSelectedEngine()) || ENGINES[0];
        const url = engine.searchUrl + encodeURIComponent(query);
        openWebview(url, -1);
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

        // 检测 iframe 是否被阻止：3 秒内未触发 load 事件，则显示兜底
        clearTimeout(frameLoadTimeout);
        frameLoadTimeout = setTimeout(() => {
            try {
                // 若 frame 不可访问，则认为被拦截
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
            // 写入历史
            if (currentWebview.url) {
                store.addHistory(title || currentWebview.url, currentWebview.url);
            }
            // 回写窗口
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
        // 域名识别：含 . 且无空格
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
                <button class="lic-close" data-id="${win.id}" aria-label="关闭">✕</button>
            `;
            card.querySelector('.lic-body').onclick = () => openWebview(win.url, win.id);
            card.querySelector('.lic-close').onclick = (e) => {
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
    function renderHistory() {
        const list = store.getHistory();
        $('#history-empty').hidden = list.length > 0;
        const listEl = $('#history-list');
        listEl.innerHTML = '';
        list.forEach(h => {
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
                if (ok) { store.deleteHistory(h.id); renderHistory(); }
            };
            listEl.appendChild(card);
        });
    }

    function setupHistoryPage() {
        $('#clear-history-btn').onclick = async () => {
            const ok = await confirmDialog('清空历史', '确定要清空所有浏览历史吗？此操作不可撤销。');
            if (ok) { store.clearHistory(); renderHistory(); showToast('已清空历史'); }
        };
    }

    // ============ 书签 ============
    function renderBookmarks() {
        const list = store.getBookmarks();
        $('#bookmarks-empty').hidden = list.length > 0;
        const listEl = $('#bookmarks-list');
        listEl.innerHTML = '';
        list.forEach(b => {
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
                if (ok) { store.deleteBookmark(b.url); renderBookmarks(); }
            };
            listEl.appendChild(card);
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
                else if (action === 'settings') showToast('设置功能即将上线');
            };
        });
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

    // ============ 初始化 ============
    function init() {
        renderHome();
        setupWebview();
        setupWindowsPage();
        setupHistoryPage();
        setupProfilePage();
        setupBottomNav();
        setupBackButtons();

        $('#search-btn').onclick = handleSearch;
        $('#search-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') handleSearch();
        });

        // 浏览器返回键支持（hash 路由）
        window.addEventListener('popstate', () => navigateBack());
    }

    document.addEventListener('DOMContentLoaded', init);
})();
