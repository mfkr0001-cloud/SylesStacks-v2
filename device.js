// Веб-диагностика ограничена данными, которые предоставляет браузер.
export function getBrowserDeviceInfo() {
  return { userAgent: navigator.userAgent, language: navigator.language, screen: `${screen.width}x${screen.height}`, online: navigator.onLine };
}
