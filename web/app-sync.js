/**
 * MultiSearch Browser · 跨设备同步模块
 * v2.1.0: 从 app.js 抽离，封装 WebDAV / GitHub Gist 同步逻辑。
 * 依赖：MSBUtils / MSBConstants / MSBStore
 *      运行时还依赖 app.js 通过 window.MSBApp 暴露的内部函数
 *      （showToast / fetchWithCorsFallback / applyTheme 等），因 app.js 在本文件之后加载，
 *      这些依赖在各同步函数实际被调用时（用户触发）才解析，故安全。
 */
(function () {
    'use strict';

    const U = window.MSBUtils || {};
    const STORAGE_KEYS = window.MSBConstants.STORAGE_KEYS;
    const store = window.MSBStore;

    // 复用 app.js 的 safeStorage 包装（与 app.js 中 save/load 实现一致）
    // 保持 applySyncData / exportDataRaw 内部 save(...)/load(...) 调用不变
    const load = (key, def = null) => U.safeStorage.get(key, def);
    const save = (key, val) => U.safeStorage.set(key, val);

    // ============ v1.4.0: 跨设备同步 ============

    // 根据 syncConfig.type 分发同步：下载远程 → 合并 → 上传合并结果
    async function syncNow() {
        const { showToast } = window.MSBApp;
        const cfg = store.getSyncConfig();
        if (cfg.type === 'off' || !cfg.url) {
            showToast('请先配置同步');
            return;
        }
        showToast('🔄 正在同步...');
        try {
            if (cfg.type === 'webdav') {
                await syncWebDAV(cfg);
            } else if (cfg.type === 'gist') {
                await syncGist(cfg);
            }
            // 同步完成后更新 lastSync 时间戳
            cfg.lastSync = Date.now();
            store.setSyncConfig(cfg);
            showToast('✅ 同步成功');
        } catch (e) {
            showToast('❌ 同步失败：' + (e && e.message ? e.message : '网络错误'));
        }
    }

    // WebDAV 同步：PUT/GET {url}/msb-backup.json，使用 Basic Auth
    // WebDAV 服务器通常不支持 CORS，需通过 fetchWithCorsFallback 走代理
    async function syncWebDAV(cfg) {
        const { fetchWithCorsFallback } = window.MSBApp;
        const baseUrl = cfg.url.replace(/\/$/, '');
        const fileUrl = `${baseUrl}/msb-backup.json`;
        const auth = 'Basic ' + btoa(cfg.user + ':' + cfg.pass);
        const headers = {
            'Authorization': auth,
            'Content-Type': 'application/json'
        };

        // 1. 下载远程数据（首次同步文件不存在时忽略错误）
        let remoteData = null;
        try {
            const getRes = await fetchWithCorsFallback(fileUrl, { method: 'GET', headers });
            if (getRes && getRes.ok) {
                const text = await getRes.text();
                if (text) {
                    try { remoteData = JSON.parse(text); } catch {}
                }
            }
        } catch {}

        // 2. 合并本地与远程
        const localData = exportDataRaw();
        const merged = mergeSyncData(localData, remoteData);

        // 3. 上传合并结果
        const putRes = await fetchWithCorsFallback(fileUrl, {
            method: 'PUT',
            headers,
            body: JSON.stringify(merged)
        });
        if (!putRes || !putRes.ok) {
            throw new Error(`上传失败 HTTP ${putRes ? putRes.status : '网络错误'}`);
        }

        // 4. 将合并结果应用回本地
        applySyncData(merged);
    }

    // GitHub Gist 同步：PATCH/GET gist，使用 token 鉴权
    // GitHub API 原生支持 CORS，不需要代理
    async function syncGist(cfg) {
        const gistId = cfg.url;
        const token = cfg.user;
        const filename = cfg.pass || 'msb-backup.json';
        const headers = {
            'Authorization': `token ${token}`,
            'Content-Type': 'application/json'
        };
        const apiUrl = `https://api.github.com/gists/${gistId}`;

        // 1. 下载远程数据（首次同步 gist 文件不存在时忽略错误）
        let remoteData = null;
        try {
            const getRes = await fetch(apiUrl, { method: 'GET', headers });
            if (getRes.ok) {
                const gist = await getRes.json();
                const file = gist.files && gist.files[filename];
                if (file && file.content) {
                    try { remoteData = JSON.parse(file.content); } catch {}
                }
            }
        } catch {}

        // 2. 合并本地与远程
        const localData = exportDataRaw();
        const merged = mergeSyncData(localData, remoteData);

        // 3. 上传合并结果
        const patchRes = await fetch(apiUrl, {
            method: 'PATCH',
            headers,
            body: JSON.stringify({
                files: { [filename]: { content: JSON.stringify(merged, null, 2) } }
            })
        });
        if (!patchRes.ok) {
            throw new Error(`上传失败 HTTP ${patchRes.status}`);
        }

        // 4. 将合并结果应用回本地
        applySyncData(merged);
    }

    // 同步合并策略：
    //  - 列表字段（bookmarks/history/notes 等）按 id 去重，取 timestamp/addedAt 较新者
    //  - 字符串/标量数组（bookmarkFolders/searchHistory/rssReadSet）直接并集去重
    //  - 标量/对象字段：比较双方 lastSync，远程较新则取远程，否则保留本地
    //  - 加密密码：取较新且非空的一方，避免空值覆盖已有密码
    //  - 不合并 syncConfig，避免各端本机同步凭据互相覆盖
    function mergeSyncData(local, remote) {
        if (!remote || typeof remote !== 'object') {
            // 远程为空，直接使用本地并打上时间戳
            return { ...local, lastSync: Date.now() };
        }

        // 按 id 去重合并的列表字段
        const idListFields = [
            'windows', 'bookmarks', 'history', 'notes', 'laterList',
            'tabs', 'customEngines', 'rssFeeds', 'userScripts'
        ];
        // 字符串/标量数组字段：直接并集去重
        const unionFields = ['bookmarkFolders', 'searchHistory', 'rssReadSet'];
        // 标量/对象字段：取较新版本
        const scalarFields = [
            'engine', 'theme', 'themeColor', 'parallelEngines', 'darkSchedule',
            'avatar', 'activeTabId', 'scrollProgress', 'llmConfig',
            'aiSummaryMode', 'translateMode', 'readingSettings', 'pwdAutoLock',
            'adBlockEnabled', 'translateCache', 'rssCache'
        ];

        const merged = { ...local };

        // 列表按 id 去重，取 timestamp/addedAt 较新者
        idListFields.forEach(key => {
            const localList = Array.isArray(local[key]) ? local[key] : [];
            const remoteList = Array.isArray(remote[key]) ? remote[key] : [];
            const map = new Map();
            const pushItem = (item) => {
                if (!item || item.id == null) return;
                const t = item.timestamp || item.addedAt || 0;
                const exist = map.get(item.id);
                if (!exist || t >= (exist.timestamp || exist.addedAt || 0)) {
                    map.set(item.id, item);
                }
            };
            localList.forEach(pushItem);
            remoteList.forEach(pushItem);
            merged[key] = Array.from(map.values());
        });

        // 字符串数组并集去重
        unionFields.forEach(key => {
            const localList = Array.isArray(local[key]) ? local[key] : [];
            const remoteList = Array.isArray(remote[key]) ? remote[key] : [];
            merged[key] = Array.from(new Set([...localList, ...remoteList]));
        });

        // 比较 lastSync 决定标量字段方向（远程较新则覆盖本地）
        const localTs = Number(local.lastSync) || 0;
        const remoteTs = Number(remote.lastSync) || 0;
        const remoteNewer = remoteTs > localTs;
        scalarFields.forEach(key => {
            if (remoteNewer && remote[key] !== undefined) {
                merged[key] = remote[key];
            } else if (local[key] !== undefined) {
                merged[key] = local[key];
            }
        });

        // 加密密码：取较新且非空的一方，避免空远程覆盖本地已有密码
        const localHasPwd = !!local.encryptedPasswords;
        const remoteHasPwd = !!remote.encryptedPasswords;
        if (remoteNewer && remoteHasPwd) {
            merged.encryptedPasswords = remote.encryptedPasswords;
            merged.masterPwdHash = remote.masterPwdHash;
            merged.masterPwdSalt = remote.masterPwdSalt;
        } else if (localHasPwd) {
            merged.encryptedPasswords = local.encryptedPasswords;
            merged.masterPwdHash = local.masterPwdHash;
            merged.masterPwdSalt = local.masterPwdSalt;
        }

        merged.exportTime = new Date().toISOString();
        merged.lastSync = Date.now();
        return merged;
    }

    // 将同步合并后的数据写回本地 store 并刷新界面
    function applySyncData(data) {
        if (!data) return;
        // 运行时从 app.js 获取 UI/渲染依赖（app.js 在本文件之后加载，调用时已就绪）
        const { applyTheme, applyThemeColor, applyDarkSchedule, renderHome, renderProfile, renderTabs } = window.MSBApp;
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
        if (data.rssFeeds) save(STORAGE_KEYS.rssFeeds, data.rssFeeds);
        if (data.rssCache) save(STORAGE_KEYS.rssCache, data.rssCache);
        if (data.rssReadSet) save(STORAGE_KEYS.rssReadSet, data.rssReadSet);
        if (data.userScripts) store.setUserScripts(data.userScripts);
        if (data.adBlockEnabled !== undefined) store.setAdBlockEnabled(data.adBlockEnabled);
        if (data.readingSettings) store.setReadingSettings(data.readingSettings);
        if (data.pwdAutoLock !== undefined) store.setPwdAutoLock(data.pwdAutoLock);
        if (data.translateCache) store.setTranslateCache(data.translateCache);
        if (data.llmConfig) store.setLlmConfig(data.llmConfig);
        if (data.aiSummaryMode) store.setAiSummaryMode(data.aiSummaryMode);
        if (data.translateMode) store.setTranslateMode(data.translateMode);
        // 加密密码相关（已加密，可安全同步）
        if (data.encryptedPasswords !== undefined) save(STORAGE_KEYS.passwords, data.encryptedPasswords);
        if (data.masterPwdHash) save(STORAGE_KEYS.masterPwdHash, data.masterPwdHash);
        if (data.masterPwdSalt) save(STORAGE_KEYS.masterPwdSalt, data.masterPwdSalt);
        // 注意：不覆盖 syncConfig，避免各端同步凭据互相覆盖
        renderHome();
        renderProfile();
        renderTabs();
    }

    // 导出全部 v2.0.0 数据（同步用）：书签/历史/笔记/标签/RSS/密码/设置等
    function exportDataRaw() {
        const syncCfg = store.getSyncConfig() || {};
        return {
            version: '2.0.0',
            exportTime: new Date().toISOString(),
            lastSync: Number(syncCfg.lastSync) || 0,
            // 基础设置
            engine: store.getSelectedEngine(),
            theme: store.getTheme(),
            themeColor: store.getThemeColor(),
            parallelEngines: store.getParallelEngines(),
            darkSchedule: store.getDarkSchedule(),
            avatar: store.getAvatar(),
            // 多窗口 / 标签页
            windows: store.getWindows(),
            tabs: store.getTabs(),
            activeTabId: store.getActiveTabId(),
            scrollProgress: load(STORAGE_KEYS.scrollProgress, {}),
            // 书签
            bookmarks: store.getBookmarks(),
            bookmarkFolders: store.getBookmarkFolders(),
            // 历史
            history: store.getHistory(),
            searchHistory: store.getSearchHistory(),
            // 稍后阅读
            laterList: store.getLaterList(),
            // 划线笔记
            notes: store.getNotes(),
            // 自定义引擎
            customEngines: store.getCustomEngines(),
            // RSS
            rssFeeds: store.getRssFeeds(),
            rssCache: store.getRssCache(),
            rssReadSet: load(STORAGE_KEYS.rssReadSet, []),
            // 密码管理（AES 加密后的密文 + 主密码哈希/盐，可安全同步）
            encryptedPasswords: store.getEncryptedPasswords(),
            masterPwdHash: store.getMasterPwdHash(),
            masterPwdSalt: store.getMasterPwdSalt(),
            // LLM / AI / 翻译
            llmConfig: store.getLlmConfig(),
            aiSummaryMode: store.getAiSummaryMode(),
            translateMode: store.getTranslateMode(),
            translateCache: store.getTranslateCache(),
            // 阅读设置 / 密码自动锁
            readingSettings: store.getReadingSettings(),
            pwdAutoLock: store.getPwdAutoLock(),
            // 用户脚本 / 广告拦截
            userScripts: store.getUserScripts(),
            adBlockEnabled: store.getAdBlockEnabled(),
        };
    }

    // ============ 导出同步模块到全局 ============
    window.MSBSync = {
        syncNow,
        syncWebDAV,
        syncGist,
        mergeSyncData,
        applySyncData,
        exportDataRaw,
    };
})();
