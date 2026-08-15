/**
 * MultiSearch Browser · 密码管理模块
 * v2.1.0: 从 app.js 抽离，封装密码 CRUD / 主密码 / 自动锁屏 / 强度评估。
 *         含 v1.5.0: 安全加固（touchPwdActivity / restartAutoLockTimer / setupActivityMonitor /
 *         evaluatePasswordStrength），因与密码管理共享 masterPwdSession 等状态，一并抽离。
 * 依赖：MSBUtils / MSBConstants / MSBStore / MSBApp（运行时桥接）
 */
(function() {
    'use strict';

    const U = window.MSBUtils || {};
    const K = window.MSBConstants.STORAGE_KEYS;
    const store = window.MSBStore;

    // ============ 本地依赖委派（app.js 内部函数的等价引用） ============
    const $ = (sel) => document.querySelector(sel);
    // showToast 委派到 MSBUtils.toast
    const showToast = (msg) => U.toast(msg);
    // escapeHtml 委派到 MSBUtils.escapeHtml
    const escapeHtml = (str) => U.escapeHtml(str);
    // save 复用 MSBUtils.safeStorage
    const save = (key, val) => U.safeStorage.set(key, val);
    // credApi 为 app.js 内部对象，运行时通过 window.MSBApp 调用
    const credApi = {
        store: (site, username, password) => (window.MSBApp && window.MSBApp.credApi && window.MSBApp.credApi.store(site, username, password)),
        get: () => (window.MSBApp && window.MSBApp.credApi ? window.MSBApp.credApi.get() : Promise.resolve(null)),
    };

    // confirmDialog 本地实现（复用 #modal DOM）
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
        save(K.passwords, null);
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

    // ============ 导出密码管理模块到全局 ============
    window.MSBPasswords = {
        renderPasswords,
        setupPasswordsPage,
        editPassword,
        generatePassword,
        showPwdUnlock,
        setupMasterPwd,
        setupActivityMonitor,
        evaluatePasswordStrength,
        touchPwdActivity,
    };
})();
