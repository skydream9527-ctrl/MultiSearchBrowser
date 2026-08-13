/**
 * MultiSearch Browser · 数据访问层
 * v2.1.0: 从 app.js 抽离，封装所有 localStorage CRUD 操作。
 * 依赖：MSBUtils.safeStorage / MSBConstants.STORAGE_KEYS / CryptoJS
 */
(function () {
    'use strict';

    const K = window.MSBConstants.STORAGE_KEYS;
    const storage = window.MSBUtils.safeStorage;

    // 闭包内 store 自引用，方法间互调用 store.xxx()
    const store = {
        // ============ 基础 ============
        getSelectedEngine: () => storage.get(K.engine, 'baidu'),
        setSelectedEngine: (id) => storage.set(K.engine, id),

        // ============ 多窗口 ============
        getWindows: () => storage.get(K.windows, []),
        saveWindows: (list) => storage.set(K.windows, list),
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
            if (idx >= 0) { list[idx] = { ...list[idx], ...patch, timestamp: Date.now() }; store.saveWindows(list); }
        },
        reorderWindows: (newList) => storage.set(K.windows, newList),
        deleteWindow: (id) => store.saveWindows(store.getWindows().filter(w => w.id !== id)),

        // ============ 书签 ============
        getBookmarks: () => storage.get(K.bookmarks, []),
        saveBookmarks: (list) => storage.set(K.bookmarks, list),
        isBookmarked: (url) => store.getBookmarks().some(b => b.url === url),
        toggleBookmark: (title, url) => {
            const list = store.getBookmarks();
            const idx = list.findIndex(b => b.url === url);
            if (idx >= 0) { list.splice(idx, 1); store.saveBookmarks(list); return false; }
            list.unshift({ id: Date.now(), title, url, timestamp: Date.now(), folder: '' });
            store.saveBookmarks(list);
            return true;
        },
        moveBookmark: (url, folder) => {
            const list = store.getBookmarks();
            const b = list.find(b => b.url === url);
            if (b) { b.folder = folder; store.saveBookmarks(list); }
        },
        deleteBookmark: (url) => store.saveBookmarks(store.getBookmarks().filter(b => b.url !== url)),

        getBookmarkFolders: () => storage.get(K.bookmarkFolders, []),
        addBookmarkFolder: (name) => {
            const list = store.getBookmarkFolders();
            if (!list.includes(name)) { list.push(name); storage.set(K.bookmarkFolders, list); }
        },
        deleteBookmarkFolder: (name) => {
            storage.set(K.bookmarkFolders, store.getBookmarkFolders().filter(f => f !== name));
            const bookmarks = store.getBookmarks().map(b => b.folder === name ? { ...b, folder: '' } : b);
            store.saveBookmarks(bookmarks);
        },

        // ============ 历史 ============
        getHistory: () => storage.get(K.history, []),
        saveHistory: (list) => storage.set(K.history, list),
        addHistory: (title, url) => {
            const list = store.getHistory();
            const idx = list.findIndex(h => h.url === url);
            if (idx >= 0) { list[idx] = { ...list[idx], title, timestamp: Date.now() }; }
            else { list.unshift({ id: Date.now(), title, url, timestamp: Date.now() }); }
            if (list.length > 500) list.length = 500;
            store.saveHistory(list);
        },
        deleteHistory: (id) => store.saveHistory(store.getHistory().filter(h => h.id !== id)),
        clearHistory: () => storage.set(K.history, []),

        // ============ 头像 ============
        getAvatar: () => storage.get(K.avatar, null),
        setAvatar: (dataUrl) => storage.set(K.avatar, dataUrl),

        // ============ 搜索历史 ============
        getSearchHistory: () => storage.get(K.searchHistory, []),
        addSearchHistory: (query) => {
            const list = store.getSearchHistory().filter(q => q !== query);
            list.unshift(query);
            if (list.length > 20) list.length = 20;
            storage.set(K.searchHistory, list);
        },
        deleteSearchHistory: (query) => storage.set(K.searchHistory, store.getSearchHistory().filter(q => q !== query)),
        clearSearchHistory: () => storage.set(K.searchHistory, []),

        // ============ 主题 ============
        getTheme: () => storage.get(K.theme, 'light'),
        setTheme: (theme) => storage.set(K.theme, theme),
        getThemeColor: () => storage.get(K.themeColor, 'blue'),
        setThemeColor: (id) => storage.set(K.themeColor, id),

        // ============ 并行搜索 ============
        getParallelEngines: () => storage.get(K.parallelEngines, ['baidu', 'bing', 'bilibili', 'doubao']),
        setParallelEngines: (list) => storage.set(K.parallelEngines, list),

        // ============ 稍后阅读 ============
        getLaterList: () => storage.get(K.laterList, []),
        addLater: (title, url) => {
            const list = store.getLaterList().filter(l => l.url !== url);
            list.unshift({ id: Date.now(), title, url, timestamp: Date.now() });
            storage.set(K.laterList, list);
        },
        deleteLater: (id) => storage.set(K.laterList, store.getLaterList().filter(l => l.id !== id)),

        // ============ v1.3.0: 划线笔记 ============
        getNotes: () => storage.get(K.notes, []),
        addNote: (text, source, sourceUrl) => {
            const list = store.getNotes();
            list.unshift({ id: Date.now(), text, source: source || '', sourceUrl: sourceUrl || '', timestamp: Date.now() });
            storage.set(K.notes, list);
        },
        deleteNote: (id) => storage.set(K.notes, store.getNotes().filter(n => n.id !== id)),
        clearNotes: () => storage.set(K.notes, []),

        // ============ v1.3.0: 多标签页 ============
        getTabs: () => storage.get(K.tabs, []),
        saveTabs: (list) => storage.set(K.tabs, list),
        addTab: (url = '', title = '新标签') => {
            const list = store.getTabs();
            const tab = { id: Date.now(), url, title, timestamp: Date.now() };
            list.push(tab);
            store.saveTabs(list);
            return tab;
        },
        updateTab: (id, patch) => {
            const list = store.getTabs();
            const idx = list.findIndex(t => t.id === id);
            if (idx >= 0) { list[idx] = { ...list[idx], ...patch }; store.saveTabs(list); }
        },
        deleteTab: (id) => store.saveTabs(store.getTabs().filter(t => t.id !== id)),
        getActiveTabId: () => storage.get(K.activeTabId, null),
        setActiveTabId: (id) => storage.set(K.activeTabId, id),

        // ============ 阅读进度 ============
        getScrollProgress: (url) => {
            const map = storage.get(K.scrollProgress, {});
            return map[url] || 0;
        },
        setScrollProgress: (url, progress) => {
            const map = storage.get(K.scrollProgress, {});
            map[url] = progress;
            storage.set(K.scrollProgress, map);
        },

        // ============ v1.3.1: 自定义引擎 ============
        getCustomEngines: () => storage.get(K.customEngines, []),
        addCustomEngine: (name, searchUrl, color) => {
            const list = store.getCustomEngines();
            const id = 'custom_' + Date.now();
            list.push({ id, name, searchUrl, color: color || '#757575', custom: true });
            storage.set(K.customEngines, list);
            return id;
        },
        deleteCustomEngine: (id) => storage.set(K.customEngines, store.getCustomEngines().filter(e => e.id !== id)),

        // ============ v1.3.1: 暗黑定时 ============
        getDarkSchedule: () => storage.get(K.darkSchedule, 'off'),
        setDarkSchedule: (mode) => storage.set(K.darkSchedule, mode),

        // ============ v1.4.0: RSS ============
        getRssFeeds: () => storage.get(K.rssFeeds, []),
        addRssFeed: (name, url) => {
            const list = store.getRssFeeds();
            const feed = { id: Date.now(), name, url, addedAt: Date.now() };
            list.push(feed);
            storage.set(K.rssFeeds, list);
            return feed;
        },
        deleteRssFeed: (id) => storage.set(K.rssFeeds, store.getRssFeeds().filter(f => f.id !== id)),
        getRssCache: () => storage.get(K.rssCache, []),
        setRssCache: (items) => storage.set(K.rssCache, items),

        // ============ v1.4.0: 密码管理（AES 加密） ============
        getMasterPwdHash: () => storage.get(K.masterPwdHash, null),
        getMasterPwdSalt: () => storage.get(K.masterPwdSalt, null),
        setMasterPwd: (pwd) => {
            const salt = CryptoJS.lib.WordArray.random(128 / 8).toString();
            const hash = CryptoJS.PBKDF2(pwd, salt, { keySize: 256 / 32, iterations: 100000 }).toString();
            storage.set(K.masterPwdHash, hash);
            storage.set(K.masterPwdSalt, salt);
        },
        verifyMasterPwd: (pwd) => {
            const hash = store.getMasterPwdHash();
            const salt = store.getMasterPwdSalt();
            if (!hash || !salt) return false;
            const test = CryptoJS.PBKDF2(pwd, salt, { keySize: 256 / 32, iterations: 100000 }).toString();
            return test === hash;
        },
        getEncryptedPasswords: () => storage.get(K.passwords, null),
        savePasswords: (list, masterPwd) => {
            const salt = store.getMasterPwdSalt() || CryptoJS.lib.WordArray.random(128 / 8).toString();
            const key = CryptoJS.PBKDF2(masterPwd, salt, { keySize: 256 / 32, iterations: 100000 });
            const iv = CryptoJS.lib.WordArray.random(128 / 8);
            const ciphertext = CryptoJS.AES.encrypt(JSON.stringify(list), key, {
                iv: iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7
            }).toString();
            const blob = iv.toString(CryptoJS.enc.Base64) + ':' + ciphertext;
            storage.set(K.passwords, blob);
        },
        loadPasswords: (masterPwd) => {
            const blob = store.getEncryptedPasswords();
            if (!blob) return [];
            try {
                const parts = blob.split(':');
                if (parts.length !== 2) {
                    // 兼容 v1.x 旧格式
                    const bytes = CryptoJS.AES.decrypt(blob, masterPwd);
                    const text = bytes.toString(CryptoJS.enc.Utf8);
                    return text ? JSON.parse(text) : [];
                }
                const iv = CryptoJS.enc.Base64.parse(parts[0]);
                const salt = store.getMasterPwdSalt();
                const key = CryptoJS.PBKDF2(masterPwd, salt, { keySize: 256 / 32, iterations: 100000 });
                const bytes = CryptoJS.AES.decrypt(parts[1], key, {
                    iv: iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7
                });
                const text = bytes.toString(CryptoJS.enc.Utf8);
                return text ? JSON.parse(text) : [];
            } catch { return null; }
        },

        // ============ 同步 / LLM / AI / 翻译 ============
        getSyncConfig: () => storage.get(K.syncConfig, { type: 'off' }),
        setSyncConfig: (cfg) => storage.set(K.syncConfig, cfg),
        getLlmConfig: () => storage.get(K.llmConfig, null),
        setLlmConfig: (cfg) => storage.set(K.llmConfig, cfg),
        getAiSummaryMode: () => storage.get(K.aiSummaryMode, 'local'),
        setAiSummaryMode: (mode) => storage.set(K.aiSummaryMode, mode),
        getTranslateMode: () => storage.get(K.translateMode, 'online'),
        setTranslateMode: (mode) => storage.set(K.translateMode, mode),

        // ============ v1.5.0: 阅读设置 ============
        getReadingSettings: () => storage.get(K.readingSettings, {
            fontFamily: 'system', lineHeight: 1.8, paraGap: 16, theme: 'default', scrollSpeed: 3
        }),
        setReadingSettings: (s) => storage.set(K.readingSettings, s),

        // ============ v1.5.0: 密码自动锁 ============
        getPwdAutoLock: () => storage.get(K.pwdAutoLock, 5),
        setPwdAutoLock: (min) => storage.set(K.pwdAutoLock, min),

        // ============ v1.5.0: 翻译缓存 ============
        getTranslateCache: () => storage.get(K.translateCache, {}),
        setTranslateCache: (obj) => storage.set(K.translateCache, obj),
        addTranslateCache: (key, value) => {
            const cache = storage.get(K.translateCache, {});
            cache[key] = { value, time: Date.now() };
            const keys = Object.keys(cache);
            if (keys.length > 500) {
                keys.sort((a, b) => cache[a].time - cache[b].time);
                for (let i = 0; i < keys.length - 500; i++) delete cache[keys[i]];
            }
            storage.set(K.translateCache, cache);
        },
        clearTranslateCache: () => storage.set(K.translateCache, {}),

        // ============ v1.5.0: 错误日志 ============
        getErrorLog: () => storage.get(K.errorLog, []),
        addErrorLog: (entry) => {
            const log = storage.get(K.errorLog, []);
            log.unshift({ ...entry, time: new Date().toISOString() });
            if (log.length > 100) log.length = 100;
            storage.set(K.errorLog, log);
        },
        clearErrorLog: () => storage.set(K.errorLog, []),

        // ============ v1.6.0: 用户脚本 ============
        getUserScripts: () => storage.get(K.userScripts, []),
        setUserScripts: (list) => storage.set(K.userScripts, list),
        addUserScript: (script) => {
            const list = storage.get(K.userScripts, []);
            list.push({ id: Date.now(), enabled: true, ...script });
            storage.set(K.userScripts, list);
        },
        updateUserScript: (id, patch) => {
            const list = storage.get(K.userScripts, []);
            const idx = list.findIndex(s => s.id === id);
            if (idx >= 0) { list[idx] = { ...list[idx], ...patch }; storage.set(K.userScripts, list); }
        },
        deleteUserScript: (id) => storage.set(K.userScripts, storage.get(K.userScripts, []).filter(s => s.id !== id)),

        // ============ v1.6.0: 广告拦截 ============
        getAdBlockEnabled: () => storage.get(K.adBlockEnabled, true),
        setAdBlockEnabled: (on) => storage.set(K.adBlockEnabled, on),

        // ============ v1.6.0: RSS 已读集合 ============
        getRssReadSet: () => new Set(storage.get(K.rssReadSet, [])),
        markRssRead: (guid) => {
            const set = new Set(storage.get(K.rssReadSet, []));
            set.add(guid);
            storage.set(K.rssReadSet, Array.from(set));
        },
        clearRssRead: () => storage.set(K.rssReadSet, []),

        // ============ 全量清空 ============
        clearAllData: () => { Object.values(K).forEach(key => localStorage.removeItem(key)); },
    };

    window.MSBStore = store;
})();
