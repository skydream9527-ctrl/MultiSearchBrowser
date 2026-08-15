/**
 * MultiSearch Browser · AI/LLM 模块
 * v2.1.0: 从 app.js 抽离，封装 TextRank 摘要 / LLM API / 划词翻译。
 * 依赖：MSBUtils / MSBConstants / MSBStore / CryptoJS（可选）
 */
(function() {
    'use strict';

    const U = window.MSBUtils || {};
    const store = window.MSBStore;

    // ============ 本地依赖委派（app.js 内部函数的等价引用） ============
    // $ 直接复用 DOM 选择器实现
    const $ = (sel) => document.querySelector(sel);
    // showToast 委派到 MSBUtils.toast
    const showToast = (msg) => U.toast(msg);
    // escapeHtml 委派到 MSBUtils.escapeHtml
    const escapeHtml = (str) => U.escapeHtml(str);
    // navigate / handleSearch 为 app.js 路由与搜索入口，运行时通过 window.MSBApp 调用
    const navigate = (route, push) => (window.MSBApp && window.MSBApp.navigate(route, push));
    const handleSearch = () => (window.MSBApp && window.MSBApp.handleSearch());

    // ============ v1.4.0: AI 摘要（TextRank + LLM 可选） ============
    let translateActive = false;
    // v1.7.0: AI 对话式摘要上下文
    let summaryChatMessages = []; // [{role, content}]
    let summarySourceText = '';   // 原始文本，供追问引用

    function generateSummary(content) {
        const text = (content.innerText || '').trim();
        if (!text) {
            showToast('没有可摘要的内容');
            return;
        }
        // v1.7.0: 重置对话上下文
        summarySourceText = text;
        summaryChatMessages = [];
        const mode = store.getAiSummaryMode();
        const panel = $('#summary-panel');
        const spContent = $('#sp-content');
        panel.hidden = false;
        spContent.innerHTML = '<div style="text-align:center;padding:24px;color:var(--text-secondary)">⏳ 生成中...</div>';

        if (mode === 'llm') {
            // v1.7.0: 首次生成时建立 system + user 上下文
            const truncated = text.length > 4000 ? text.slice(0, 4000) + '...' : text;
            summaryChatMessages.push({
                role: 'system',
                content: '你是一个文档摘要助手。用户会提供一段内容，请生成中文摘要，并在用户追问时基于原文继续回答。'
            });
            summaryChatMessages.push({
                role: 'user',
                content: `请对以下内容生成中文摘要，包含：1) 3 句话核心摘要 2) 5-8 个关键词 3) 主要观点列表。\n\n内容：${truncated}`
            });
            generateLlmSummary(text).then(result => {
                summaryChatMessages.push({ role: 'assistant', content: result.replace(/<[^>]+>/g, '') });
                spContent.innerHTML = result;
                appendFollowUpUI(spContent);
            }).catch(err => {
                spContent.innerHTML = `<div style="color:var(--accent);padding:16px">❌ ${err}</div><div style="margin-top:8px;color:var(--text-secondary);font-size:13px">已切换到本地摘要</div>`;
                setTimeout(() => {
                    const localResult = generateLocalSummary(text);
                    spContent.innerHTML = localResult;
                    appendFollowUpUI(spContent);
                }, 1500);
            });
        } else {
            setTimeout(() => {
                const localResult = generateLocalSummary(text);
                spContent.innerHTML = localResult;
                appendFollowUpUI(spContent);
            }, 300);
        }

        $('#sp-close').onclick = () => { panel.hidden = true; };
    }

    // v1.7.0: 追问 UI（输入框 + 快捷问题）
    function appendFollowUpUI(container) {
        const followUp = document.createElement('div');
        followUp.className = 'sp-followup';
        followUp.innerHTML = `
            <div class="sp-section">
                <div class="sp-section-title">💬 追问</div>
                <div class="sp-quick-questions">
                    <button class="sp-qq-btn" data-q="请用一句话总结">一句话总结</button>
                    <button class="sp-qq-btn" data-q="列出核心观点">核心观点</button>
                    <button class="sp-qq-btn" data-q="有哪些值得注意的细节？">注意细节</button>
                    <button class="sp-qq-btn" data-q="适合什么读者？">适合读者</button>
                </div>
                <div class="sp-input-row">
                    <input type="text" id="sp-followup-input" placeholder="输入追问..." autocomplete="off">
                    <button id="sp-followup-send">发送</button>
                </div>
                <div id="sp-followup-history"></div>
            </div>
        `;
        container.appendChild(followUp);
        // 绑定事件
        followUp.querySelectorAll('.sp-qq-btn').forEach(btn => {
            btn.onclick = () => askFollowUp(btn.dataset.q);
        });
        $('#sp-followup-send').onclick = () => {
            const input = $('#sp-followup-input');
            const q = input.value.trim();
            if (q) {
                askFollowUp(q);
                input.value = '';
            }
        };
        $('#sp-followup-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const q = e.target.value.trim();
                if (q) {
                    askFollowUp(q);
                    e.target.value = '';
                }
            }
        });
    }

    async function askFollowUp(question) {
        const historyEl = $('#sp-followup-history');
        if (!historyEl) return;
        // 渲染用户问题
        const qDiv = document.createElement('div');
        qDiv.className = 'sp-chat sp-chat-user';
        qDiv.innerHTML = `<div class="sp-chat-bubble">${escapeHtml(question)}</div>`;
        historyEl.appendChild(qDiv);
        historyEl.scrollTop = historyEl.scrollHeight;

        // 渲染"思考中"
        const aDiv = document.createElement('div');
        aDiv.className = 'sp-chat sp-chat-ai';
        aDiv.innerHTML = '<div class="sp-chat-bubble">⏳ 思考中...</div>';
        historyEl.appendChild(aDiv);
        historyEl.scrollTop = historyEl.scrollHeight;

        const mode = store.getAiSummaryMode();
        if (mode === 'llm') {
            try {
                summaryChatMessages.push({ role: 'user', content: question });
                const answer = await callLlmChat(summaryChatMessages);
                summaryChatMessages.push({ role: 'assistant', content: answer });
                aDiv.querySelector('.sp-chat-bubble').innerHTML = escapeHtml(answer).replace(/\n/g, '<br>');
            } catch (e) {
                aDiv.querySelector('.sp-chat-bubble').innerHTML = `❌ ${e.message}<br><span style="font-size:11px;color:var(--gray)">使用本地回答</span>`;
                const localAnswer = localFollowUp(question);
                aDiv.querySelector('.sp-chat-bubble').innerHTML = escapeHtml(localAnswer).replace(/\n/g, '<br>');
            }
        } else {
            const localAnswer = localFollowUp(question);
            setTimeout(() => {
                aDiv.querySelector('.sp-chat-bubble').innerHTML = escapeHtml(localAnswer).replace(/\n/g, '<br>');
            }, 300);
        }
        historyEl.scrollTop = historyEl.scrollHeight;
    }

    async function callLlmChat(messages, opts = {}) {
        const { withSourceContext = true } = opts;
        const cfg = store.getLlmConfig();
        // 错误处理：API key 未配置时提示用户去设置页配置
        if (!cfg || !cfg.apiKey) {
            throw new Error('未配置 LLM API Key，请到设置页 → LLM 配置中填写');
        }
        const provider = cfg.provider || 'qianwen';

        // 统一以 OpenAI 兼容的 messages 作为输入；追问场景下附带截断原文作为 system 上下文
        const fullMessages = [...messages];
        if (withSourceContext && summarySourceText) {
            const ctx = summarySourceText.length > 3000 ? summarySourceText.slice(0, 3000) + '...' : summarySourceText;
            fullMessages.push({ role: 'system', content: `参考原文（仅用于回答追问）：\n${ctx}` });
        }

        const headers = {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${cfg.apiKey}`
        };

        // 根据不同 provider 构建 endpoint 与请求体
        let endpoint, body, parseResponse;
        if (provider === 'qianwen') {
            // 通义千问：自有 generation 接口，需将 messages 包装到 input 字段（与 OpenAI 格式不同）
            endpoint = 'https://dashscope.aliyuncs.com/api/v1/services/aio/generation/generation';
            body = {
                model: cfg.model || 'qwen-turbo',
                input: { messages: fullMessages }
            };
            parseResponse = (data) => data.output?.choices?.[0]?.message?.content || data.output?.text || '无返回';
        } else if (provider === 'deepseek') {
            // DeepSeek：OpenAI 兼容格式
            endpoint = 'https://api.deepseek.com/v1/chat/completions';
            body = {
                model: cfg.model || 'deepseek-chat',
                messages: fullMessages,
                max_tokens: 800
            };
            parseResponse = (data) => data.choices?.[0]?.message?.content || '无返回';
        } else if (provider === 'doubao') {
            // 豆包：OpenAI 兼容格式
            endpoint = 'https://ark.cn-beijing.volces.com/api/v3/chat/completions';
            body = {
                model: cfg.model || 'doubao-pro-4k',
                messages: fullMessages,
                max_tokens: 800
            };
            parseResponse = (data) => data.choices?.[0]?.message?.content || '无返回';
        } else {
            // custom：使用用户配置的 apiUrl，按 OpenAI 兼容格式请求
            if (!cfg.apiUrl) throw new Error('未配置自定义 API URL，请到设置页填写');
            endpoint = cfg.apiUrl;
            body = {
                model: cfg.model || 'gpt-3.5-turbo',
                messages: fullMessages,
                max_tokens: 800
            };
            parseResponse = (data) => data.choices?.[0]?.message?.content || data.output?.text || '无返回';
        }

        const res = await fetch(endpoint, {
            method: 'POST',
            headers,
            body: JSON.stringify(body)
        });
        if (!res.ok) throw new Error(`LLM API 返回 ${res.status}`);
        const data = await res.json();
        return parseResponse(data);
    }

    // 本地追问回答（基于 TextRank 数据的简单启发式）
    function localFollowUp(question) {
        if (!summarySourceText) return '请先生成摘要';
        const q = question.toLowerCase();
        if (q.includes('一句话') || q.includes('简短')) {
            const sentences = splitSentences(summarySourceText);
            if (sentences.length > 0) return sentences[0];
        }
        if (q.includes('关键词') || q.includes('核心')) {
            const words = tokenize(summarySourceText);
            const freq = {};
            words.forEach(w => freq[w] = (freq[w] || 0) + 1);
            const top = Object.entries(freq).sort((a, b) => b[1] - a[1]).slice(0, 8).map(([w]) => w);
            return '关键词：' + top.join('、');
        }
        if (q.includes('观点') || q.includes('细节')) {
            const sentences = splitSentences(summarySourceText);
            return sentences.slice(0, 3).join('\n');
        }
        // 默认：返回前 3 句
        const sentences = splitSentences(summarySourceText);
        return sentences.slice(0, 3).join('\n');
    }

    function generateLocalSummary(text) {
        // TextRank 算法
        const sentences = splitSentences(text);
        if (sentences.length < 3) {
            return `<div class="sp-section"><div class="sp-section-title">原文（太短未摘要）</div><div>${escapeHtml(text.slice(0, 500))}</div></div>`;
        }
        // 词频统计（简易中文分词：按字符+英文按空格）
        const words = tokenize(text);
        const wordFreq = {};
        words.forEach(w => { wordFreq[w] = (wordFreq[w] || 0) + 1; });
        // 句子评分：词频累加
        const sentScores = sentences.map((s, i) => {
            const sw = tokenize(s);
            let score = 0;
            sw.forEach(w => score += wordFreq[w] || 0);
            return { idx: i, text: s, score: score / (sw.length || 1) };
        });
        // 取 Top 3
        const top = sentScores.sort((a, b) => b.score - a.score).slice(0, 3).sort((a, b) => a.idx - b.idx);
        // 关键词 Top 8
        const topWords = Object.entries(wordFreq).sort((a, b) => b[1] - a[1]).slice(0, 8);

        let html = '<div class="sp-section"><div class="sp-section-title">📋 摘要</div>';
        top.forEach(s => html += `<div style="margin-bottom:8px">${escapeHtml(s.text)}</div>`);
        html += '</div>';
        html += '<div class="sp-section"><div class="sp-section-title">🔑 关键词</div><div class="sp-keywords">';
        topWords.forEach(([w, c]) => html += `<span class="sp-keyword">${escapeHtml(w)} (${c})</span>`);
        html += '</div></div>';
        html += `<div class="sp-section"><div class="sp-section-title">📊 统计</div>
            <div style="font-size:13px;color:var(--text-secondary)">
                句子数：${sentences.length} · 词数：${words.length} · 字符：${text.length}
            </div></div>`;
        // v1.7.0: 关键词云可视化按钮（携带 Top 30 词频数据）
        const cloudWords = Object.entries(wordFreq).sort((a, b) => b[1] - a[1]).slice(0, 30);
        const cloudData = cloudWords.map(([w, c]) => ({ word: w, count: c }));
        html += `<div class="sp-section">
            <div class="sp-section-title">☁ 词云</div>
            <button class="sp-qq-btn" id="sp-show-cloud">查看词云可视化</button>
            <div id="sp-cloud-container"></div>
        </div>`;
        html += `<div style="font-size:11px;color:var(--gray);margin-top:8px">由本地 TextRank 算法生成</div>`;
        // 异步绑定按钮事件（innerHTML 设置后才能找到元素）
        setTimeout(() => {
            const btn = document.getElementById('sp-show-cloud');
            if (btn) {
                btn.onclick = () => {
                    const container = document.getElementById('sp-cloud-container');
                    if (container) {
                        container.innerHTML = renderKeywordCloud(cloudData);
                    }
                };
            }
        }, 0);
        return html;
    }

    // v1.7.0: SVG 词云渲染
    function renderKeywordCloud(words) {
        if (!words || words.length === 0) return '<div style="color:var(--text-secondary);padding:8px">无关键词</div>';
        const maxCount = words[0].count;
        const minCount = words[words.length - 1].count;
        const range = maxCount - minCount || 1;
        // 螺旋布局：从中心向外旋转放置
        const W = 320, H = 200;
        const placed = [];
        const colors = ['#2196F3', '#FF5722', '#4CAF50', '#FF9800', '#9C27B0', '#00BCD4', '#795548', '#607D8B'];
        let angle = 0, radius = 0;
        const cx = W / 2, cy = H / 2;
        const items = words.slice(0, 24).map((w, i) => {
            // 字号 12-28，按频率映射
            const ratio = (w.count - minCount) / range;
            const fontSize = 12 + Math.round(ratio * 16);
            const color = colors[i % colors.length];
            // 螺旋放置算法
            let x = cx, y = cy;
            let attempts = 0;
            while (attempts < 80) {
                x = cx + Math.cos(angle) * radius;
                y = cy + Math.sin(angle) * radius;
                // 检查碰撞（简化：估算文本宽 = 字号 * 字数 / 1.6）
                const tw = fontSize * w.word.length * 0.6;
                const th = fontSize;
                const overlap = placed.some(p => {
                    return Math.abs(x - p.x) < (tw + p.tw) / 2 + 4 && Math.abs(y - p.y) < (th + p.th) / 2 + 2;
                });
                if (!overlap && x - tw / 2 > 2 && x + tw / 2 < W - 2 && y - th / 2 > 2 && y + th / 2 < H - 2) {
                    placed.push({ x, y, tw, th });
                    break;
                }
                angle += 0.3;
                radius += 0.8;
                attempts++;
            }
            return { word: w.word, count: w.count, x, y, fontSize, color };
        });
        let svg = `<svg viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" style="width:100%;max-width:${W}px">`;
        svg += `<rect width="${W}" height="${H}" fill="var(--gray-light)" rx="8"/>`;
        items.forEach(item => {
            svg += `<text x="${item.x}" y="${item.y}" font-size="${item.fontSize}" fill="${item.color}"
                text-anchor="middle" dominant-baseline="middle"
                class="kw-cloud-item"
                data-word="${MSBUtils.escapeAttr(item.word)}"
                onclick="window.__msbCloudClick && window.__msbCloudClick('${MSBUtils.escapeAttr(item.word)}')">${escapeHtml(item.word)}</text>`;
        });
        svg += '</svg>';
        // 暴露点击回调
        window.__msbCloudClick = (word) => {
            // 点击关键词：填入搜索框并搜索
            const input = $('#search-input');
            if (input) {
                input.value = word;
                navigate('home');
                handleSearch();
            }
        };
        return svg;
    }

    function splitSentences(text) {
        return text.split(/[。！？!?\n]+/).map(s => s.trim()).filter(s => s.length > 5);
    }

    function tokenize(text) {
        const words = [];
        // 英文单词
        const enMatches = text.match(/[a-zA-Z]{2,}/g) || [];
        words.push(...enMatches.map(w => w.toLowerCase()));
        // 中文：按字符二元组（简易分词）
        const zhMatches = text.match(/[\u4e00-\u9fa5]{2,}/g) || [];
        zhMatches.forEach(seg => {
            for (let i = 0; i < seg.length - 1; i++) {
                words.push(seg.substr(i, 2));
            }
        });
        return words;
    }

    async function generateLlmSummary(text) {
        const cfg = store.getLlmConfig();
        if (!cfg || !cfg.apiKey) {
            throw new Error('未配置 LLM API Key，请到设置页 → LLM 配置中填写');
        }
        const truncated = text.length > 4000 ? text.slice(0, 4000) + '...' : text;
        const prompt = `请对以下内容生成中文摘要，包含：1) 3 句话核心摘要 2) 5-8 个关键词 3) 主要观点列表。\n\n内容：${truncated}`;
        // 复用 callLlmChat 统一对接各 provider；摘要生成不需要再附带原文上下文（原文已在 prompt 中）
        const content = await callLlmChat(
            [{ role: 'user', content: prompt }],
            { withSourceContext: false }
        );
        return `<div class="sp-section"><div class="sp-section-title">🤖 LLM 摘要</div><div style="white-space:pre-wrap">${escapeHtml(content)}</div></div>`;
    }

    // ============ v1.4.0: 划词翻译 ============
    function toggleTranslateMode(content) {
        translateActive = !translateActive;
        const btn = $('#reading-translate');
        if (translateActive) {
            btn.classList.add('reading-tts-active');
            showToast('🌐 翻译模式已开启，选词即翻译');
        } else {
            btn.classList.remove('reading-tts-active');
            $('#translate-popover').hidden = true;
            showToast('翻译模式已关闭');
        }
    }

    async function performTranslate(text, event) {
        const pop = $('#translate-popover');
        const result = $('#tp-result');
        const lang = $('#tp-lang');
        const isZh = /[\u4e00-\u9fa5]/.test(text);
        const targetLang = isZh ? 'en' : 'zh';
        lang.textContent = `${isZh ? '中' : '英'} → ${targetLang === 'en' ? '英' : '中'}`;
        result.textContent = '翻译中...';
        pop.hidden = false;
        // 定位
        const x = event.clientX || (window.innerWidth / 2);
        const y = event.clientY || 100;
        pop.style.left = Math.max(8, Math.min(window.innerWidth - 260, x - 120)) + 'px';
        pop.style.top = Math.max(8, y + 16) + 'px';

        const mode = store.getTranslateMode();
        let html;
        if (mode === 'offline') {
            html = await offlineTranslate(text, isZh);
        } else {
            // v1.5.0: 先查缓存
            const cacheKey = `${text}|${isZh ? 'zh' : 'en'}|${targetLang}`;
            const cache = store.getTranslateCache();
            if (cache[cacheKey]) {
                html = cache[cacheKey].value + '<div style="font-size:11px;color:var(--gray)">via 缓存</div>';
            } else {
                html = await onlineTranslate(text, isZh, targetLang);
                // 入库（成功翻译才缓存）
                if (!html.includes('❌') && !html.includes('翻译失败')) {
                    const cacheInner = html.replace(/<div style="font-size:11px;color:var\(--gray\)">via MyMemory API<\/div>/, '').trim();
                    store.addTranslateCache(cacheKey, cacheInner);
                }
            }
        }
        result.innerHTML = html;

        $('#tp-close').onclick = () => { pop.hidden = true; };
    }

    async function onlineTranslate(text, isZh, targetLang) {
        try {
            const src = isZh ? 'zh' : 'en';
            const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${src}|${targetLang}`;
            const res = await fetch(url);
            const data = await res.json();
            const translated = data.responseData?.translatedText || '翻译失败';
            return `<div style="font-weight:500;margin-bottom:4px">${escapeHtml(translated)}</div>
                <div style="font-size:11px;color:var(--gray)">via MyMemory API</div>`;
        } catch (e) {
            return `❌ 在线翻译失败：${e.message}<br><span style="font-size:11px;color:var(--gray)">可切换到离线模式</span>`;
        }
    }

    async function offlineTranslate(text, isZh) {
        // 简易离线：英文单词直接给词义提示
        if (isZh) {
            return `<div style="color:var(--text-secondary)">离线模式暂不支持中→英</div>`;
        }
        const lower = text.toLowerCase().trim();
        if (lower.length > 30) {
            return `<div style="color:var(--text-secondary)">离线模式仅支持单词/短语</div>`;
        }
        return `<div style="font-weight:500">${escapeHtml(text)}</div>
            <div style="font-size:12px;color:var(--text-secondary);margin-top:4px">离线模式：建议切换到 MyMemory 在线</div>`;
    }

    // ============ v1.4.0: LLM API 配置 ============
    function showLlmConfig() {
        const modal = $('#llm-config-modal');
        const cfg = store.getLlmConfig() || {};
        $('#llm-provider').value = cfg.provider || 'qianwen';
        $('#llm-api-key').value = cfg.apiKey || '';
        $('#llm-api-url').value = cfg.apiUrl || '';
        $('#llm-model').value = cfg.model || '';
        modal.hidden = false;
        // 自动填充默认 URL/model
        $('#llm-provider').onchange = (e) => {
            const presets = {
                qianwen: { url: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', model: 'qwen-turbo' },
                doubao: { url: 'https://ark.cn-beijing.volces.com/api/v3/chat/completions', model: 'doubao-pro-4k' },
                deepseek: { url: 'https://api.deepseek.com/v1/chat/completions', model: 'deepseek-chat' },
                custom: { url: '', model: '' }
            };
            const p = presets[e.target.value];
            if (p) {
                $('#llm-api-url').value = p.url;
                $('#llm-model').value = p.model;
            }
        };
        $('#llm-save').onclick = () => {
            const newCfg = {
                provider: $('#llm-provider').value,
                apiKey: $('#llm-api-key').value.trim(),
                apiUrl: $('#llm-api-url').value.trim(),
                model: $('#llm-model').value.trim()
            };
            store.setLlmConfig(newCfg);
            modal.hidden = true;
            showToast('✅ LLM 配置已保存');
        };
        $('#llm-cancel').onclick = () => { modal.hidden = true; };
    }

    // ============ 导出 AI 模块到全局 ============
    window.MSBAi = {
        generateSummary,
        generateLlmSummary,
        callLlmChat,
        toggleTranslateMode,
        performTranslate,
        showLlmConfig,
        isTranslateActive: () => translateActive,
    };
})();
