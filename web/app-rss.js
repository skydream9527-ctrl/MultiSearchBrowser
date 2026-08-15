/**
 * MultiSearch Browser · RSS 订阅模块
 * v2.1.0: 从 app.js 抽离，封装 RSS 订阅源管理 / 抓取 / 渲染。
 * 依赖：MSBUtils / MSBConstants / MSBStore / MSBApp（运行时桥接）
 */
(function() {
    'use strict';

    const U = window.MSBUtils || {};
    const store = window.MSBStore;

    // ============ 本地依赖委派（app.js 内部函数的等价引用） ============
    const $ = (sel) => document.querySelector(sel);
    // showToast 委派到 MSBUtils.toast
    const showToast = (msg) => U.toast(msg);
    // escapeHtml 委派到 MSBUtils.escapeHtml
    const escapeHtml = (str) => U.escapeHtml(str);
    // fetchWithCorsFallback / openWebview 为 app.js 内部函数，运行时通过 window.MSBApp 调用
    const fetchWithCorsFallback = (url, options) => (window.MSBApp && window.MSBApp.fetchWithCorsFallback(url, options));
    const openWebview = (url, windowId) => (window.MSBApp && window.MSBApp.openWebview(url, windowId));

    // 时间格式化（与 app.js 实现保持一致：绝对时间 YYYY-MM-DD HH:MM）
    const formatTime = (ts) => {
        const d = new Date(ts);
        const pad = (n) => String(n).padStart(2, '0');
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    };

    // inputDialog 本地实现（复用 #input-modal DOM）
    const inputDialog = (title, defaultValue = '', autocomplete = 'off') => new Promise((resolve) => {
        const modal = $('#input-modal');
        $('#input-modal-title').textContent = title;
        const field = $('#input-modal-field');
        field.value = defaultValue;
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

    // ============ 导出 RSS 模块到全局 ============
    window.MSBRss = {
        renderRss,
        setupRssPage,
        refreshRss,
        fetchRssFeed,
    };
})();
