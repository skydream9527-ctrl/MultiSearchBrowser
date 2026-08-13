/* v2.0.0: 从内联 script 抽离，配合 CSP 策略（禁止内联脚本） */
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('sw.js').catch(() => {});
    });
}
