/**
 * MultiSearch Browser · Service Worker
 * v1.3.0：缓存静态资源 + 离线回退页
 */
const CACHE_VERSION = 'msb-v1.3.0';
const STATIC_ASSETS = [
    './',
    './index.html',
    './styles.css',
    './app.js',
    './manifest.json',
];

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

// 拦截请求：静态资源 cache-first，跨域动态请求 network-first
self.addEventListener('fetch', (event) => {
    const req = event.request;
    if (req.method !== 'GET') return;

    const url = new URL(req.url);
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
