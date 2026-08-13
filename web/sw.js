/**
 * MultiSearch Browser · Service Worker
 * v2.0.0：缓存静态资源 + 离线回退页 + 广告拦截 + PWA + i18n + CSP 配套
 */
const CACHE_VERSION = 'msb-v2.0.0';
const STATIC_ASSETS = [
    './',
    './index.html',
    './styles.css',
    './app.js',
    './utils.js',
    './i18n.js',
    './sw-register.js',
    './manifest.json',
];

// v2.0.0: 广告/追踪域名黑名单（扩充至 40+）
const AD_BLACKLIST = [
    'doubleclick.net',
    'googlesyndication.com',
    'googletagmanager.com',
    'googletagservices.com',
    'google-analytics.com',
    'adservice.google.com',
    'facebook.net',
    'amazon-adsystem.com',
    'criteo.com',
    'criteo.net',
    'taboola.com',
    'outbrain.com',
    'adnxs.com',
    'pubmatic.com',
    'rubiconproject.com',
    'openx.net',
    'quantserve.com',
    'scorecardresearch.com',
    'hotjar.com',
    'mixpanel.com',
    'segment.io',
    'adroll.com',
    'cnzz.com',
    'umeng.com',
    'talkingdata.com',
    // v2.0.0 新增
    'appsflyer.com',
    'branch.io',
    'smartadserver.com',
    'admob.com',
    'adsystem.com',
    'mgid.com',
    'propellerads.com',
    'adsterra.com',
    'popads.net',
    'popcash.net',
    'mediavine.com',
    'mediavine.net',
    'monetag.com',
    'adskeeper.com',
    'mgid.com',
    'yieldmo.com',
    'infolinks.com',
    'chitika.com',
    'buysellads.com',
    'carbonads.com',
];

// 检查 URL 是否命中黑名单
function isAdRequest(url) {
    try {
        const hostname = new URL(url).hostname.toLowerCase();
        const pathname = new URL(url).pathname.toLowerCase();
        return AD_BLACKLIST.some(item => {
            if (item.includes('/')) {
                // 含路径的黑名单项
                return (hostname + pathname).includes(item);
            }
            return hostname.includes(item);
        });
    } catch {
        return false;
    }
}

// 安装：预缓存静态资源
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_VERSION).then((cache) => cache.addAll(STATIC_ASSETS)).then(() => self.skipWaiting())
    );
});

// 激活：清理旧缓存
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(keys.filter(k => k !== CACHE_VERSION).map(k => caches.delete(k)));
        }).then(() => self.clients.claim())
    );
});

// 拦截请求：广告拦截 + 静态资源 cache-first + 跨域 network-first
self.addEventListener('fetch', (event) => {
    const req = event.request;
    if (req.method !== 'GET') return;

    const url = new URL(req.url);

    // v1.6.0: 广告拦截 - 命中黑名单直接返回空响应
    if (isAdRequest(req.url)) {
        // 根据请求类型返回合适的空响应
        const dest = req.destination;
        let emptyBody = '';
        let contentType = 'text/plain';
        if (dest === 'script') { emptyBody = '/* blocked by MSB */'; contentType = 'application/javascript'; }
        else if (dest === 'image') { emptyBody = ''; contentType = 'image/png'; }
        else if (dest === 'style') { emptyBody = '/* blocked by MSB */'; contentType = 'text/css'; }
        event.respondWith(new Response(emptyBody, {
            status: 200,
            headers: { 'Content-Type': contentType, 'Cache-Control': 'no-store' }
        }));
        return;
    }

    const sameOrigin = url.origin === self.location.origin;

    if (sameOrigin) {
        // 同源静态资源：cache-first
        event.respondWith(
            caches.match(req).then((cached) => {
                if (cached) return cached;
                return fetch(req).then((res) => {
                    if (res.ok && (req.url.includes('.html') || req.url.includes('.css') || req.url.includes('.js') || req.url.includes('.json'))) {
                        const clone = res.clone();
                        caches.open(CACHE_VERSION).then(c => c.put(req, clone));
                    }
                    return res;
                }).catch(() => caches.match('./index.html'));
            })
        );
    } else {
        // 跨域请求：network-first，失败回退 cache
        event.respondWith(
            fetch(req).then((res) => {
                if (res.ok) {
                    const clone = res.clone();
                    caches.open(CACHE_VERSION + '-runtime').then(c => c.put(req, clone));
                }
                return res;
            }).catch(() => caches.match(req))
        );
    }
});
