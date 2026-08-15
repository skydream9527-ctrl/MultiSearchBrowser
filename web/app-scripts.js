/**
 * MultiSearch Browser · 用户脚本（油猴）模块
 * v2.1.0: 从 app.js 抽离，封装用户脚本管理 / 注入。
 * 依赖：MSBUtils / MSBConstants / MSBStore / MSBApp（运行时桥接）
 */
(function() {
    'use strict';

    const U = window.MSBUtils || {};
    const store = window.MSBStore;
    const BUILTIN_SCRIPTS = window.MSBConstants.BUILTIN_SCRIPTS;

    // ============ 本地依赖委派（app.js 内部函数的等价引用） ============
    const $ = (sel) => document.querySelector(sel);
    // showToast 委派到 MSBUtils.toast
    const showToast = (msg) => U.toast(msg);
    // escapeHtml 委派到 MSBUtils.escapeHtml
    const escapeHtml = (str) => U.escapeHtml(str);

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

    // ============ 导出用户脚本模块到全局 ============
    window.MSBScripts = {
        injectUserScripts,
        showUserScriptsManager,
        editUserScript,
    };
})();
