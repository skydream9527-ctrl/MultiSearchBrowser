// v2.0.0: Web 端单元测试（Node.js 内置 test runner）
// 运行方式: node --test test/utils.test.js
// 需要在 web/ 目录下运行

const { test, describe } = require('node:test');
const assert = require('node:assert');

// ============ 模拟浏览器环境 ============
global.window = {};
global.document = {
    createElement: () => ({ style: {}, setAttribute: () => {} }),
    querySelector: () => null,
    querySelectorAll: () => [],
};
global.localStorage = {
    _data: {},
    getItem(k) { return this._data[k] || null; },
    setItem(k, v) { this._data[k] = String(v); },
    removeItem(k) { delete this._data[k]; },
    clear() { this._data = {}; },
};
global.navigator = { serviceWorker: undefined };

// 加载 utils.js（IIFE 会将 MSBUtils 挂到 global）
require('../utils.js');
const U = global.MSBUtils;

// ============ 测试用例 ============

describe('MSBUtils.escapeHtml', () => {
    test('转义 HTML 特殊字符', () => {
        assert.strictEqual(U.escapeHtml('<script>alert("xss")</script>'), '&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;');
    });
    test('空值返回空字符串', () => {
        assert.strictEqual(U.escapeHtml(null), '');
        assert.strictEqual(U.escapeHtml(undefined), '');
    });
    test('普通文本不变', () => {
        assert.strictEqual(U.escapeHtml('hello world'), 'hello world');
    });
});

describe('MSBUtils.getDomain', () => {
    test('提取域名', () => {
        assert.strictEqual(U.getDomain('https://www.baidu.com/s?q=test'), 'baidu.com');
    });
    test('无协议 URL', () => {
        assert.strictEqual(U.getDomain('example.com/path'), 'example.com');
    });
    test('空值返回空', () => {
        assert.strictEqual(U.getDomain(''), '');
    });
});

describe('MSBUtils.normalizeUrl', () => {
    test('自动补 https://', () => {
        assert.strictEqual(U.normalizeUrl('baidu.com'), 'https://baidu.com');
    });
    test('已有协议保持不变', () => {
        assert.strictEqual(U.normalizeUrl('https://baidu.com'), 'https://baidu.com');
    });
    test('空值返回空', () => {
        assert.strictEqual(U.normalizeUrl(''), '');
    });
});

describe('MSBUtils.isUrl', () => {
    test('合法 URL 返回 true', () => {
        assert.strictEqual(U.isUrl('https://www.baidu.com'), true);
        assert.strictEqual(U.isUrl('http://localhost:8000'), true);
    });
    test('非 URL 返回 false', () => {
        assert.strictEqual(U.isUrl('hello world'), false);
        assert.strictEqual(U.isUrl(''), false);
    });
});

describe('MSBUtils.truncate', () => {
    test('截断超长文本', () => {
        assert.strictEqual(U.truncate('abcdefghij', 5), 'ab...');
    });
    test('短文本不截断', () => {
        assert.strictEqual(U.truncate('abc', 10), 'abc');
    });
});

describe('MSBUtils.formatTime', () => {
    test('格式化时间戳', () => {
        const ts = new Date('2024-01-15T10:30:00').getTime();
        const result = U.formatTime(ts);
        assert.ok(result.includes('2024'));
        assert.ok(result.includes('10:30'));
    });
});

describe('MSBUtils.evaluatePasswordStrength', () => {
    test('空密码 score=0', () => {
        const r = U.evaluatePasswordStrength('');
        assert.strictEqual(r.score, 0);
    });
    test('强密码 score>=4', () => {
        const r = U.evaluatePasswordStrength('Str0ngP@ssw0rd2024!');
        assert.ok(r.score >= 4, `expected score>=4, got ${r.score}`);
    });
    test('弱密码 score<=2', () => {
        const r = U.evaluatePasswordStrength('abc');
        assert.ok(r.score <= 2, `expected score<=2, got ${r.score}`);
    });
});

describe('MSBUtils.safeStorage', () => {
    test('set/get 往返一致', () => {
        global.localStorage.clear();
        U.safeStorage.set('test_key', { a: 1, b: 'hello' });
        const result = U.safeStorage.get('test_key', null);
        assert.deepStrictEqual(result, { a: 1, b: 'hello' });
    });
    test('key 不存在返回 defaultValue', () => {
        global.localStorage.clear();
        assert.strictEqual(U.safeStorage.get('nonexistent', 'default'), 'default');
    });
    test('remove 删除后返回 defaultValue', () => {
        global.localStorage.clear();
        U.safeStorage.set('temp', 42);
        U.safeStorage.remove('temp');
        assert.strictEqual(U.safeStorage.get('temp', null), null);
    });
});

describe('MSBUtils.uid', () => {
    test('生成唯一 ID', () => {
        const id1 = U.uid();
        const id2 = U.uid();
        assert.ok(typeof id1 === 'string');
        assert.notStrictEqual(id1, id2);
    });
});

describe('MSBUtils.debounce', () => {
    test('防抖只执行最后一次', (t) => {
        return new Promise((resolve) => {
            let count = 0;
            const fn = U.debounce(() => { count++; }, 50);
            fn();
            fn();
            fn();
            setTimeout(() => {
                assert.strictEqual(count, 1);
                resolve();
            }, 100);
        });
    });
});
