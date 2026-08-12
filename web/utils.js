/* ============ MultiSearch Browser · 通用工具库 v1.5.0 ============
 * 此文件为独立工具库，提供纯函数工具，无副作用，可被任意模块复用。
 * app.js 中的 IIFE 工具函数优先委派到 window.MSBUtils。
 * 未来若改造为 ES Module，本文件可作为首个抽离的模块。
 */
(function (global) {
    'use strict';

    const MSBUtils = {};

    // ============ DOM 选择器 ============
    MSBUtils.$ = (sel, root) => (root || document).querySelector(sel);
    MSBUtils.$$ = (sel, root) => Array.from((root || document).querySelectorAll(sel));

    // ============ HTML 转义 ============
    MSBUtils.escapeHtml = (str) => {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    };

    // 属性专用（同 escapeHtml，但语义分离便于未来扩展）
    MSBUtils.escapeAttr = MSBUtils.escapeHtml;

    // ============ 时间格式化 ============
    MSBUtils.formatTime = (ts) => {
        if (!ts) return '';
        const d = new Date(ts);
        const now = new Date();
        const diff = (now - d) / 1000;
        if (diff < 60) return '刚刚';
        if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前';
        if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前';
        if (diff < 86400 * 7) return Math.floor(diff / 86400) + ' 天前';
        const y = d.getFullYear(), m = d.getMonth() + 1, day = d.getDate();
        if (y === now.getFullYear()) return `${m}月${day}日`;
        return `${y}/${m}/${day}`;
    };

    // ============ URL 处理 ============
    MSBUtils.getDomain = (url) => {
        try {
            return new URL(url).hostname.replace(/^www\./, '');
        } catch {
            return url || '';
        }
    };

    MSBUtils.getFavicon = (url) => {
        try {
            const u = new URL(url);
            return `https://www.google.com/s2/favicons?domain=${u.hostname}&sz=64`;
        } catch {
            return '';
        }
    };

    MSBUtils.normalizeUrl = (input) => {
        if (!input) return '';
        const trimmed = input.trim();
        if (/^https?:\/\//i.test(trimmed)) return trimmed;
        // 看起来像域名（含 . 且无空格）
        if (/^[\w-]+(\.[\w-]+)+/.test(trimmed)) return 'https://' + trimmed;
        return ''; // 视为搜索词
    };

    MSBUtils.isUrl = (input) => {
        if (!input) return false;
        return /^https?:\/\/[^\s]+$/i.test(input.trim()) || /^[\w-]+(\.[\w-]+)+[^\s]*$/.test(input.trim());
    };

    // ============ 字符串工具 ============
    MSBUtils.truncate = (str, maxLen) => {
        if (!str) return '';
        return str.length > maxLen ? str.slice(0, maxLen) + '…' : str;
    };

    MSBUtils.debounce = (fn, wait = 200) => {
        let timer = null;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), wait);
        };
    };

    MSBUtils.throttle = (fn, wait = 200) => {
        let last = 0;
        return function (...args) {
            const now = Date.now();
            if (now - last >= wait) {
                last = now;
                fn.apply(this, args);
            }
        };
    };

    // ============ UUID / ID 生成 ============
    MSBUtils.uid = () => {
        return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
    };

    // ============ 复制到剪贴板 ============
    MSBUtils.copyToClipboard = async (text) => {
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch {
            // 回退方案
            const ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.opacity = '0';
            document.body.appendChild(ta);
            ta.select();
            try {
                document.execCommand('copy');
                document.body.removeChild(ta);
                return true;
            } catch {
                document.body.removeChild(ta);
                return false;
            }
        }
    };

    // ============ localStorage 安全包装 ============
    MSBUtils.safeStorage = {
        get: (key, defaultValue) => {
            try {
                const raw = localStorage.getItem(key);
                return raw ? JSON.parse(raw) : defaultValue;
            } catch {
                return defaultValue;
            }
        },
        set: (key, value) => {
            try {
                localStorage.setItem(key, JSON.stringify(value));
                return true;
            } catch (e) {
                if (e.name === 'QuotaExceededError') {
                    console.warn('[MSB] localStorage 配额已满');
                }
                return false;
            }
        },
        remove: (key) => {
            try { localStorage.removeItem(key); } catch {}
        }
    };

    // ============ 密码强度评估（与 app.js 保持一致） ============
    MSBUtils.evaluatePasswordStrength = (pwd) => {
        if (!pwd) return { score: 0, label: '空', color: '#9E9E9E' };
        let score = 0;
        if (pwd.length >= 8) score++;
        if (pwd.length >= 12) score++;
        if (pwd.length >= 16) score++;
        const has = {
            lower: /[a-z]/.test(pwd),
            upper: /[A-Z]/.test(pwd),
            digit: /\d/.test(pwd),
            symbol: /[^a-zA-Z0-9]/.test(pwd)
        };
        const variety = Object.values(has).filter(Boolean).length;
        if (variety >= 2) score++;
        if (variety >= 3) score++;
        const weak = ['123456', 'password', 'qwerty', '111111', '000000', 'abc123'];
        if (weak.includes(pwd.toLowerCase())) score = 0;
        score = Math.min(4, score);
        const labels = ['弱', '一般', '中等', '强', '非常强'];
        const colors = ['#F44336', '#FF9800', '#FFC107', '#8BC34A', '#4CAF50'];
        return { score, label: labels[score], color: colors[score] };
    };

    // ============ Toast（简化版，供非 app.js 上下文使用） ============
    let toastTimer = null;
    MSBUtils.toast = (msg, duration = 2000) => {
        let el = document.getElementById('toast');
        if (!el) {
            el = document.createElement('div');
            el.id = 'toast';
            el.style.cssText = 'position:fixed;left:50%;bottom:80px;transform:translateX(-50%);background:rgba(0,0,0,0.8);color:#fff;padding:8px 16px;border-radius:4px;font-size:13px;z-index:9999;pointer-events:none;';
            document.body.appendChild(el);
        }
        el.textContent = msg;
        el.style.display = 'block';
        el.style.opacity = '1';
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => {
            el.style.opacity = '0';
            setTimeout(() => { el.style.display = 'none'; }, 300);
        }, duration);
    };

    // ============ 导出 ============
    global.MSBUtils = MSBUtils;
})(window);
